package com.shido.giffity.usecases.build_gif.di

import com.shido.giffity.usecases.build_gif.BuildGif
import com.shido.giffity.usecases.build_gif.BuildGifUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class BuildGifModule {
    @Binds
    abstract fun provideBuildGif(buildGifUseCase: BuildGifUseCase): BuildGif
}
