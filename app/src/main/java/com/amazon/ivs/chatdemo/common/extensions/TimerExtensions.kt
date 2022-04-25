package com.amazon.ivs.chatdemo.common.extensions

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper

private var currentDelay = 0L
private var currentCallback = {}
private val timeoutHandler = Handler(Looper.getMainLooper())
private val timeoutRunnable = Runnable {
    currentDelay.onRepeat(currentCallback)
}

fun Long.onRepeat(onRepeat: () -> Unit) {
    timeoutHandler.removeCallbacks(timeoutRunnable)
    onRepeat()
    currentDelay = this
    currentCallback = onRepeat
    timeoutHandler.postDelayed(timeoutRunnable, this)
}

fun countDownTimer(time: Long, tickSize: Long = 1000L, onFinish: () -> Unit) = object : CountDownTimer(time, tickSize) {
    override fun onTick(p0: Long) {
        /* Ignored */
    }

    override fun onFinish() {
        onFinish()
    }
}
