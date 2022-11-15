package com.shido.giffity.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import android.net.Uri

sealed class MainState {

    object Initial : MainState()

    object DisplaySelectBackgroundAsset : MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = emptyList()
    ) : MainState()

    data class DisplayBackgroundAssetImage(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmap: Bitmap? = null
    ) : MainState()

}