package com.amazon.ivs.chatdemo.repository.models

enum class ErrorCode(val code: Int) {
    DISCONNECTED_BY_MODERATOR(1001),
    SOCKET_RESTART(3000)
}
