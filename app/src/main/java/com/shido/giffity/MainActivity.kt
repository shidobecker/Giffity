package com.shido.giffity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.shido.giffity.ui.MainState
import com.shido.giffity.ui.compose.BackgroundAsset
import com.shido.giffity.ui.compose.SelectBackgroundAsset
import com.shido.giffity.ui.compose.theme.GiffityTheme

class MainActivity : ComponentActivity() {

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let { uri ->
                    when (val state = _state.value) {
                        is MainState.DisplayBackgroundAsset, is MainState.DisplaySelectBackgroundAsset -> {
                            _state.value =
                                MainState.DisplayBackgroundAsset(backgroundAssetUri = uri)
                        }

                        else -> throw Exception("Invalid State ${state}")
                    }
                    Log.d("Tag", "Got the uri${uri}")
                }
            } else {
                Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            cropAssetLauncher.launch(
                CropImageContractOptions(
                    uri = uri, cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON
                    )
                )
            )
        }

    private val _state: MutableState<MainState> = mutableStateOf(MainState.Initial)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GiffityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {

                    val state = _state.value
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            is MainState.Initial -> {
                                //TODO SHOW LOADING
                                _state.value = MainState.DisplaySelectBackgroundAsset
                            }

                            is MainState.DisplaySelectBackgroundAsset -> {
                                SelectBackgroundAsset(launchImagePicker = {
                                    launchPicker()
                                })
                            }

                            is MainState.DisplayBackgroundAsset -> {
                                BackgroundAsset(backgroundAssetUri = state.backgroundAssetUri,
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


    private fun launchPicker() {
        backgroundAssetPickerLauncher.launch("image/*")
    }
}

