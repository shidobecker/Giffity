package com.shido.giffity.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecordActionBar(
    modifier: Modifier, isRecording: Boolean, updateIsRecording: (Boolean) -> Unit
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(3f)
                .height(50.dp)
                .background(Color.Transparent)
        ) {
            //TODO
        }

        RecordButton(
            modifier = Modifier.weight(1f),
            isRecording = isRecording,
            updateIsRecording = updateIsRecording
        )
    }
}

@Composable
fun RecordButton(modifier: Modifier, isRecording: Boolean, updateIsRecording: (Boolean) -> Unit) {
    Button(
        modifier = modifier.wrapContentWidth(),
        colors = if (isRecording) ButtonDefaults.buttonColors(backgroundColor = Color.Red) else ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary
        ),
        onClick = { updateIsRecording(isRecording.not()) }
    ) {
        Text(
            style = MaterialTheme.typography.button.copy(
                fontWeight = FontWeight.Bold
            ), text = if (isRecording) "End" else "Record"
        )
    }
}
