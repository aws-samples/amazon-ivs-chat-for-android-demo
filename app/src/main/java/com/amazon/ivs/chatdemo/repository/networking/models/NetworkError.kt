package com.amazon.ivs.chatdemo.repository.networking.models

sealed class NetworkError(
    val errorMessage: String,
    val errorCode: Int? = null
) {
    class Error(errorMessage: String, errorCode: Int? = null) : NetworkError(errorMessage, errorCode)
    class MessageSendFailed(errorMessage: String, errorCode: Int) : NetworkError(errorMessage, errorCode)
    class MessageDeleteFailed(errorMessage: String, errorCode: Int) : NetworkError(errorMessage, errorCode)
}
