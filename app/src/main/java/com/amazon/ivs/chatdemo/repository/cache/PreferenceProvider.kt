package com.amazon.ivs.chatdemo.repository.cache

import android.content.Context
import com.amazon.ivs.chatdemo.BuildConfig
import com.amazon.ivs.chatdemo.common.PREFERENCES_NAME
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferenceProvider(context: Context) {
    var customUrl by stringPreference()
    var isUsingCustomUrl by booleanPreference()
    var useBulletChat by booleanPreference()
    val customPlaybackUrl get() = customUrl.takeIf { isUsingCustomUrl && !it.isNullOrBlank() }
    val playbackUrl get() = customPlaybackUrl ?: BuildConfig.STREAM_URL

    private val sharedPreferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    private fun stringPreference() = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(property.name, null)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            sharedPreferences.edit().putString(property.name, value).apply()
        }
    }

    private fun booleanPreference() = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getBoolean(property.name, false)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            sharedPreferences.edit().putBoolean(property.name, value).apply()
        }
    }
}
