package com.shido.giffity.interactors

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import com.shido.giffity.domain.DataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

interface CaptureBitmaps {

    fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window?
    ): Flow<DataState<List<Bitmap>>>

}


class CaptureBitmapsInteractor constructor(private val pixelCopyJob: PixelCopyJob) :
    CaptureBitmaps {

    override fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window?
    ): Flow<DataState<List<Bitmap>>> = flow {
        emit(DataState.Loading(DataState.Loading.LoadingState.Active()))

        try {
            check(capturingViewBounds != null) { "Invalid view bounds" }
            check(view != null) { "Invalid view" }

            var elapsedTime = 0f
            val bitmaps: MutableList<Bitmap> = mutableListOf()

            while (elapsedTime < TOTAL_CAPTURE_TIME) {
                delay(CAPTURE_INTERVAL_MS)

                elapsedTime += CAPTURE_INTERVAL_MS

                emit(DataState.Loading(DataState.Loading.LoadingState.Active(elapsedTime / TOTAL_CAPTURE_TIME))) //Progress

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    check(window != null) { "Window is required for pixelcopy" }

                    when (val pixelCopyJobState =
                        pixelCopyJob.execute(capturingViewBounds, view, window)) {
                        is PixelCopyJob.PixelCopyJobState.Done -> {
                            pixelCopyJobState.bitmap
                        }

                        is PixelCopyJob.PixelCopyJobState.Error -> {
                            throw Exception(pixelCopyJobState.message)
                        }
                    }


                } else {
                    captureBitmap(capturingViewBounds, view)
                }

                bitmaps.add(bitmap)
                emit(DataState.Data(bitmaps.toList())) //Emit list everytime since user can cancel the job
            }


        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }

    }

    //To capture screenshot on API 25 or below
    private fun captureBitmap(rect: Rect, view: View): Bitmap {
        return Bitmap.createBitmap(
            rect.width.roundToInt(),
            rect.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-rect.left, -rect.top)
            view.draw(this)
        }
    }

    companion object {
        const val CAPTURE_BITMAP_ERROR = "An error occurred capturing the bitmaps."

        const val TOTAL_CAPTURE_TIME = 4000F

        const val CAPTURE_INTERVAL_MS = 250L

        const val CAPTURE_BITMAP_SUCCESS = "Completed Successfully"
    }

}