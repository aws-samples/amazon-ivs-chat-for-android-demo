package com.amazon.ivs.chatdemo.repository.models

import com.amazon.ivs.chatdemo.common.MESSAGE_TIMEOUT
import com.amazon.ivs.chatdemo.common.STICKERS
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

const val KEY_USERNAME = "username"
const val KEY_AVATAR = "avatar"
const val EVENT_STICKER = "STICKER"
const val MESSAGE_TYPE = "MESSAGE"

@Serializable
data class ChatMessageRequest(
    @SerialName("id") var id: String = "",
    @SerialName("action") val action: String = Actions.SEND_MESSAGE.value,
    @SerialName("content") val content: String = "",
    @SerialName("attributes") val attributes: MessageAttributes? = null,
)

@Serializable
data class ChatMessageResponse(
    @SerialName("Type") val type: String = MESSAGE_TYPE,
    @SerialName("Id") val id: String = "",
    @SerialName("RequestId") val requestId: String = "",
    @SerialName("Attributes") val attributes: MessageAttributes? = null,
    @SerialName("Content") var content: String = "",
    @SerialName("Sender") val sender: Sender = Sender(),
    @Transient val timeStamp: Long = Date().time,
    @Transient var viewType: MessageViewType =
        if (attributes?.messageType == EVENT_STICKER) MessageViewType.STICKER else MessageViewType.MESSAGE
) {
    val isExpired get() = Date().time - timeStamp > MESSAGE_TIMEOUT
    val imageResource
        get() = STICKERS.find { it.resource == attributes?.stickerSource }?.resource ?: STICKERS.first().resource

    fun setNewViewType(type: MessageViewType) {
        viewType = type
    }
}

@Serializable
@SerialName("Attributes")
data class MessageAttributes(
    @SerialName("message_type") val messageType: String = "",
    @SerialName("sticker_src") val stickerSource: String = ""
)

@Serializable
data class Sender(
    @SerialName("UserId") val id: String = "",
    @SerialName("Attributes") val attributes: Map<String, String> = emptyMap()
) {
    var userName = attributes[KEY_USERNAME]
    val avatarSrc = attributes[KEY_AVATAR]
}

enum class MessageViewType(val index: Int) {
    MESSAGE(0),
    STICKER(1),
    GREEN(2),
    RED(3)
}
