package com.amazon.ivs.chatdemo.repository.models

import com.amazon.ivs.chatdemo.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val DELETE_MESSAGE_DEFAULT_REASON = "Deleted by moderator"
private const val KICK_USER_DEFAULT_REASON = "Kicked by moderator"
private const val KEY_USER_ID = "userId"

@Serializable
data class DeleteMessageRequest(
    val id: String,
    val action: String = Actions.DELETE_MESSAGE.value,
    val reason: String = DELETE_MESSAGE_DEFAULT_REASON,
)

@Serializable
data class DeleteMessageResponse(
    @SerialName("Attributes") val attributes: DeleteMessageAttributes = DeleteMessageAttributes(),
)

@Serializable
data class DeleteAllMessagesRequest(
    val arn: String = BuildConfig.CHAT_ROOM_ID,
    val eventName: String = EventNames.DELETE_BY_USER.value,
    val eventAttributes: HashMap<String, String> = hashMapOf()
) {
    fun setUserId(userId: String) {
        eventAttributes[KEY_USER_ID] = userId
    }
}

@Serializable
@SerialName("Attributes")
data class DeleteMessageAttributes(
    @SerialName("MessageID") val messageId: String = "",
)

@Serializable
data class KickUserRequest(
    val userId: String,
    val action: String = Actions.DISCONNECT_USER.value,
    val reason: String = KICK_USER_DEFAULT_REASON
)

@Serializable
data class KickUserResponse(
    @SerialName("Attributes") val attributes: HashMap<String, String> = hashMapOf()
) {
    val userId = attributes[KEY_USER_ID]
}
