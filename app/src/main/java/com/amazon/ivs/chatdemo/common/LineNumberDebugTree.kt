package com.amazon.ivs.chatdemo.common

import timber.log.Timber

private const val TIMBER_TAG = "ChatDemo"

class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement) =
        "$TIMBER_TAG: (${element.fileName}:${element.lineNumber}) #${element.methodName} "
}
