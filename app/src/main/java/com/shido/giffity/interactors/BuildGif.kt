package com.shido.giffity.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.shido.giffity.domain.CacheProvider
import com.shido.giffity.domain.DataState
import com.shido.giffity.domain.VersionProvider
import com.shido.giffity.domain.util.AnimatedGIFWriter
import com.shido.giffity.domain.util.FileNameBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import java.io.File

interface BuildGif {
    fun execute(
        contentResolver: ContentResolver, bitmaps: List<Bitmap>
    ): Flow<DataState<BuildGifResult>>
}

data class BuildGifResult(
    val uri: Uri, val gifSize: Int
)

class BuildGifInteractor constructor(
    private val versionProvider: VersionProvider, private val cacheProvider: CacheProvider
) : BuildGif {
    override fun execute(
        contentResolver: ContentResolver, bitmaps: List<Bitmap>
    ): Flow<DataState<BuildGifResult>> = flow {
        emit(DataState.Loading(DataState.Loading.LoadingState.Active()))

        try {
            val result = buildGifAndSaveToInternalStorage(
                bitmaps = bitmaps,
                contentResolver = contentResolver,
                versionProvider = versionProvider,
                cacheProvider = cacheProvider
            )

            emit(DataState.Data(result))

        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: BUILD_GIF_ERROR))
        }

        emit(DataState.Loading(DataState.Loading.LoadingState.Idle))

    }

    companion object {
        const val BUILD_GIF_ERROR = "An error ocurred when gif"
        private const val NO_BITMAPS_ERROR = "You can't build a gif without bitmaps"
        private const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "Save gif error"

        private fun buildGifAndSaveToInternalStorage(
            contentResolver: ContentResolver,
            versionProvider: VersionProvider,
            cacheProvider: CacheProvider,
            bitmaps: List<Bitmap>
        ): BuildGifResult {
            check(bitmaps.isNotEmpty()) { NO_BITMAPS_ERROR }

            val writer = AnimatedGIFWriter(true)
            val bos = ByteArrayOutputStream()
            writer.prepareForWrite(bos, -1, -1)
            for (bitmap in bitmaps) {
                writer.writeFrame(bos, bitmap)
            }

            writer.finishWrite(bos)
            val byteArray = bos.toByteArray()
            val uri =
                saveGifToInternalStorage(contentResolver, versionProvider, cacheProvider, byteArray)
            return BuildGifResult(uri, byteArray.size)
        }

        @SuppressLint("NewApi")
        private fun saveGifToInternalStorage(
            contentResolver: ContentResolver,
            versionProvider: VersionProvider,
            cacheProvider: CacheProvider,
            bytes: ByteArray
        ): Uri {
            val fileName = if (versionProvider.provideVersion() >= Build.VERSION_CODES.O) {
                "${FileNameBuilder.buildFileNameAPI26()}.gif"
            } else {
                "${FileNameBuilder.buildFileName()}.gif"
            }

            val file = File.createTempFile(fileName, null, cacheProvider.gifCache())
            val uri = file.toUri()

            return contentResolver.openOutputStream(uri)?.use { os ->
                os.write(bytes)
                os.flush()
                os.close()
                uri
            } ?: throw Exception(SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)

        }


    }
}