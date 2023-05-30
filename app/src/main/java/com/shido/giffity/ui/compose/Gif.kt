package com.shido.giffity.ui.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.shido.giffity.domain.DataState

@Composable
fun Gif(
    imageLoader: ImageLoader,
    gifUri: Uri?,
    discardGif: () -> Unit,
    onSaveGif: () -> Unit,
    resetToOriginal: () -> Unit,
    isResizedGif: Boolean,
    currentGifSize: Int,
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    resizeGif: () -> Unit,
    gifResizingLoadingState: DataState.Loading.LoadingState,
    gifSaveLoadingState: DataState.Loading.LoadingState
) {

    StandardLoadingUI(loadingState = gifSaveLoadingState)

    //StandardLoadingUI(loadingState = gifResizingLoadingState)
    //TODO: Linear progress indicator
    if (gifResizingLoadingState is DataState.Loading.LoadingState.Active) {
        Text(modifier = Modifier.fillMaxSize(), text = "REsizing GIF!")
    }

    if (gifUri != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            val configuration = LocalConfiguration.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = discardGif,
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Discard",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }

                Button(
                    onClick = onSaveGif,
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Green)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }
            }

            val image = rememberAsyncImagePainter(model = gifUri, imageLoader)

            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((configuration.screenHeightDp * 0.6).dp),
                contentScale = ContentScale.Crop,
                painter = image,
                contentDescription = "gif"
            )

            GifFooter(
                adjustedBytes = adjustedBytes,
                updateAdjustedBytes = updateAdjustedBytes,
                sizePercentage = sizePercentage,
                updateSizePercentage = updateSizePercentage,
                gifSize = currentGifSize,
                isResizedGif = isResizedGif,
                resetResizing = resetToOriginal,
                resizeGif = resizeGif
            )

        }
    }
}

@Composable
fun GifFooter(
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    gifSize: Int,
    isResizedGif: Boolean,
    resetResizing: () -> Unit,
    resizeGif: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.bodySmall,
            text = "Approximate gif size"
        )
        Text(
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.bodyMedium,
            text = "${adjustedBytes / 1024} KB"
        )

        if (isResizedGif) {
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = resetResizing
            ) {
                Text(
                    text = "Reset resizing",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Text(
                style = MaterialTheme.typography.bodyMedium,
                text = "$sizePercentage %"
            )

            var sliderPosition by remember { mutableStateOf(100f) }

            Slider(
                value = sliderPosition,
                valueRange = 1f..100f,
                onValueChange = {
                    sliderPosition = it
                    updateSizePercentage(sliderPosition.toInt())
                    updateAdjustedBytes(gifSize * sliderPosition.toInt() / 100)
                }
            )

            Button(modifier = Modifier.align(Alignment.End), onClick = resizeGif) {
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    text = "Resize"
                )
            }
        }


    }

}
