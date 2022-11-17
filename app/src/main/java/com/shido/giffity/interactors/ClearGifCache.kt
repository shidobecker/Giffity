package com.shido.giffity.interactors

import com.shido.giffity.domain.CacheProvider
import com.shido.giffity.domain.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ClearGifCache {

    fun execute(): Flow<DataState<Boolean>>

}

class ClearGifCacheInteractor constructor(private val cacheProvider: CacheProvider) :
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