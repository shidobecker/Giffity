package com.shido.giffity.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SelectBackgroundAsset(launchImagePicker: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        Button(
            onClick = launchImagePicker,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                style = MaterialTheme.typography.body2.copy( fontWeight = FontWeight.Bold
                ), text = "Select Background Image"
            )
        }
    }
}