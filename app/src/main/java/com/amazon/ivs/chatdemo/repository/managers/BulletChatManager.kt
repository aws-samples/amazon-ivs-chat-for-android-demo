package com.amazon.ivs.chatdemo.repository.managers

import com.amazon.ivs.chatdemo.common.extensions.asStateFlow
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class BulletChatManager(
    preferenceProvider: PreferenceProvider,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var rows = mutableListOf<Int>()
    private val availableRows = mutableListOf<Int>()
    private val _useBulletChatMode = MutableStateFlow(preferenceProvider.useBulletChat)

    val useBulletChatMode = _useBulletChatMode
        .onEach { preferenceProvider.useBulletChat = it }
        .asStateFlow(coroutineScope, preferenceProvider.useBulletChat)

    fun initRows(rowCount: Int) {
        rows = (0 until rowCount).toMutableList()
        availableRows.addAll(rows)
    }

    fun setUseChatBulletMode(shouldUseChatBulletMode: Boolean) {
        _useBulletChatMode.update { shouldUseChatBulletMode }
    }

    fun setRowForBulletChatMessage(message: ChatMessageResponse): Pair<Int, ChatMessageResponse>? {
        if (!useBulletChatMode.value || availableRows.isEmpty()) return null

        val row = availableRows.random()
        availableRows.remove(row)

        if (availableRows.isEmpty()) {
            availableRows.addAll(rows)
            availableRows.remove(row)
        }

        return row to message
    }
}
