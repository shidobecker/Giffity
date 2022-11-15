package com.shido.giffity

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.shido.giffity.ui.MainState
import com.shido.giffity.ui.compose.BackgroundAssetImage
import com.shido.giffity.ui.compose.SelectBackgroundAsset
import com.shido.giffity.ui.compose.theme.GiffityTheme
import com.shido.giffity.viewmodel.MainViewModelImage

class MainActivityImage : ComponentActivity() {

    private val viewModel: MainViewModelImage by viewModels()

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let { uri ->
                    when (val state = viewModel.state.value) {
                        is MainState.DisplayBackgroundAsset, is MainState.DisplaySelectBackgroundAsset -> {
                            viewModel.updateState(
                                MainState.DisplayBackgroundAssetImage(
                                    backgroundAssetUri = uri,
                                    capturedBitmap = null
                                )
                            )
                        }

                        else -> throw Exception("Invalid State ${state}")
                    }
                    Log.d("Tag", "Got the uri${uri}")
                }
            } else {
                viewModel.toastShow(message = "Something went wrong cropping the image")
            }
        }

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                cropAssetLauncher.launch(
                    CropImageContractOptions(
                        uri = uri, cropImageOptions = CropImageOptions(
                            guidelines = CropImageView.Guidelines.ON
                        )
                    )
                )
            } ?: viewModel.toastShow(message = "Something wrong when selecting the image")

        }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectFlows()

        setContent {
            GiffityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {

                    val state = viewModel.state.value
                    val view = LocalView.current

                    Column(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            is MainState.Initial -> {
                                //TODO SHOW LOADING
                                viewModel.updateState(MainState.DisplaySelectBackgroundAsset)
                            }

                            is MainState.DisplaySelectBackgroundAsset -> {
                                SelectBackgroundAsset(launchImagePicker = {
                                    launchPicker()
                                })
                            }

                            is MainState.DisplayBackgroundAsset -> {

                            }

                            is MainState.DisplayBackgroundAssetImage -> {
                                BackgroundAssetImage(backgroundAssetUri = state.backgroundAssetUri,
                                    capturedBitmap = state.capturedBitmap,
                                    updateCapturingViewBounds = { rect ->
                                        viewModel.updateState(state.copy(capturingViewBounds = rect))
                                    },
                                    startBitmapCaptureJob = {
                                        viewModel.captureScreenshot(view = view, window = window)
                                    },
                                    launchImagePicker = {
                                        launchPicker()
                                    })
                            }
                        }
                    }

                }
            }
        }
    }

    private fun collectFlows() {
        lifecycleScope.launchWhenResumed {
            viewModel.toastEventRelay.collect { toastEvent ->
                toastEvent?.let { event ->
                    Toast.makeText(this@MainActivityImage, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun launchPicker() {
        backgroundAssetPickerLauncher.launch("image/*")
    }
}

