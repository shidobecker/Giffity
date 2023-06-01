package com.shido.giffity.viewmodel

import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.shido.giffity.domain.ErrorEvent
import com.shido.giffity.usecases.pixel_copy_job.PixelCopyJob
import com.shido.giffity.usecases.pixel_copy_job.PixelCopyJobInteractor
import com.shido.giffity.ui.MainState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModelImage : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy = PixelCopyJobInteractor()
/*    private val captureBitmaps: CaptureBitmaps = CaptureBitmapsInteractor(
        pixelCopyJob = pixelCopy,
    )*/

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

    fun showToast(id: String = UUID.randomUUID().toString(), message: String) {
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
     @RequiresApi(Build.VERSION_CODES.O)
     fun captureScreenshot(view: View, window: Window) {
         val state = state.value
         check(state is MainState.DisplayBackgroundAssetImage) { "Invalid state  $state" }
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
     }

}
