package com.shido.giffity.usecases.save_gif

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.shido.giffity.domain.providers.CacheProvider
import com.shido.giffity.domain.providers.VersionProvider
import com.shido.giffity.domain.util.FileNameBuilder
import com.shido.giffity.usecases.build_gif.BuildGifUseCase
import java.io.File
import javax.inject.Inject

class SaveGifToInternalStorageUseCase @Inject constructor(
    private val versionProvider: VersionProvider,
    private val cacheProvider: CacheProvider
) : SaveGifToInternalStorage {

    @SuppressLint("NewApi")
    override fun execute(contentResolver: ContentResolver, bytes: ByteArray): Uri {
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
        } ?: throw Exception(BuildGifUseCase.SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)

    }


}