package com.shido.giffity.viewmodel

import android.content.ContentResolver
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shido.giffity.domain.CacheProvider
import com.shido.giffity.domain.DataState
import com.shido.giffity.domain.ErrorEvent
import com.shido.giffity.domain.RealVersionProvider
import com.shido.giffity.domain.whenState
import com.shido.giffity.interactors.BuildGifInteractor
import com.shido.giffity.interactors.CaptureBitmaps
import com.shido.giffity.interactors.CaptureBitmapsInteractor
import com.shido.giffity.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.shido.giffity.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.shido.giffity.interactors.ClearGifCache
import com.shido.giffity.interactors.ClearGifCacheInteractor
import com.shido.giffity.interactors.PixelCopyJobInteractor
import com.shido.giffity.ui.MainState
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Dispatchers
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
    private val mainDispatcher = Dispatchers.Main

    private val versionProvider = RealVersionProvider()

    private val pixelCopy = PixelCopyJobInteractor()

    private val captureBitmaps: CaptureBitmaps = CaptureBitmapsInteractor(
        pixelCopyJob = pixelCopy, mainDispatcher = mainDispatcher, versionProvider = versionProvider
    )

    private var cacheProvider: CacheProvider? = null

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
    fun runBitmapCaptureJob(contentResolver: ContentResolver, view: View, window: Window) {
        check(state.value is MainState.DisplayBackgroundAsset) { "Invalid state  $state" }
        //Set the job is running
        updateState(
            (state.value as MainState.DisplayBackgroundAsset).copy(
                bitmapCaptureLoadingState = DataState.Loading.LoadingState.Active(
                    0f
                )
            )
        )

        //Stop the job if a user presses "STOP".
        val bitmapCaptureJob = Job()

        //Create a convenience functions for checking if the user pressed "STOP"
        /*val checkShouldCancelJob: (MainState) -> Unit = { mainState ->
            val shouldCancel = when (mainState) {
                is MainState.DisplayBackgroundAsset -> {
                    mainState.bitmapCaptureLoadingState !is DataState.Loading.LoadingState.Active
                }

                else -> true
            }

            if (shouldCancel) {
                bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
            }
        }*/

        checkShouldCancelJob(state.value, bitmapCaptureJob)


        //Execute the use case
        captureBitmaps.execute(
            capturingViewBounds = (state.value as MainState.DisplayBackgroundAsset).capturingViewBounds,
            window = window,
            view = view
        ).onEach { dataState ->
            checkShouldCancelJob(state.value, bitmapCaptureJob)

            when (dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps ->
                        updateState(
                            (state.value as MainState.DisplayBackgroundAsset).copy(
                                capturedBitmaps = bitmaps
                            )
                        )
                    }
                }

                is DataState.Error -> {
                    //For this use-case if an error occurs we need to stop the job
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)

                    updateState(
                        (state.value as MainState.DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = DataState.Loading.LoadingState.Idle
                        )
                    )
                    publishErrorEvent(ErrorEvent(message = dataState.message))
                }

                is DataState.Loading -> {
                    updateState(
                        (state.value as MainState.DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = dataState.loading
                        )
                    )
                }

            }

        }.flowOn(dispatcher).launchIn(viewModelScope + bitmapCaptureJob)
            .invokeOnCompletion { throwable ->
                updateState(
                    (state.value as MainState.DisplayBackgroundAsset).copy(
                        bitmapCaptureLoadingState = DataState.Loading.LoadingState.Idle
                    )
                )

                val onSuccess: () -> Unit = {
                    buildGif(contentResolver)
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


    private fun checkShouldCancelJob(mainState: MainState, bitmapCaptureJob: CompletableJob) {
        val shouldCancel = when (mainState) {
            is MainState.DisplayBackgroundAsset -> {
                mainState.bitmapCaptureLoadingState !is DataState.Loading.LoadingState.Active
            }

            else -> true
        }

        if (shouldCancel) {
            bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
        }
    }

    fun endBitmapCaptureJob() {
        updateState((state.value as MainState.DisplayBackgroundAsset).copy(bitmapCaptureLoadingState = DataState.Loading.LoadingState.Idle))
    }


    //TODO: This will be removed once we add hilt
    fun setCacheProvider(cacheProvider: CacheProvider) {
        this.cacheProvider = cacheProvider
    }

    private fun buildGif(contentResolver: ContentResolver) {
        check(state.value is MainState.DisplayBackgroundAsset) { "Invalid state ${state.value}" }

        val capturedBitmaps = (state.value as MainState.DisplayBackgroundAsset).capturedBitmaps

        check(capturedBitmaps.isNotEmpty()) { "You have no bitmaps to build a gif with!" }

        updateState((state.value as MainState.DisplayBackgroundAsset).copy(loadingState = DataState.Loading.LoadingState.Active()))

        //TODO: This will be injected into the viewmodel later

        cacheProvider?.let {
            val buildGif = BuildGifInteractor(cacheProvider = it, versionProvider = versionProvider)

            buildGif.execute(contentResolver = contentResolver, bitmaps = capturedBitmaps)
                .onEach { dataState ->

                    dataState.whenState(onLoading = { loadingState ->

                        //Need to check here since there is a state change to DisplayGif and loading
                        //Emissions can still come after the job is complete
                        if (state.value is MainState.DisplayBackgroundAsset) {
                            updateState(
                                (state.value as MainState.DisplayBackgroundAsset).copy(
                                    loadingState = loadingState.loading
                                )
                            )
                        }

                    }, onError = { message ->

                        publishErrorEvent(ErrorEvent(message = message))

                        updateState(
                            (state.value as MainState.DisplayBackgroundAsset).copy(
                                loadingState = DataState.Loading.LoadingState.Idle
                            )
                        )

                    }, onData = { gifResult ->
                        (state.value as MainState.DisplayBackgroundAsset).let {
                            val gifSize = gifResult?.gifSize ?: 0
                            val gifUri = gifResult?.uri

                            updateState(
                                MainState.DisplayGif(
                                    gifUri = gifUri,
                                    originalGifSize = gifSize,
                                    backgroundAssetUri = it.backgroundAssetUri
                                )
                            )

                        }
                    })

                }.flowOn(IO).launchIn(viewModelScope)

        }


    }

    private fun clearCachedFiles() {
        //TODO WILL BE INJECTING THIS LATER
        cacheProvider?.let { provider ->
            val clearGifCache: ClearGifCache = ClearGifCacheInteractor(
                cacheProvider = provider,
            )
            clearGifCache.execute().onEach {
                //Don't update the UI here. Should just succeed or fail silently
            }.flowOn(dispatcher).launchIn(viewModelScope)
        }
    }

    fun deleteGif() {
        clearCachedFiles()
        check(state.value is MainState.DisplayGif) { "deleteGif : Invalid State: ${state.value}" }
        //Resetting the state
        updateState(MainState.DisplayBackgroundAsset(backgroundAssetUri = (state.value as MainState.DisplayGif).backgroundAssetUri))
    }

}
