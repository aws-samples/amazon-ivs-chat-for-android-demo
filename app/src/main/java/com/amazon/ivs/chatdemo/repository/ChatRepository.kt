package com.amazon.ivs.chatdemo.repository

import com.amazon.ivs.chatdemo.common.MESSAGE_EXPIRATION_RETRY_TIME
import com.amazon.ivs.chatdemo.common.MESSAGE_HISTORY
import com.amazon.ivs.chatdemo.common.TOKEN_REFRESH_DELAY
import com.amazon.ivs.chatdemo.common.extensions.onRepeat
import com.amazon.ivs.chatdemo.common.extensions.updateList
import com.amazon.ivs.chatdemo.injection.IOScope
import com.amazon.ivs.chatdemo.repository.managers.ChatManager
import com.amazon.ivs.chatdemo.repository.networking.Endpoints
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationAttributes
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationBody
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationResponse
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageRequest
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.networking.models.MessageViewType
import com.amazon.ivs.chatdemo.repository.networking.models.Sender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatManager: ChatManager,
    private val endpoints: Endpoints,
    @IOScope private val ioScope: CoroutineScope,
) {
    private var authBody: AuthenticationResponse? = null
    private val uuid = UUID.randomUUID().toString()
    private val _messages = MutableStateFlow(emptyList<ChatMessageResponse>())
    private val _onLocalKicked = Channel<Unit>()
    private val _onRemoteKicked = Channel<Unit>()

    private var isModeratorGranted = false
    private var userName: String = ""

    val onLocalKicked = _onLocalKicked.receiveAsFlow()
    val onRemoteKicked = _onRemoteKicked.receiveAsFlow()
    val messages = _messages.asStateFlow()
    val onMessage = chatManager.onMessage

    init {
        ioScope.launch {
            chatManager.onError.collect { error ->
                _messages.updateList {
                    add(ChatMessageResponse().apply {
                        setNewViewType(MessageViewType.RED)
                        sender = Sender(
                            "",
                            error.errorCode.toString(),
                            ""
                        )
                        content = error.errorMessage
                    })
                }
            }
        }
        ioScope.launch {
            chatManager.onRemoteKicked.collect { userId ->
                val messagesToRemove = _messages.value.filter { it.sender?.id == userId }
                removeUserLocalMessages(messagesToRemove)
                _onRemoteKicked.trySend(Unit)
            }
        }
        ioScope.launch {
            chatManager.onLocalKicked.collect {
                userName = ""
                clearAllLocalMessages()
                _onLocalKicked.trySend(Unit)
            }
        }
        ioScope.launch {
            chatManager.onRoomConnected.collect {
                if (userName.isNotBlank()) {
                    _messages.updateList {
                        add(ChatMessageResponse().apply { setNewViewType(MessageViewType.GREEN) })
                    }
                }
            }
        }
        ioScope.launch {
            chatManager.onMessageDeleted.collect { messageId ->
                _messages.updateList { firstOrNull { it.id == messageId }?.let { remove(it) } }
            }
        }
        ioScope.launch {
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
        ioScope.launch {
            while (isActive) {
                delay(MESSAGE_EXPIRATION_RETRY_TIME)
                expireMessages()
            }
        }
    }

    fun sendMessage(chatMessage: ChatMessageRequest) {
        chatManager.sendMessage(chatMessage)
    }

    fun deleteMessage(id: String) {
        Timber.d("Deleting message: $id")
        chatManager.deleteMessage(id)
    }

    fun kickUser(userId: String) = ioScope.launch {
        try {
            if (isModeratorGranted) {
                chatManager.disconnectUser(userId)
                val messagesToRemove = _messages.value.filter { it.sender?.id == userId }

                ioScope.launch {
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

    fun reconnectToRoom(userName: String, avatar: String) {
        if (authBody == null) getToken(userName, avatar) else chatManager.connect()
    }

    private fun getToken(userName: String, avatar: String) = ioScope.launch {
        try {
            Timber.d("Requesting token")
            val attributes = AuthenticationAttributes(username = userName, avatar = avatar)
            val body = AuthenticationBody(userId = uuid, attributes = attributes)
            body.addModeratorPermissions(isModeratorGranted)
            authBody = endpoints.authenticate(body)
            Timber.d("Reached here")
            chatManager.initRoom(authBody!!, uuid)
            chatManager.connect()
        } catch (e: Exception) {
            Timber.d(e, "Authentication error")
        }
    }

    private fun expireMessages() {
        _messages.updateList { removeAll { it.isExpired } }
    }

    private fun removeUserLocalMessages(messagesToRemove: List<ChatMessageResponse>) {
        _messages.updateList { removeAll(messagesToRemove.toSet()) }
    }
}
