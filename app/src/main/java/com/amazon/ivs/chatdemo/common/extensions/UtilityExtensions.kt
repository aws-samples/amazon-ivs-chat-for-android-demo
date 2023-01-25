package com.amazon.ivs.chatdemo.common.extensions

import com.amazon.ivs.chatdemo.common.DATE_FORMAT
import com.amazon.ivs.chatdemo.common.ScreenDensities
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

val Int.sp: Float get() = if (this == 0) 0f else floor(ScreenDensities.fontDensity * this.toDouble()).toFloat()
val Int.dp: Int get() = if (this == 0) 0 else floor(ScreenDensities.density * this.toDouble()).toInt()

fun String.toDate(format: String = DATE_FORMAT): Date? =  SimpleDateFormat(format, Locale.getDefault()).parse(this)
