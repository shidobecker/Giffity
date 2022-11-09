package com.shido.giffity.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundAssetFooter(
    modifier: Modifier,
    isRecording: Boolean,
    launchImagePicker: () -> Unit
) {
//if bitmap capture job is not running, show footer
    if (!isRecording) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = launchImagePicker, modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 16.dp)
            ) {
                Text(text = "Change background image")
            }
        }
    }
}