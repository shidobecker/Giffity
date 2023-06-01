package com.shido.giffity.usecases.save_gif.di

import com.shido.giffity.usecases.save_gif.SaveGifToInternalStorage
import com.shido.giffity.usecases.save_gif.SaveGifToInternalStorageUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class SaveGifToInternalStorageModule {
    @Binds
    abstract fun provideSaveGifToInternalStorageUseCase(saveGifToInternalStorageUseCase: SaveGifToInternalStorageUseCase): SaveGifToInternalStorage
}
