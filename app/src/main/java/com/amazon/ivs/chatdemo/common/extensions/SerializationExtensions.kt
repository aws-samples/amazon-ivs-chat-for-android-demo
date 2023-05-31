package com.amazon.ivs.chatdemo.common.extensions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

inline fun <reified T> T.toJson() = json.encodeToString(this)

inline fun <reified T> String.asObject(onException: () -> Unit = {}): T? {
    var obj: T? = null
    try {
        obj = json.decodeFromString(this)
    } catch (e: Exception) {
        Timber.d("Serialization exception: ${e.message}")
        onException()
    }
    return obj
}
