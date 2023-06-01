package com.shido.giffity.domain

import kotlin.math.roundToInt

sealed class DataState<T> {

    data class Data<T>(val data: T? = null) : DataState<T>()

    data class Error<T>(val message: String) : DataState<T>()

    data class Loading<T>(val loading: LoadingState) : DataState<T>() {

        sealed class LoadingState {

            //Active loading state with optional progress
            data class Active(
                val progress: Float? = 0f
            ) : LoadingState() {
                val progressInPercent = progress?.let { it * 100 }?.roundToInt()
            }

            object Idle : LoadingState()

        }
    }
}

fun <T> DataState<T>.whenState(
    onLoading: (DataState.Loading<T>) -> Unit,
    onError: (String) -> Unit,
    onData: (T?) -> Unit
) {
    when (this) {
        is DataState.Loading -> onLoading(this)
        is DataState.Error -> onError(this.message)
        is DataState.Data -> onData(this.data)
    }
}

fun <T> DataState<T>.whenLoading(onLoading: () -> Unit) {
    if (this is DataState.Loading) onLoading()
}

fun <T> DataState<T>.whenError(onError: (String) -> Unit) {
    if (this is DataState.Error) onError(this.message)
}

fun <T> DataState<T>.whenData(onData: (T?) -> Unit) {
    if (this is DataState.Data) onData(this.data)
}

