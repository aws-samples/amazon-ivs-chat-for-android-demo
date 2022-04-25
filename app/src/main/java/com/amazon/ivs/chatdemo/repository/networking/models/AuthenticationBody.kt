package com.amazon.ivs.chatdemo.repository.networking.models

import com.amazon.ivs.chatdemo.BuildConfig

data class AuthenticationBody(
    val arn: String = BuildConfig.CHAT_ROOM_ID,
    val userId: String = "0",
    val attributes: AuthenticationAttributes,
    var capabilities: List<String> = mutableListOf<String>().apply {
        if (attributes.username != null) {
            add("SEND_MESSAGE")
        }
    },
    val durationInMinutes: Int = 55
) {
    fun addModeratorPermissions(isModerator: Boolean) {
        val permissions = capabilities.toMutableList()
        if (isModerator) {
            // Add more permissions if needed
            permissions.add("DISCONNECT_USER")
            permissions.add("DELETE_MESSAGE")
        }
        capabilities = permissions
    }
}

data class AuthenticationAttributes(
    val username: String? = "",
    val avatar: String? = ""
)
