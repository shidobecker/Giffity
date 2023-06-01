package com.shido.giffity.usecases.build_gif

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.shido.giffity.domain.providers.CacheProvider
import com.shido.giffity.domain.DataState
import com.shido.giffity.domain.providers.VersionProvider
import com.shido.giffity.interactors.util.GifUtil.buildGifAndSaveToInternalStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface BuildGif {
    fun execute(
        contentResolver: ContentResolver, bitmaps: List<Bitmap>
    ): Flow<DataState<BuildGifResult>>
}

data class BuildGifResult(
    val uri: Uri, val gifSize: Int
)


class BuildGifUseCase @Inject constructor(
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
        const val NO_BITMAPS_ERROR = "You can't build a gif without bitmaps"
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "Save gif error"
    }
}