package com.shido.giffity.domain

sealed class DataState<T> {

    data class Data<T>(val data: T? = null) : DataState<T>()

    data class Error<T>(val message: String) : DataState<T>()

    data class Loading<T>(val loading: LoadingState) : DataState<T>() {

        sealed class LoadingState{

            //Active loading state with optional progress
            data class Active(
                val progress: Float? = 0f
            ): LoadingState()

            object Idle: LoadingState()

        }

    }

}
