package com.shido.giffity.usecases.clear_gif_cache

import com.shido.giffity.domain.providers.CacheProvider
import com.shido.giffity.domain.DataState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ClearGifCache {

    fun execute(): Flow<DataState<Boolean>>

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ClearGifCacheModule {
    @Binds
    abstract fun provideClearGifCacheUseCase(clearGifCacheInteractor: ClearGifCacheInteractor): ClearGifCache
}


class ClearGifCacheInteractor @Inject constructor(private val cacheProvider: CacheProvider) :
    ClearGifCache {

    override fun execute(): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading(DataState.Loading.LoadingState.Active()))

        try {
            clearGifCache(cacheProvider)

            emit(DataState.Data(true))

        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CLEAR_CACHED_FILES_ERROR))
        }

    }

    companion object {
        const val CLEAR_CACHED_FILES_ERROR = "An error occurred deleting the cached files"

        private fun clearGifCache(cacheProvider: CacheProvider) {
            val internalStorageDirectory = cacheProvider.gifCache()
            val files = internalStorageDirectory.listFiles()
            files?.forEach { it.delete() }
        }

    }
}