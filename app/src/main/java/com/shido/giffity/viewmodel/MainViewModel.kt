package com.shido.giffity.viewmodel

import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shido.giffity.domain.DataState
import com.shido.giffity.domain.ErrorEvent
import com.shido.giffity.interactors.CaptureBitmaps
import com.shido.giffity.interactors.CaptureBitmapsInteractor
import com.shido.giffity.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.shido.giffity.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.shido.giffity.interactors.PixelCopyJobInteractor
import com.shido.giffity.ui.MainState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import java.util.UUID

class MainViewModel : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy = PixelCopyJobInteractor()
    private val captureBitmaps: CaptureBitmaps = CaptureBitmapsInteractor(
        pixelCopyJob = pixelCopy
    )

    private val _state: MutableState<MainState> = mutableStateOf(MainState.Initial)
    val state: State<MainState> get() = _state

    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay

    private val _errorEventRelay: MutableStateFlow<MutableSet<ErrorEvent>> = MutableStateFlow(
        mutableSetOf()
    )
    val errorEventRelay: StateFlow<Set<ErrorEvent>> get() = _errorEventRelay

    fun updateState(mainState: MainState) {
        _state.value = mainState
    }

    fun toastShow(id: String = UUID.randomUUID().toString(), message: String) {
        _toastEventRelay.tryEmit(ToastEvent(id = id, message = message))
    }

    fun publishErrorEvent(errorEvent: ErrorEvent) {
        _errorEventRelay.update {
            val current = it
            current.add(errorEvent)
            current
        }
    }

    //Old
    /* @RequiresApi(Build.VERSION_CODES.O)
     fun captureScreenshot(view: View, window: Window) {
         val state = state.value
         check(state is MainState.DisplayBackgroundAsset) { "Invalid state  $state" }
         CoroutineScope(dispatcher).launch {
             val result = pixelCopy.execute(
                 capturingViewBounds = state.capturingViewBounds, view = view, window = window
             )

             when (result) {
                 is PixelCopyJob.PixelCopyJobState.Done -> {
                     _state.value = state.copy(capturedBitmap = result.bitmap)
                 }

                 is PixelCopyJob.PixelCopyJobState.Error -> {
                     publishErrorEvent(
                         ErrorEvent(
                             id = UUID.randomUUID().toString(), message = result.message
                         )
                     )
                 }

             }
         }
     } */

    @RequiresApi(Build.VERSION_CODES.O)
    fun runBitmapCaptureJob(view: View, window: Window) {
        val state = state.value
        check(state is MainState.DisplayBackgroundAsset) { "Invalid state  $state" }

        //Stop the job if a user presses "STOP".

        val bitmapCaptureJob = Job()
        //Create a convenience functions for checking if the user pressed "STOP"
        val checkShouldCancelJob: () -> Unit = {
            //TODO("Logic here if state is correct before cancelling)

             //bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
        }

        //Execute the use case
        captureBitmaps.execute(
            capturingViewBounds = state.capturingViewBounds,
            window = window,
            view = view
        ).onEach { dataState ->
            //if the user hits the "STOP" button, complete the job by canceling
            checkShouldCancelJob()
            when(dataState){
                is DataState.Data -> {
                    dataState.data?.let { bitmaps ->
                        _state.value = state.copy(capturedBitmaps = bitmaps)
                    }
                }

                is DataState.Error -> {
                    //For this use-case if an error occurs we need to stop the job
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)
                    //TODO("UPDATE LOADING STATE")
                    publishErrorEvent(ErrorEvent(message = dataState.message))
                }

                is DataState.Loading -> {
                    //TODO: Update the loading state
                }

            }

        }.flowOn(dispatcher).launchIn(viewModelScope + bitmapCaptureJob)
            .invokeOnCompletion { throwable ->
                //If completes normally or if cancelled
                //TODO("Update laoding state")
                val onSuccess: () -> Unit = {
                    //TODO: Build the gif from the list of captured bitmaps
                    val newState = _state.value
                    if (newState is MainState.DisplayBackgroundAsset) {
                        Log.d("TAG", "Num bitmaps ${newState.capturedBitmaps.size}")
                    }
                }

                //if the throwable is null or the message is capture bitmap succes it was successful
                when (throwable) {
                    null -> onSuccess()
                    else -> {
                        if (throwable.message == CAPTURE_BITMAP_SUCCESS) {
                            onSuccess()
                        } else {
                            publishErrorEvent(
                                ErrorEvent(message = throwable.message ?: CAPTURE_BITMAP_ERROR)
                            )
                        }
                    }
                }

            }

    }
}
