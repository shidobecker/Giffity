package com.shido.giffity.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shido.giffity.di.IO
import com.shido.giffity.domain.DataState
import com.shido.giffity.domain.ErrorEvent
import com.shido.giffity.domain.providers.VersionProvider
import com.shido.giffity.domain.whenState
import com.shido.giffity.usecases.build_gif.BuildGif
import com.shido.giffity.usecases.capture_bitmaps.CaptureBitmaps
import com.shido.giffity.usecases.capture_bitmaps.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.shido.giffity.usecases.capture_bitmaps.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.shido.giffity.usecases.clear_gif_cache.ClearGifCache
import com.shido.giffity.usecases.resize_gif.ResizeGif
import com.shido.giffity.usecases.resize_gif.ResizeGifInteractor.Companion.RESIZE_ERROR_MESSAGE
import com.shido.giffity.usecases.save_gif.SaveGifToExternalStorage
import com.shido.giffity.usecases.save_gif.SaveGifToExternalStorageUseCase.Companion.SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
import com.shido.giffity.ui.MainState
import com.shido.giffity.ui.asDisplayBackgroundAssetState
import com.shido.giffity.ui.asDisplayGifState
import com.shido.giffity.ui.executeWhenMainStateIsDisplayBackgroundAssetState
import com.shido.giffity.ui.isDisplayBackgroundAssetState
import com.shido.giffity.ui.isDisplayGifState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @IO private val ioDispatcher: CoroutineDispatcher,
    private val captureBitmaps: CaptureBitmaps,
    private val saveGifToExternalStorage: SaveGifToExternalStorage,
    private val buildGif: BuildGif,
    private val resizeGif: ResizeGif,
    private val clearGifCache: ClearGifCache,
    private val versionProvider: VersionProvider,
) : ViewModel() {


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


    @RequiresApi(Build.VERSION_CODES.O)
    fun runBitmapCaptureJob(contentResolver: ContentResolver, view: View, window: Window) {
        check(state.isDisplayBackgroundAssetState()) { "Invalid state  $state" }
        //Set the job is running
        updateState(
            (state.asDisplayBackgroundAssetState()).copy(
                bitmapCaptureLoadingState = DataState.Loading.LoadingState.Active(
                    0f
                )
            )
        )

        //Stop the job if a user presses "STOP".
        val bitmapCaptureJob = Job()

        checkShouldCancelJob(state.value, bitmapCaptureJob)

        //Execute the use case
        captureBitmaps.execute(
            capturingViewBounds = (state.asDisplayBackgroundAssetState()).capturingViewBounds,
            window = window,
            view = view
        ).onEach { dataState ->
            checkShouldCancelJob(state.value, bitmapCaptureJob)

            when (dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps ->
                        updateState(
                            (state.asDisplayBackgroundAssetState()).copy(
                                capturedBitmaps = bitmaps
                            )
                        )
                    }
                }

                is DataState.Error -> {
                    //For this use-case if an error occurs we need to stop the job
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)

                    updateState(
                        (state.asDisplayBackgroundAssetState()).copy(
                            bitmapCaptureLoadingState = DataState.Loading.LoadingState.Idle
                        )
                    )
                    publishErrorEvent(ErrorEvent(message = dataState.message))
                }

                is DataState.Loading -> {
                    updateState(
                        (state.asDisplayBackgroundAssetState()).copy(
                            bitmapCaptureLoadingState = dataState.loading
                        )
                    )
                }

            }

        }.flowOn(ioDispatcher).launchIn(viewModelScope + bitmapCaptureJob)
            .invokeOnCompletion { throwable ->
                updateState(
                    (state.asDisplayBackgroundAssetState()).copy(
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
        updateState((state.asDisplayBackgroundAssetState()).copy(bitmapCaptureLoadingState = DataState.Loading.LoadingState.Idle))
    }


    private fun buildGif(contentResolver: ContentResolver) {
        check(state.value is MainState.DisplayBackgroundAsset) { "Invalid state ${state.value}" }

        val capturedBitmaps = (state.asDisplayBackgroundAssetState()).capturedBitmaps

        check(capturedBitmaps.isNotEmpty()) { "You have no bitmaps to build a gif with!" }

        updateState((state.asDisplayBackgroundAssetState()).copy(loadingState = DataState.Loading.LoadingState.Active()))



        buildGif.execute(contentResolver = contentResolver, bitmaps = capturedBitmaps)
            .onEach { dataState ->

                dataState.whenState(onLoading = { loadingState ->

                    //Need to check here since there is a state change to DisplayGif and loading
                    //Emissions can still come after the job is complete
                    state.executeWhenMainStateIsDisplayBackgroundAssetState { state ->
                        updateState(
                            state.copy(loadingState = loadingState.loading)
                        )
                    }

                }, onError = { message ->

                    publishErrorEvent(ErrorEvent(message = message))

                    state.executeWhenMainStateIsDisplayBackgroundAssetState {
                        updateState(
                            (state.asDisplayBackgroundAssetState()).copy(
                                loadingState = DataState.Loading.LoadingState.Idle
                            )
                        )
                    }

                }, onData = { gifResult ->
                    (state.asDisplayBackgroundAssetState()).let { displayBackgroundAsset ->
                        val gifSize = gifResult?.gifSize ?: 0
                        val gifUri = gifResult?.uri
                        updateState(
                            MainState.DisplayGif(
                                gifUri = gifUri,
                                originalGifSize = gifSize,
                                backgroundAssetUri = displayBackgroundAsset.backgroundAssetUri,
                                resizedGifUri = null,
                                adjustedBytes = gifSize,
                                sizePercentage = 100,
                                capturedBitmaps = displayBackgroundAsset.capturedBitmaps
                            )
                        )

                    }
                })

            }.flowOn(ioDispatcher).launchIn(viewModelScope)


    }

    fun resizeGif(contentResolver: ContentResolver) {
        check(state.isDisplayGifState()) { "Invalid State ${state.value}" }
        state.asDisplayGifState().let {
            //Calculate the target size of the resulting gif
            val targetSize = it.originalGifSize * (it.sizePercentage.toFloat() / 100)

            resizeGif.execute(contentResolver = contentResolver,
                capturedBitmaps = it.capturedBitmaps,
                originalGifSize = it.originalGifSize.toFloat(),
                targetSize = targetSize,
                discardCachedGif = { uri ->
                    discardCachedGif(uri)
                }
            ).onEach { dataState ->
                when (dataState) {
                    is DataState.Loading -> {
                        updateState(
                            state.asDisplayGifState()
                                .copy(resizeGifLoadingState = dataState.loading)
                        )
                    }

                    is DataState.Data -> {
                        dataState.data?.let { data ->
                            val copiedState = state.asDisplayGifState().copy(
                                resizedGifUri = data.uri,
                                adjustedBytes = data.gifSize
                            )
                            updateState(copiedState)
                        } ?: publishErrorEvent(
                            ErrorEvent(
                                id = UUID.randomUUID().toString(),
                                message = RESIZE_ERROR_MESSAGE
                            )
                        )
                    }

                    is DataState.Error -> {
                        publishErrorEvent(
                            ErrorEvent(
                                id = UUID.randomUUID().toString(),
                                message = dataState.message
                            )
                        )
                    }
                }
            }.onCompletion {
                updateState(
                    state.asDisplayGifState()
                        .copy(resizeGifLoadingState = DataState.Loading.LoadingState.Idle)
                )
            }.flowOn(ioDispatcher)
                .launchIn(viewModelScope)
        }
    }


    fun resetGifToOriginal() {
        check(state.value is MainState.DisplayGif) { "Invalid State ${state.value}" }
        (state.asDisplayGifState()).run {
            resizedGifUri?.let { resizedGifUri ->
                discardCachedGif(resizedGifUri)
            }
            updateState(
                (state.asDisplayGifState()).copy(
                    resizedGifUri = null,
                    adjustedBytes = originalGifSize,
                    sizePercentage = 100
                )
            )
        }

    }

    fun updateAdjustedBytes(adjustedBytes: Int) {
        check(state.isDisplayGifState()) { "Invalid State ${state.value}" }
        val displayGifState = state.asDisplayGifState()
        updateState(displayGifState.copy(adjustedBytes = adjustedBytes))
    }

    fun updateSizePercentage(sizePercentage: Int) {
        check(state.isDisplayGifState()) { "Invalid State ${state.value}" }
        val displayGifState = state.asDisplayGifState()

        updateState(displayGifState.copy(sizePercentage = sizePercentage))
    }

    companion object {
        const val DISCARD_CACHED_GIF_ERROR = "Failed To delete cached gif ar uri"

        private fun discardCachedGif(uri: Uri) {
            val file = File(uri.path)
            val success = file.delete()
            if (!success) {
                throw Exception("$DISCARD_CACHED_GIF_ERROR")
            }
        }
    }


    private fun clearCachedFiles() {
        clearGifCache.execute().onEach {
            //Don't update the UI here. Should just succeed or fail silently
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    fun deleteGif() {
        clearCachedFiles()
        check(state.isDisplayGifState()) { "deleteGif : Invalid State: ${state.value}" }
        //Resetting the state
        updateState(MainState.DisplayBackgroundAsset(backgroundAssetUri = (state.asDisplayGifState()).backgroundAssetUri))
    }

    fun saveGif(
        contentResolver: ContentResolver,
        context: Context,
        launchPermissionRequest: () -> Unit, checkFilePermissions: () -> Boolean
    ) {
        check(state.value is MainState.DisplayGif) { "Incorrect state ${state.value}" }

        if (versionProvider.provideVersion() < Build.VERSION_CODES.Q && checkFilePermissions().not()) {
            launchPermissionRequest()
            return
        }

        //Use resized gif uri if it was resized once
        val uriToSave =
            state.asDisplayGifState().let { it.resizedGifUri ?: it.gifUri } ?: throw Exception(
                SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
            )
        saveGifToExternalStorage.execute(
            contentResolver = contentResolver,
            context = context,
            cachedUri = uriToSave,
            checkFilePermissions = checkFilePermissions
        ).onEach { dataState ->
            dataState.whenState(onLoading = {
                updateState(state.asDisplayGifState().copy(saveGifLoadingState = it.loading))
            }, onError = {
                publishErrorEvent(ErrorEvent(message = it))
            }, onData = {
                showToast(message = "Saved")
            })
        }.onCompletion {
            clearCachedFiles()
            updateState(MainState.DisplayBackgroundAsset(backgroundAssetUri = state.asDisplayGifState().backgroundAssetUri))

        }.flowOn(ioDispatcher).launchIn(viewModelScope)

    }

}
