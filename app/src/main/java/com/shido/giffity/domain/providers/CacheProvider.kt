package com.shido.giffity.domain.providers

import android.app.Application
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CacheProvider {

    fun gifCache(): File

}

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheProviderModule {
    @Binds
    abstract fun provideCacheProvider(cacheProvider: RealCacheProvider): CacheProvider
}

@Singleton
class RealCacheProvider @Inject constructor(private val app: Application) : CacheProvider {
    override fun gifCache(): File {
        val file = File("${app.cacheDir.path}/temp_gifs")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }
}