package com.amazon.ivs.chatdemo.repository

import android.os.Handler
import android.os.Looper
import com.amazon.ivs.chatdemo.common.MESSAGE_EXPIRATION_RETRY_TIME
import com.amazon.ivs.chatdemo.common.MESSAGE_HISTORY
import com.amazon.ivs.chatdemo.common.TOKEN_REFRESH_DELAY
import com.amazon.ivs.chatdemo.common.extensions.ConsumableSharedFlow
import com.amazon.ivs.chatdemo.common.extensions.launchIO
import com.amazon.ivs.chatdemo.common.extensions.onRepeat
import com.amazon.ivs.chatdemo.common.extensions.updateList
import com.amazon.ivs.chatdemo.repository.models.ChatMessageRequest
import com.amazon.ivs.chatdemo.repository.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.models.MessageViewType
import com.amazon.ivs.chatdemo.repository.models.Sender
import com.amazon.ivs.chatdemo.repository.networking.NetworkClient
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationAttributes
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationBody
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.*

class ChatRepository(
    private val chatManager: ChatManager,
    private val networkClient: NetworkClient
) {
    private var authBody: AuthenticationResponse? = null
    private val uuid = UUID.randomUUID().toString()
    private val rawMessages = mutableListOf<ChatMessageResponse>()
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { expireMessages() }
    private val _messages = MutableStateFlow(emptyList<ChatMessageResponse>())
    private val _onLocalKicked = ConsumableSharedFlow<Unit>()
    private val _onRemoteKicked = ConsumableSharedFlow<Unit>()

    private var isModeratorGranted = false

    private var userName: String = ""
    val onLocalKicked = _onLocalKicked.asSharedFlow()
    val onRemoteKicked = _onRemoteKicked.asSharedFlow()
    val messages = _messages.asStateFlow()
    val onMessage = chatManager.onMessage

    init {
        launchIO {
            chatManager.onError.collect { error ->
                _messages.updateList {
                    add(ChatMessageResponse().apply {
                        setNewViewType(MessageViewType.RED)
                        sender = Sender(
                            "",
                            if (error.rawCode == -1) "" else error.rawCode.toString(),
                            ""
                        )
                        content = error.rawError.toString()
                    })
                }
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
                    _messages.updateList {
                        add(ChatMessageResponse().apply { setNewViewType(MessageViewType.GREEN) })
                    }
                }
            }
        }
        launchIO {
            chatManager.onMessageDeleted.collect { messageId ->
                _messages.updateList { firstOrNull { it.id == messageId }?.let { remove(it) } }
            }
        }
        launchIO {
            chatManager.onMessage.collect { message ->
                if (_messages.value.contains(message)) return@collect
                if (_messages.value.size == MESSAGE_HISTORY) {
                    _messages.update { it.drop(1) }
                }
                _messages.update {
                    Timber.d("Updating messages with $message")
                    it + message
                }
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
        _messages.update { emptyList() }
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
            Timber.d("Reached here")
            chatManager.initRoom(authBody!!, uuid)
            chatManager.connect()
        } catch (e: Exception) {
            Timber.d(e, "Authentication error")
        }
    }

    private fun expireMessages() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        _messages.updateList { removeAll { it.isExpired } }
        timeoutHandler.postDelayed(timeoutRunnable, MESSAGE_EXPIRATION_RETRY_TIME)
    }

    private fun removeUserLocalMessages(messagesToRemove: List<ChatMessageResponse>) {
        _messages.updateList { removeAll(messagesToRemove) }
    }
}
