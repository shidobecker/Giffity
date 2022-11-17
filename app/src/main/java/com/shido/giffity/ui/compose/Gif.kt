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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter

@Composable
fun Gif(imageLoader: ImageLoader, gifUri: Uri?, discardGif:() -> Unit) {
    if (gifUri != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            val configuration = LocalConfiguration.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(onClick =  discardGif, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Discard",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }

                Button(onClick = {// TODO
                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)) {
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

            //TODO: Add a footer for resizing the gif

        }
    }
}