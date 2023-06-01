package com.shido.giffity.ui.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import com.shido.giffity.R
import com.shido.giffity.domain.DataState
import kotlinx.coroutines.NonDisposableHandle.parent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BackgroundAsset(
    backgroundAssetUri: Uri,
    updateCapturingViewBounds: (Rect) -> Unit,
    bitmapCaptureLoadingState: DataState.Loading.LoadingState,
    startBitmapCaptureJob: () -> Unit,
    stopBitmapCaptureJob: () -> Unit,
    launchImagePicker: () -> Unit,
    loadingState: DataState.Loading.LoadingState
) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (topBar, assetContainer, bottomContainer) = createRefs()

        //Top bar
        //topBarHeight = (default app bar height + button padding)
        val topBarHeight = remember { 56 + 16 }

        RecordActionBar(modifier = Modifier
            .fillMaxWidth()
            .height(topBarHeight.dp)
            .constrainAs(topBar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
            .zIndex(2f)
            .background(color = Color.White),
            bitmapCaptureLoadingState = bitmapCaptureLoadingState,
            startBitmapCaptureJob = startBitmapCaptureJob,
            stopBitmapCaptureJob = stopBitmapCaptureJob)

        //Gif capture area
        val configuration = LocalConfiguration.current
        val assetContainerHeight = remember { (configuration.screenHeightDp * 0.6).toInt() }

        RenderBackground(modifier = Modifier
            .constrainAs(assetContainer) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(topBar.bottom)
            }
            .zIndex(1f),
            updateCapturingViewBounds = updateCapturingViewBounds,
            backgroundAssetUri = backgroundAssetUri,
            assetContainerHeightDp = assetContainerHeight)

        StandardLoadingUI(loadingState = loadingState)


        //Bottom Container
        val bottomContainerHeight =
            remember { configuration.screenHeightDp - assetContainerHeight - topBarHeight }

        BackgroundAssetFooter(modifier = Modifier
            .fillMaxWidth()
            .height(bottomContainerHeight.dp)
            .constrainAs(bottomContainer) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(assetContainer.bottom)
                bottom.linkTo(parent.bottom)
            }
            .zIndex(2f)
            .background(color = Color.White),
            isRecording = bitmapCaptureLoadingState is DataState.Loading.LoadingState.Active,
            launchImagePicker = launchImagePicker)
    }
}

@Composable
fun RenderBackground(
    modifier: Modifier, backgroundAssetUri: Uri,
    updateCapturingViewBounds: (Rect) -> Unit,
    assetContainerHeightDp: Int
) {
    Box(modifier = modifier.wrapContentSize()) {
        val backgroundAsset = rememberAsyncImagePainter(model = backgroundAssetUri)

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(assetContainerHeightDp.dp)
                .onGloballyPositioned { updateCapturingViewBounds(it.boundsInRoot()) }, //The area is where the screenshots will be taken
            painter = backgroundAsset,
            contentScale = ContentScale.Crop,
            contentDescription = ""
        )

        RenderAsset(assetContainerHeightDp = assetContainerHeightDp)
    }
}

@Composable
fun RenderAsset(assetContainerHeightDp: Int) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var inverted by remember { mutableStateOf(false) }

    val offsetXDp = with(LocalDensity.current) {
        offset.x.toDp()
    }
    val offsetYDp = with(LocalDensity.current) {
        offset.y.toDp()
    }

    val asset = painterResource(id = R.drawable.deal_with_it_sunglasses_default)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(assetContainerHeightDp.dp)
    ) {
        Image(painter = asset, modifier = Modifier
            .scale(scaleX = if (inverted) -1f else 1f, scaleY = 1f)
            .graphicsLayer {
                val rotatedOffset = offset.rotateBy(angle)
                translationX = -rotatedOffset.x
                translationY = -rotatedOffset.y
                scaleX = zoom
                scaleY = zoom
                rotationZ = angle
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    inverted = inverted.not()
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures(onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                    val oldScale = zoom
                    val newScale = zoom * gestureZoom

                    offset =
                        (offset - centroid * oldScale).rotateBy(-gestureRotate) + (centroid * newScale - pan * oldScale)
                    zoom = newScale
                    angle += gestureRotate
                })

            }
            .width(200.dp)
            .height(200.dp), contentDescription = "")
    }
}

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * Math.PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}
