package com.amazon.ivs.chatdemo.repository

import android.os.Handler
import android.os.Looper
import com.amazon.ivs.chatdemo.common.MESSAGE_EXPIRATION_RETRY_TIME
import com.amazon.ivs.chatdemo.common.MESSAGE_HISTORY
import com.amazon.ivs.chatdemo.common.TOKEN_REFRESH_DELAY
import com.amazon.ivs.chatdemo.common.chat.ChatManager
import com.amazon.ivs.chatdemo.common.extensions.ConsumableSharedFlow
import com.amazon.ivs.chatdemo.common.extensions.launchIO
import com.amazon.ivs.chatdemo.common.extensions.onRepeat
import com.amazon.ivs.chatdemo.repository.models.ChatMessageRequest
import com.amazon.ivs.chatdemo.repository.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.models.MessageViewType
import com.amazon.ivs.chatdemo.repository.models.Sender
import com.amazon.ivs.chatdemo.repository.networking.NetworkClient
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationAttributes
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationBody
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.*

class ChatRepository(
    private val chatManager: ChatManager,
    private val networkClient: NetworkClient
) {

    private var authBody:  AuthenticationResponse? = null
    private val uuid = UUID.randomUUID().toString()
    private val rawMessages = mutableListOf<ChatMessageResponse>()
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { expireMessages() }
    private val _messages = ConsumableSharedFlow<List<ChatMessageResponse>>()
    private val _onLocalKicked = ConsumableSharedFlow<Unit>()
    private val _onRemoteKicked = ConsumableSharedFlow<Unit>()

    private var isModeratorGranted = false

    private var userName: String = ""
    val onLocalKicked = _onLocalKicked.asSharedFlow()
    val onRemoteKicked = _onRemoteKicked.asSharedFlow()
    val messages = _messages.asSharedFlow()

    init {
        launchIO {
            chatManager.onError.collect { error ->
                rawMessages.add(ChatMessageResponse().apply {
                    setNewViewType(MessageViewType.RED)
                    sender = Sender("", if (error.rawCode == -1) "" else error.rawCode.toString(), "")
                    content = error.rawError.toString()
                })
                _messages.tryEmit(rawMessages.map { it.copy() })
            }
        }
        launchIO {
            chatManager.onRemoteKicked.collect { userId ->
                val messagesToRemove = rawMessages.filter { it.sender?.id == userId }
                removeUserLocalMessages(messagesToRemove)
                _onRemoteKicked.tryEmit(Unit)
            }
        }
        launchIO {
            chatManager.onLocalKicked.collect {
                userName = ""
                clearAllLocalMessages()
                _onLocalKicked.tryEmit(Unit)
            }
        }
        launchIO {
            chatManager.onRoomConnected.collect {
                if (userName.isNotBlank()) {
                    rawMessages.add(ChatMessageResponse().apply { setNewViewType(MessageViewType.GREEN) })
                    _messages.tryEmit(rawMessages.map { it.copy() })
                }
            }
        }
        launchIO {
            chatManager.onMessageDeleted.collect { messageId ->
                rawMessages.firstOrNull { it.id == messageId }?.let { message ->
                    rawMessages.remove(message)
                    _messages.tryEmit(rawMessages.map { it.copy() })
                }
            }
        }
        launchIO {
            chatManager.onMessage.collect { message ->
                if (rawMessages.contains(message)) return@collect
                if (rawMessages.size == MESSAGE_HISTORY) {
                    rawMessages.removeFirst()
                }
                rawMessages.add(message)
                _messages.tryEmit(rawMessages.map { it.copy() })
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, MESSAGE_EXPIRATION_RETRY_TIME)
    }

    fun sendMessage(chatMessage: ChatMessageRequest) {
        chatManager.sendMessage(chatMessage)
    }

    fun deleteMessage(id: String) {
        Timber.d("Deleting message: $id")
        chatManager.deleteMessage(id)
    }

    fun kickUser(userId: String) = launchIO {
        try {
            if (isModeratorGranted) {
                chatManager.disconnectUser(userId)
                val messagesToRemove = rawMessages.filter { it.sender?.id == userId }

                launchIO {
                    messagesToRemove.forEach { chatMessage ->
                        delay(300) // Delay because calling too fast causes socket exception
                        chatManager.deleteMessage(chatMessage.id)
                    }
                }
                removeUserLocalMessages(messagesToRemove)
            }
        } catch (e: Exception) {
            Timber.d(e, "Kick error")
        }
    }

    fun refreshToken(userName: String, avatar: String) {
        this.userName = userName
        TOKEN_REFRESH_DELAY.onRepeat {
            Timber.d("Token expired")
            getToken(userName, avatar)
        }
    }

    fun clearAllLocalMessages() {
        rawMessages.clear()
        _messages.tryEmit(rawMessages.map { it.copy() })
    }

    fun updatePermissions(isGranted: Boolean) {
        Timber.d("Moderator permissions enabled: $isGranted")
        isModeratorGranted = isGranted
    }

    fun onResume(userName: String, avatar: String) {
        if (authBody == null) getToken(userName, avatar) else chatManager.onResume()
    }

    private fun getToken(userName: String, avatar: String) = launchIO {
        try {
            Timber.d("Requesting token")
            val attributes = AuthenticationAttributes(username = userName, avatar = avatar)
            val body = AuthenticationBody(userId = uuid, attributes = attributes)
            body.addModeratorPermissions(isModeratorGranted)
            authBody = networkClient.api.authenticate(body)
            chatManager.initRoom(authBody!!, uuid)
            chatManager.connect()
        } catch (e: Exception) {
            Timber.d(e, "Authentication error")
        }
    }

    private fun expireMessages() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        if (rawMessages.removeAll { it.isExpired }) {
            _messages.tryEmit(rawMessages.map { it.copy() })
        }
        timeoutHandler.postDelayed(timeoutRunnable, MESSAGE_EXPIRATION_RETRY_TIME)
    }

    private fun removeUserLocalMessages(messagesToRemove: List<ChatMessageResponse>) {
        rawMessages.removeAll(messagesToRemove)
        _messages.tryEmit(rawMessages.map { it.copy() })
    }
}
