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


//region Initial State
fun State<MainState>.asInitialState() = this.value as MainState.Initial

//endregion Initial State


//region DisplayBackgroundAssetState
fun State<MainState>.isDisplayBackgroundAssetState() =
    this.value is MainState.DisplayBackgroundAsset

fun State<MainState>.asDisplayBackgroundAssetState() =
    this.value as MainState.DisplayBackgroundAsset

fun State<MainState>.executeWhenMainStateIsDisplayBackgroundAssetState(action: (MainState.DisplayBackgroundAsset) -> Unit) {
    if (this.isDisplayBackgroundAssetState()) {
        action(this.asDisplayBackgroundAssetState())
    }
}

//endregion DisplayBackgroundAssetState


//region DisplaySelectBackgroundAssetState
fun State<MainState>.isDisplaySelectBackgroundAssetState() =
    this.value is MainState.DisplaySelectBackgroundAsset

fun State<MainState>.asDisplaySelectBackgroundAssetState() =
    this.value as MainState.DisplaySelectBackgroundAsset

fun State<MainState>.executeWhenMainStateIsDisplaySelectBackgroundAssetState(action: (MainState.DisplaySelectBackgroundAsset) -> Unit) {
    if (this.isDisplaySelectBackgroundAssetState()) {
        action(this.asDisplaySelectBackgroundAssetState())
    }
}

//endregion DisplaySelectBackgroundAssetState


//region DisplayBackgroundAssetImageState
fun State<MainState>.asDisplaySelectBackgroundAssetImageState() =
    this.value as MainState.DisplayBackgroundAssetImage

fun State<MainState>.isDisplaySelectBackgroundAssetImageState() =
    this.value is MainState.DisplayBackgroundAssetImage

fun State<MainState>.executeWhenMainStateIsDisplaySelectBackgroundImageState(action: (MainState.DisplayBackgroundAssetImage) -> Unit) {
    if (this.isDisplaySelectBackgroundAssetImageState()) {
        action(this.asDisplaySelectBackgroundAssetImageState())
    }
}

//endregion DisplayBackgroundAssetImageState


//region DisplayGifState
fun State<MainState>.asDisplayGifState() = this.value as MainState.DisplayGif

fun State<MainState>.isDisplayGifState() = this.value is MainState.DisplayGif

fun State<MainState>.executeWhenMainStateIsDisplaySGifState(action: (MainState.DisplayGif) -> Unit) {
    if (this.isDisplayGifState()) {
        action(this.asDisplayGifState())
    }
}
//endregion DisplayGifState




