package com.amazon.ivs.chatdemo.common.extensions

import com.amazon.ivs.chatdemo.common.DATE_FORMAT
import java.text.SimpleDateFormat
import java.util.*

fun String.toDate(format: String = DATE_FORMAT): Date? =  SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(this)
