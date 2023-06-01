package com.shido.giffity.usecases.resize_gif

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

interface ResizeGif {

    fun execute(
        contentResolver: ContentResolver, capturedBitmaps: List<Bitmap>,
        originalGifSize: Float, targetSize: Float, bilinearFiltering: Boolean = true,
        discardCachedGif: (Uri) -> Unit
    ): Flow<DataState<ResizeGifResult>>

    data class ResizeGifResult(val uri: Uri, val gifSize: Int)

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ResizeGifModule {
    @Binds
    abstract fun provideResizeGifUseCase(resizeGifInteractor: ResizeGifInteractor): ResizeGif
}



class ResizeGifInteractor @Inject constructor(
    private val versionProvider: VersionProvider,
    private val cacheProvider: CacheProvider
) : ResizeGif {
    override fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        bilinearFiltering: Boolean,
        discardCachedGif: (Uri) -> Unit
    ): Flow<DataState<ResizeGif.ResizeGifResult>> = flow {
        var previousUri: Uri? = null
        var progress: Float
        var percentageLoss = percentageLossIncrementSize

        emit(DataState.Loading(DataState.Loading.LoadingState.Active(percentageLoss)))

        try {
            var resizing = true

            //Decrease the size by 5% incremental on each iteration and check to see if it's below the [targetSize]
            while (resizing) {
                //Delete the previous resize gif since we re moving to the next iteration
                previousUri?.let {
                    discardCachedGif(it)
                }

                //Resize the bitmaps individually and build a new gif from them
                val resizedBitmaps: MutableList<Bitmap> = mutableListOf()

                for (bitmap in capturedBitmaps) {
                    val resizedBitmap = resizeBitmap(
                        bitmap = bitmap,
                        sizePercentage = 1 - percentageLoss,
                        bilinearFiltering = bilinearFiltering
                    )

                    resizedBitmaps.add(resizedBitmap)
                }

                val result = buildGifAndSaveToInternalStorage(
                    contentResolver = contentResolver,
                    versionProvider = versionProvider,
                    cacheProvider = cacheProvider,
                    bitmaps = resizedBitmaps
                )

                val uri = result.uri
                val newSize = result.gifSize

                //Progress indicator
                progress = (originalGifSize - newSize) / (originalGifSize - targetSize)
                emit(DataState.Loading(DataState.Loading.LoadingState.Active(progress)))

                if (newSize > targetSize) {
                    previousUri = uri
                    percentageLoss += percentageLossIncrementSize
                } else {
                    //Done
                    emit(DataState.Data(ResizeGif.ResizeGifResult(uri = uri, gifSize = newSize)))
                    resizing = false
                }

            }


            emit(DataState.Loading(DataState.Loading.LoadingState.Idle))
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: RESIZE_ERROR_MESSAGE))
        }

    }

    /**
     * @param bilinearFiltering : has better image quality at the cost of worse performance
     */
    private fun resizeBitmap(
        bitmap: Bitmap,
        sizePercentage: Float,
        bilinearFiltering: Boolean
    ): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, bilinearFiltering)
    }

    companion object {
        const val percentageLossIncrementSize = 0.05f //5%
        const val RESIZE_ERROR_MESSAGE = "An error occurred resizing the gif"
    }
}