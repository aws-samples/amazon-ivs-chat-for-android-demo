package com.amazon.ivs.chatdemo.repository.models

enum class NetworkError(
    var rawError: String? = null,
    var rawCode: Int = -1,
) {
    RAW_ERROR,
    CONNECTION_FAILED,
    MESSAGE_RECEIVE_FAILED,
    MESSAGE_SEND_FAILED,
    MESSAGE_DELETE_FAILED;
}
