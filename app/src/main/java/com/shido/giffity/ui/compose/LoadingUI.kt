package com.shido.giffity.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shido.giffity.domain.DataState
import com.shido.giffity.ui.compose.theme.GiffityTheme

@Composable
fun StandardLoadingUI(
    loadingState: DataState.Loading.LoadingState
) {
    if (loadingState is DataState.Loading.LoadingState.Active) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(3f)
        ) {
            LoadingCircleAnimation(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun ResizingGifLoadingUI(
    gifResizingLoadingState: DataState.Loading.LoadingState
) {
    if (gifResizingLoadingState is DataState.Loading.LoadingState.Active && gifResizingLoadingState.progress != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Resizing Gif ${gifResizingLoadingState.progressInPercent}%",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LoadingCircleAnimation(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}


@Composable
fun LoadingCircleAnimation(
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 100.dp,
    circleColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.primary,
    ),
    animationDuration: Int = 1000
) {

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val rotateAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            )
        ), label = ""
    )

    CircularProgressIndicator(
        modifier = modifier
            .size(size = indicatorSize)
            .rotate(degrees = rotateAnimation)
            .border(
                width = 10.dp,
                brush = Brush.sweepGradient(circleColors),
                shape = CircleShape
            ),
        progress = 1f,
        strokeWidth = 1.dp,
        color = MaterialTheme.colorScheme.background // Set background color
    )
}

@Composable
@Preview
private fun ResizingGifLoadingUI_Preview() {
    GiffityTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            ResizingGifLoadingUI(gifResizingLoadingState = DataState.Loading.LoadingState.Active(50f))
        }
    }
}