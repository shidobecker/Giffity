package com.shido.giffity.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shido.giffity.domain.DataState

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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center),
                color = Color.Blue,
                strokeWidth = 4.dp
            )
        }
    }
}