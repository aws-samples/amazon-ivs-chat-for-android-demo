package com.amazon.ivs.chatdemo.repository.models

data class Avatar(
    val id: Int,
    val url: String,
    var isSelected: Boolean = false
)
