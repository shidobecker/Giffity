package com.shido.giffity.interactors

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

interface PixelCopyJob {

    suspend fun execute(capturingViewBounds: Rect?, view: View, window: Window): PixelCopyJobState


    sealed class PixelCopyJobState {
        data class Done(val bitmap: Bitmap) : PixelCopyJobState()

        data class Error(val message: String) : PixelCopyJobState()
    }

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class PixelCopyJobModule {
    @Binds
    abstract fun providePixelCopyJobUseCase(pixelCopyJobInteractor: PixelCopyJobInteractor): PixelCopyJob
}


class PixelCopyJobInteractor @Inject constructor() : PixelCopyJob {

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun execute(
        capturingViewBounds: Rect?, view: View, window: Window
    ): PixelCopyJob.PixelCopyJobState = suspendCancellableCoroutine { cont ->
        try {
            check(capturingViewBounds != null) { "Invalid capture area." }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)

            val xCoordinate = locationOfViewInWindow[0]
            val yCoordinate = locationOfViewInWindow[1]

            val scope = android.graphics.Rect(
                xCoordinate, yCoordinate, xCoordinate + view.width, yCoordinate + view.height
            )

            PixelCopy.request(window, scope, bitmap, { p0 ->
                if (p0 == PixelCopy.SUCCESS) {

                    val bmp = Bitmap.createBitmap(
                        bitmap, capturingViewBounds.left.toInt(),
                        capturingViewBounds.top.toInt(),
                        capturingViewBounds.width.toInt(),
                        capturingViewBounds.height.toInt(),
                    )

                    cont.resume(PixelCopyJob.PixelCopyJobState.Done(bmp), onCancellation = null)

                } else {
                    cont.resume(
                        PixelCopyJob.PixelCopyJobState.Error(PIXEL_COPY_ERROR),
                        onCancellation = null
                    )
                }
            }, Handler(Looper.getMainLooper()))

        } catch (e: Exception) {
            cont.resume(
                PixelCopyJob.PixelCopyJobState.Error(e.message ?: PIXEL_COPY_ERROR),
                onCancellation = null
            )

        }

    }

    companion object {
        const val PIXEL_COPY_ERROR = "Something went wrong capturing a screenshot"
    }
}