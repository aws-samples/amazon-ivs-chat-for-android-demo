package com.amazon.ivs.chatdemo.repository.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    @SerialName("EventName") private val name: String = "",
    @SerialName("Type") private val type: String = "",
    @SerialName("ErrorCode") val errorCode: Int = -1,
    @SerialName("ErrorMessage") val errorMessage: String = ""
) {
    val eventName get() = EventNames.values().firstOrNull { it.value == name }
    val eventType get() = EventTypes.values().firstOrNull { it.value == type }
}

enum class EventTypes(val value: String) {
    MESSAGE("MESSAGE"),
    EVENT("EVENT"),
    ERROR("ERROR")
}

enum class EventNames(val value: String) {
    DELETE_MESSAGE("aws:DELETE_MESSAGE"),
    DELETE_BY_USER("app:DELETE_BY_USER")
}
