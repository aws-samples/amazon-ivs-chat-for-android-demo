package com.amazon.ivs.chatdemo.repository.networking.models

import com.amazon.ivs.chatdemo.common.extensions.toDate
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AuthenticationResponse(
    val token: String,
    val sessionExpirationTime: String,
    val tokenExpirationTime: String,
) {

    val sessionExpirationDate: Date? get() = sessionExpirationTime.toDate()
    val tokenExpirationDate: Date? get() = tokenExpirationTime.toDate()
}
