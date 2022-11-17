package com.shido.giffity.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import com.shido.giffity.domain.DataState

sealed class MainState {

    object Initial : MainState()

    object DisplaySelectBackgroundAsset : MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = emptyList(),

        val bitmapCaptureLoadingState: DataState.Loading.LoadingState = DataState.Loading.LoadingState.Idle,

        val loadingState: DataState.Loading.LoadingState = DataState.Loading.LoadingState.Idle
    ) : MainState()


    data class DisplayGif(
        val gifUri: Uri?,
        val originalGifSize: Int,

        //Carry the original background asset Uri in case user resets the gif
        val backgroundAssetUri: Uri
    ) : MainState()

    data class DisplayBackgroundAssetImage(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmap: Bitmap? = null
    ) : MainState()

}


