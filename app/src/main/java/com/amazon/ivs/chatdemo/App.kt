package com.amazon.ivs.chatdemo

import android.app.Application
import android.content.res.Configuration
import com.amazon.ivs.chatdemo.common.LineNumberDebugTree
import com.amazon.ivs.chatdemo.common.ScreenDensities
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ScreenDensities.onConfigChanged(this)
    }
}
