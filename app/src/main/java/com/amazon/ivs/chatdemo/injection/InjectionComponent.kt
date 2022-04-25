package com.amazon.ivs.chatdemo.injection

import com.amazon.ivs.chatdemo.ui.MainActivity
import com.amazon.ivs.chatdemo.ui.SettingsActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [InjectionModule::class])
interface InjectionComponent {
    fun inject(target: MainActivity)
    fun inject(target: SettingsActivity)
}
