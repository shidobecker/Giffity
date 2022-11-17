package com.shido.giffity.domain

import android.os.Build

interface VersionProvider {

    fun provideVersion(): Int

}

class RealVersionProvider constructor() : VersionProvider {
    override fun provideVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}