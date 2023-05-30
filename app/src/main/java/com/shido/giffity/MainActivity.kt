package com.shido.giffity

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.shido.giffity.domain.RealCacheProvider
import com.shido.giffity.ui.MainState
import com.shido.giffity.ui.compose.BackgroundAsset
import com.shido.giffity.ui.compose.Gif
import com.shido.giffity.ui.compose.SelectBackgroundAsset
import com.shido.giffity.ui.compose.theme.GiffityTheme
import com.shido.giffity.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imageLoader: ImageLoader

    fun checkFilePermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    private val externalStoragePermissionRequest = this@MainActivity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                viewModel.showToast(message = "To enable this permission you'll have to do so in system settings for this app.")
            }
        }
    }

    private fun launchPermissionRequest() {
        externalStoragePermissionRequest.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
    }

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let { uri ->
                    when (val state = viewModel.state.value) {
                        is MainState.DisplayBackgroundAsset, is MainState.DisplaySelectBackgroundAsset -> {
                            viewModel.updateState(
                                MainState.DisplayBackgroundAsset(
                                    backgroundAssetUri = uri
                                )
                            )
                        }

                        else -> throw Exception("Invalid State ${state}")
                    }
                    Log.d("Tag", "Got the uri${uri}")
                }
            } else {
                viewModel.showToast(message = "Something went wrong cropping the image")
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
            } ?: viewModel.showToast(message = "Something wrong when selecting the image")

        }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO: Remove this when hilt
        viewModel.setCacheProvider(RealCacheProvider(application))

        //TODO: Well be injeting the image loader later when we add hilt
        imageLoader = ImageLoader.Builder(application).components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }.build()

        collectFlows()

        setContent {
            GiffityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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

                            is MainState.DisplayBackgroundAssetImage -> {

                            }

                            is MainState.DisplayBackgroundAsset -> {
                                BackgroundAsset(
                                    backgroundAssetUri = state.backgroundAssetUri,
                                    updateCapturingViewBounds = { rect ->
                                        viewModel.updateState(state.copy(capturingViewBounds = rect))
                                    },
                                    startBitmapCaptureJob = {
                                        viewModel.runBitmapCaptureJob(
                                            contentResolver = contentResolver,
                                            view = view,
                                            window = window
                                        )
                                    },
                                    stopBitmapCaptureJob = viewModel::endBitmapCaptureJob,
                                    bitmapCaptureLoadingState = state.bitmapCaptureLoadingState,
                                    launchImagePicker = {
                                        launchPicker()
                                    },
                                    loadingState = state.loadingState
                                )
                            }

                            is MainState.DisplayGif -> {
                                Gif(
                                    gifUri = state.resizedGifUri ?: state.gifUri,
                                    imageLoader = imageLoader,
                                    discardGif = viewModel::deleteGif,
                                    onSaveGif = {
                                        viewModel.saveGif(
                                            context = this@MainActivity,
                                            contentResolver = contentResolver,
                                            launchPermissionRequest = ::launchPermissionRequest,
                                            checkFilePermissions = ::checkFilePermissions
                                        )
                                    },
                                    gifSaveLoadingState = state.saveGifLoadingState,
                                    resetToOriginal = viewModel::resetGifToOriginal,
                                    isResizedGif = state.resizedGifUri != null,
                                    currentGifSize = state.originalGifSize,
                                    sizePercentage = state.sizePercentage,
                                    adjustedBytes = state.adjustedBytes,
                                    updateAdjustedBytes = viewModel::updateAdjustedBytes,
                                    updateSizePercentage = viewModel::updateSizePercentage,
                                    resizeGif = {
                                        viewModel.resizeGif(contentResolver = contentResolver)
                                    },
                                    gifResizingLoadingState = state.resizeGifLoadingState,
                                )
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
                    Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun launchPicker() {
        backgroundAssetPickerLauncher.launch("image/*")
    }


    /**
     * Android storage directories
     *
     * Internal Storage Directories:
     * Private -> Other apps can't see those files, only that app can see them
     * (Temporary gif) -> Cache -> Don't need to write permission
     *
     * External Storage Directories:
     * Public -> Other apps can see it
     * (Final gif) ->
     * Scope storage -> Don't need to ask for permission.
     * 28 or below -> Ask for permission
     *
     * SD Card -> Extra storage that is external as well = more external storage
     */


}

