package com.amazon.ivs.chatdemo.repository.networking

import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationBody
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationResponse
import retrofit2.http.Body
import retrofit2.http.HTTP

interface Endpoints {
    @HTTP(method = "POST", path = "auth", hasBody = true)
    suspend fun authenticate(@Body body: AuthenticationBody): AuthenticationResponse
}
