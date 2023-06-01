package com.shido.giffity.usecases.save_gif

import android.content.ContentResolver
import android.net.Uri

interface SaveGifToInternalStorage {
    fun execute(contentResolver: ContentResolver, bytes: ByteArray): Uri
}