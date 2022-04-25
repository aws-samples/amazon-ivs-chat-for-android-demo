package com.amazon.ivs.chatdemo

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.amazon.ivs.chatdemo.common.LineNumberDebugTree
import com.amazon.ivs.chatdemo.injection.DaggerInjectionComponent
import com.amazon.ivs.chatdemo.injection.InjectionComponent
import com.amazon.ivs.chatdemo.injection.InjectionModule
import timber.log.Timber

class App : Application(), ViewModelStoreOwner {

    private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }

    override fun onCreate() {
        super.onCreate()

        component = DaggerInjectionComponent.builder().injectionModule(InjectionModule(this)).build()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }
    }

    override fun getViewModelStore() = appViewModelStore

    companion object {
        lateinit var component: InjectionComponent
            private set
    }
}
