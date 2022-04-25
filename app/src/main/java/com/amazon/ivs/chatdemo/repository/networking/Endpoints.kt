package com.amazon.ivs.chatdemo.repository.networking

import com.amazon.ivs.chatdemo.repository.models.DeleteAllMessagesRequest
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.HTTP

interface Endpoints {

    @HTTP(method = "POST", path = "auth", hasBody = true)
    suspend fun authenticate(@Body body: AuthenticationBody): ResponseBody

    @HTTP(method = "POST", path = "event", hasBody = true)
    suspend fun deleteAllMessages(@Body body: DeleteAllMessagesRequest): ResponseBody
}
