package com.amazon.ivs.chatdemo.common

import android.content.Context

object ScreenDensities {
    var density = 1f
        private set
    var fontDensity = 1f
        private set

    fun onConfigChanged(context: Context) {
        density = context.resources.displayMetrics.density
        fontDensity = context.resources.displayMetrics.scaledDensity
    }
}
