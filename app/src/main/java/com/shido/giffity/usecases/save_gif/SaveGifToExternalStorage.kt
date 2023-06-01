package com.shido.giffity.usecases.save_gif

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.shido.giffity.domain.DataState
import kotlinx.coroutines.flow.Flow

interface SaveGifToExternalStorage {

    fun execute(
        contentResolver: ContentResolver,
        context: Context,
        cachedUri: Uri,
        checkFilePermissions: () -> Boolean
    ): Flow<DataState<Unit>>

}