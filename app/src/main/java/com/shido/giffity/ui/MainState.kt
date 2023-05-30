package com.shido.giffity.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.State
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
        val backgroundAssetUri: Uri,

        //Displayed as a CircularIndeterminateProgressBar overlayed in the center of the screen
        val saveGifLoadingState: DataState.Loading.LoadingState = DataState.Loading.LoadingState.Idle,

        val resizedGifUri: Uri?,
        val adjustedBytes: Int,
        val sizePercentage: Int,
        val capturedBitmaps: List<Bitmap> = listOf(), //Take the list of bitmaps loop them and resize each one

        //Displayed as a LinearProgressIndicator on middle of screen
        val resizeGifLoadingState: DataState.Loading.LoadingState = DataState.Loading.LoadingState.Idle,
    ) : MainState()

    data class DisplayBackgroundAssetImage(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmap: Bitmap? = null
    ) : MainState()


}

fun State<MainState>.asInitialState() = this.value as MainState.Initial

fun MainState.asInitialState() = this as MainState.Initial

fun MainState.asDisplayBackgroundAssetState() = this as MainState.DisplayBackgroundAsset

fun State<MainState>.asDisplayBackgroundAssetState() = this.value as MainState.DisplayBackgroundAsset

fun MainState.asDisplaySelectBackgroundAssetState() = this as MainState.DisplaySelectBackgroundAsset

fun State<MainState>.asDisplaySelectBackgroundAssetState() = this.value as MainState.DisplaySelectBackgroundAsset

fun MainState.asDisplaySelectBackgroundAssetImageState() = this as MainState.DisplayBackgroundAssetImage

fun State<MainState>.asDisplaySelectBackgroundAssetImageState() = this.value as MainState.DisplayBackgroundAssetImage

fun MainState.asDisplayGifState() = this as MainState.DisplayGif

fun State<MainState>.asDisplayGifState() = this.value as MainState.DisplayGif


