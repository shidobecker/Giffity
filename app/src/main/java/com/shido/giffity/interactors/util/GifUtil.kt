package com.shido.giffity.interactors.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.shido.giffity.domain.CacheProvider
import com.shido.giffity.domain.VersionProvider
import com.shido.giffity.domain.util.AnimatedGIFWriter
import com.shido.giffity.domain.util.FileNameBuilder
import com.shido.giffity.interactors.BuildGifInteractor
import com.shido.giffity.interactors.BuildGifResult
import java.io.ByteArrayOutputStream
import java.io.File

object GifUtil {

    fun buildGifAndSaveToInternalStorage(
        contentResolver: ContentResolver,
        versionProvider: VersionProvider,
        cacheProvider: CacheProvider,
        bitmaps: List<Bitmap>
    ): BuildGifResult {
        check(bitmaps.isNotEmpty()) { BuildGifInteractor.NO_BITMAPS_ERROR }

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
        } ?: throw Exception(BuildGifInteractor.SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)

    }
}