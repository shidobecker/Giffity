package com.shido.giffity.domain

import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton


interface VersionProvider {

    fun provideVersion(): Int

}

@Module
@InstallIn(SingletonComponent::class)
abstract class VersionProviderModule {
    @Binds
    abstract fun provideVersionProvider(versionProvider: RealVersionProvider): VersionProvider
}

@Singleton
class RealVersionProvider @Inject constructor() : VersionProvider {
    override fun provideVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}