package com.amazon.ivs.chatdemo.repository.models

import com.amazon.ivs.chatdemo.common.MESSAGE_TIMEOUT
import com.amazon.ivs.chatdemo.common.STICKERS
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

const val EVENT_STICKER = "STICKER"
const val EVENT_MESSAGE = "MESSAGE"

@Serializable
data class ChatMessageRequest(
    @SerialName("action") val action: String = Actions.SEND_MESSAGE.value,
    @SerialName("content") val content: String = "",
    @SerialName("attributes") val attributes: MessageAttributes
)

@Serializable
data class ChatMessageResponse(
    val type: String = EVENT_MESSAGE,
    var id: String = "",
    val requestId: String = "",
    var attributes: MessageAttributes? = null,
    var content: String = "",
    var sender: Sender? = null,
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
    @SerialName("sticker_src") var stickerSource: String = ""
)

@Serializable
data class Sender(
    var id: String? = null,
    var username: String,
    val avatar: String
)

enum class MessageViewType(val index: Int) {
    MESSAGE(0),
    STICKER(1),
    GREEN(2),
    RED(3)
}
