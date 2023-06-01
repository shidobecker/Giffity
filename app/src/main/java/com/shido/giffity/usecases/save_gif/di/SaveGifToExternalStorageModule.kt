package com.shido.giffity.usecases.save_gif.di

import com.shido.giffity.usecases.save_gif.SaveGifToExternalStorage
import com.shido.giffity.usecases.save_gif.SaveGifToExternalStorageUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class SaveGifToExternalStorageModule {
    @Binds
    abstract fun provideSaveGifToExternalStorageUseCase(saveGifToExternalStorageUseCase: SaveGifToExternalStorageUseCase): SaveGifToExternalStorage
}
