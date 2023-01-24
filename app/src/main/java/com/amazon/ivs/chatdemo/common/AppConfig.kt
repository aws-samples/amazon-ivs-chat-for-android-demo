package com.amazon.ivs.chatdemo.common

import android.content.Context

object AppConfig {

    var density = 1f
    var fontDensity = 1f

    fun onConfigChanged(context: Context) {
        density = context.resources.displayMetrics.density
        fontDensity = context.resources.displayMetrics.scaledDensity
    }
}
