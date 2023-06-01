package com.shido.giffity.usecases.save_gif

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.shido.giffity.domain.DataState
import com.shido.giffity.domain.providers.VersionProvider
import com.shido.giffity.domain.util.FileNameBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject


class SaveGifToExternalStorageUseCase @Inject constructor(private val versionProvider: VersionProvider) :
    SaveGifToExternalStorage {

    @SuppressLint("NewApi")
    override fun execute(
        contentResolver: ContentResolver,
        context: Context,
        cachedUri: Uri,
        checkFilePermissions: () -> Boolean
    ): Flow<DataState<Unit>> = flow {
        try {
            emit(DataState.Loading(DataState.Loading.LoadingState.Active()))

            when {
                //if API >= 29 we can use scoped storage and don't require a permission check
                versionProvider.provideVersion() >= Build.VERSION_CODES.Q -> {
                    saveGifToScopedStorage(contentResolver = contentResolver, cachedUri = cachedUri)
                    emit(DataState.Data(Unit))
                }

                //Scope storage doesn't exist before Android 29
                checkFilePermissions() -> {
                    saveGifToExternalStorage(
                        contentResolver = contentResolver,
                        context = context,
                        cachedUri = cachedUri
                    )

                    emit(DataState.Data(Unit))
                }

                else -> emit(DataState.Error(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))

            }


        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        }

        emit(DataState.Loading(DataState.Loading.LoadingState.Idle))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveGifToScopedStorage(contentResolver: ContentResolver, cachedUri: Uri) {
        //Get bytes from the cached uri
        val bytes = getBytesFromUri(contentResolver, cachedUri)

        val fileName = "${FileNameBuilder.buildFileNameAPI26()}.gif"
        val externalUri: Uri =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
        val uri = contentResolver.insert(externalUri, contentValues)
            ?: throw Exception("Error inserting the uri into storage")

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()
            outputStream.close()
        }
    }

    companion object {
        const val SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR = "Error occurred trying to save to storage"

        fun saveGifToExternalStorage(
            contentResolver: ContentResolver,
            context: Context,
            cachedUri: Uri
        ) {
            val bytes = getBytesFromUri(contentResolver = contentResolver, cachedUri = cachedUri)

            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val fileName = FileNameBuilder.buildFileName()
            val fileSavePath = File(picturesDir, "$fileName.gif")

            //Make sure pictures dir exists
            picturesDir.mkdirs()

            val fos = FileOutputStream(fileSavePath)
            fos.write(bytes)
            fos.close()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(fileSavePath.toString()),
                null
            ) { _, _ -> }
        }

        private fun getBytesFromUri(contentResolver: ContentResolver, cachedUri: Uri): ByteArray {
            val inputStream = contentResolver.openInputStream(cachedUri)
            val bytes = inputStream?.readBytes() ?: ByteArray(0)
            inputStream?.close()
            return bytes
        }

    }
}