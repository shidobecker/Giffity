package com.shido.giffity.domain

import android.app.Application
import java.io.File

interface CacheProvider {

    fun gifCache(): File

}

class RealCacheProvider constructor(private val app: Application) : CacheProvider {
    override fun gifCache(): File {
        val file = File("${app.cacheDir.path}/temp_gifs")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }
}