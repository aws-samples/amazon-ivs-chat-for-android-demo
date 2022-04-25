package com.amazon.ivs.chatdemo.repository

import android.os.Handler
import android.os.Looper
import com.amazon.ivs.chatdemo.common.MESSAGE_EXPIRATION_RETRY_TIME
import com.amazon.ivs.chatdemo.common.MESSAGE_HISTORY
import com.amazon.ivs.chatdemo.common.TOKEN_REFRESH_DELAY
import com.amazon.ivs.chatdemo.common.extensions.ConsumableSharedFlow
import com.amazon.ivs.chatdemo.common.extensions.launchIO
import com.amazon.ivs.chatdemo.common.extensions.onRepeat
import com.amazon.ivs.chatdemo.common.extensions.toJson
import com.amazon.ivs.chatdemo.repository.models.*
import com.amazon.ivs.chatdemo.repository.networking.NetworkClient
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationAttributes
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationBody
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import okhttp3.ResponseBody
import timber.log.Timber
import java.util.*

class ChatRepository(
    private val socketClient: SocketClient,
    private val networkClient: NetworkClient
) {

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
            socketClient.onError.collect { error ->
                if (error.rawCode == ErrorCode.DISCONNECTED_BY_MODERATOR.code) {
                    userName = ""
                    clearMessages()
                    _onLocalKicked.tryEmit(Unit)
                } else {
                    rawMessages.add(ChatMessageResponse().apply {
                        setNewViewType(MessageViewType.RED)
                        sender.userName = error.rawCode.toString()
                        content = error.rawError.toString()
                    })
                    _messages.tryEmit(rawMessages.map { it.copy() })
                }
            }
        }
        launchIO {
            socketClient.onUserKicked.collect { userId ->
                _onRemoteKicked.tryEmit(Unit)
                removeKickedUserMessages(userId)
            }
        }
        launchIO {
            socketClient.onSocketConnected.collect {
                if (userName.isNotBlank()) {
                    rawMessages.add(ChatMessageResponse().apply { setNewViewType(MessageViewType.GREEN) })
                    _messages.tryEmit(rawMessages.map { it.copy() })
                }
            }
        }
        launchIO {
            socketClient.onDeleteMessage.collect { deleteMessageModel ->
                rawMessages.firstOrNull { it.id == deleteMessageModel.attributes.messageId }?.let { message ->
                    rawMessages.remove(message)
                    _messages.tryEmit(rawMessages.map { it.copy() })
                }
            }
        }
        launchIO {
            socketClient.onMessage.collect { message ->
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
        Timber.d("Sending message: ${chatMessage.toJson()}")
        socketClient.sendMessage(chatMessage.apply { id = uuid }.toJson())
    }

    fun deleteMessage(body: DeleteMessageRequest) {
        Timber.d("Deleting message: ${body.toJson()}")
        socketClient.deleteMessage(body.toJson())
    }

    fun kickUser(kickModel: KickUserRequest) = launchIO {
        try {
            socketClient.kickUser(kickModel.toJson())
            if (isModeratorGranted) {
                networkClient.api.deleteAllMessages(DeleteAllMessagesRequest().apply { setUserId(kickModel.userId) })
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

    fun clearMessages() {
        rawMessages.clear()
        _messages.tryEmit(rawMessages.map { it.copy() })
    }

    fun updatePermissions(isGranted: Boolean) {
        Timber.d("Moderator permissions enabled: $isGranted")
        isModeratorGranted = isGranted
    }

    private fun removeKickedUserMessages(userId: String) {
        rawMessages.removeAll { it.sender.id == userId }
        _messages.tryEmit(rawMessages.map { it.copy() })
    }

    private fun getToken(userName: String, avatar: String) = launchIO {
        try {
            Timber.d("Requesting token")
            val attributes = AuthenticationAttributes(username = userName, avatar = avatar)
            val body = AuthenticationBody(userId = uuid, attributes = attributes)
            body.addModeratorPermissions(isModeratorGranted)
            handleTokenResponse(networkClient.api.authenticate(body))
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

    private fun handleTokenResponse(rawResponseBody: ResponseBody) {
        val token = rawResponseBody.string()
        Timber.d("Token received: $token")
        socketClient.connect(token)
    }
}
