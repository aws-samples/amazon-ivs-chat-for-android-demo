package com.amazon.ivs.chatdemo.repository.managers

import com.amazon.ivs.chatdemo.common.REGION_URL
import com.amazon.ivs.chatdemo.common.extensions.asObject
import com.amazon.ivs.chatdemo.common.extensions.toJson
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageRequest
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.networking.models.MessageAttributes
import com.amazon.ivs.chatdemo.repository.networking.models.NetworkError
import com.amazon.ivs.chatdemo.repository.networking.models.Sender
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationResponse
import com.amazonaws.ivs.chat.messaging.ChatRoom
import com.amazonaws.ivs.chat.messaging.ChatRoomListener
import com.amazonaws.ivs.chat.messaging.ChatToken
import com.amazonaws.ivs.chat.messaging.DeleteMessageCallback
import com.amazonaws.ivs.chat.messaging.DisconnectReason
import com.amazonaws.ivs.chat.messaging.DisconnectUserCallback
import com.amazonaws.ivs.chat.messaging.SendMessageCallback
import com.amazonaws.ivs.chat.messaging.entities.ChatError
import com.amazonaws.ivs.chat.messaging.entities.ChatEvent
import com.amazonaws.ivs.chat.messaging.entities.ChatMessage
import com.amazonaws.ivs.chat.messaging.entities.DeleteMessageEvent
import com.amazonaws.ivs.chat.messaging.entities.DisconnectUserEvent
import com.amazonaws.ivs.chat.messaging.requests.DeleteMessageRequest
import com.amazonaws.ivs.chat.messaging.requests.DisconnectUserRequest
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

interface ChatManager {
    val onMessage: SharedFlow<ChatMessageResponse>
    val onError: Flow<NetworkError>
    val onRoomConnected: Flow<Unit>
    val onRemoteKicked: Flow<String>
    val onMessageDeleted: Flow<String>
    val onLocalKicked: Flow<Unit>
    fun initRoom(data: AuthenticationResponse, userId: String)
    fun connect()
    fun sendMessage(chatMessageRequest: ChatMessageRequest)
    fun deleteMessage(id: String)
    fun disconnectUser(userId: String)
}

class ChatManagerImpl : ChatManager {
    private var isConnected = false
    private var showDisconnectError = false
    private var lastMessageBody: String? = null
    private var chatRoom: ChatRoom? = null
    private var roomListener: ChatRoomListener? = null

    private val _onMessage = MutableSharedFlow<ChatMessageResponse>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _onRoomConnected = Channel<Unit>()
    private val _onRemoteKicked = Channel<String>()
    private val _onMessageDeleted = Channel<String>()
    private val _onError = Channel<NetworkError>()
    private val _onLocalKicked = Channel<Unit>()

    override val onMessage = _onMessage.asSharedFlow()
    override val onRoomConnected = _onRoomConnected.receiveAsFlow()
    override val onRemoteKicked = _onRemoteKicked.receiveAsFlow()
    override val onMessageDeleted = _onMessageDeleted.receiveAsFlow()
    override val onError = _onError.receiveAsFlow()
    override val onLocalKicked = _onLocalKicked.receiveAsFlow()

    override fun initRoom(data: AuthenticationResponse, userId: String) {
        Timber.d("Set token and init room")
        disconnect()

        chatRoom = ChatRoom(
            regionOrUrl = REGION_URL,
            tokenProvider = { chatTokenCallback ->
                chatTokenCallback.onSuccess(ChatToken(data.token, data.sessionExpirationDate, data.tokenExpirationDate))
            }
        )

        roomListener = object : ChatRoomListener {
            override fun onConnected(room: ChatRoom) {
                Timber.d("On connected ${room.id} ")
                showDisconnectError = true
                isConnected = true
                _onRoomConnected.trySend(Unit)
            }

            override fun onConnecting(room: ChatRoom) {
                Timber.d("On connecting ${room.id}")
            }

            override fun onDisconnected(room: ChatRoom, reason: DisconnectReason) {
                Timber.d("On disconnected ${room.id} ${reason.name}")
                isConnected = false
                if (showDisconnectError) {
                    _onError.trySend(NetworkError.Error(reason.name))
                }
            }

            override fun onEventReceived(room: ChatRoom, event: ChatEvent) {
                Timber.d("On event received ${room.id} ${event.eventName}")
            }

            override fun onMessageDeleted(room: ChatRoom, event: DeleteMessageEvent) {
                Timber.d("On message deleted ${room.id} ${event.attributes}")
                _onMessageDeleted.trySend(event.messageId)
            }

            override fun onMessageReceived(room: ChatRoom, message: ChatMessage) {
                val messageObject = ChatMessageResponse(
                    attributes = message.attributes.toJson().asObject<MessageAttributes>(),
                    content = message.content,
                    sender = message.sender.attributes.toJson().asObject<Sender>(),
                    id = message.id
                )
                messageObject.sender?.id = message.sender.userId
                Timber.d("Message received $messageObject")
                _onMessage.tryEmit(messageObject)
                if (messageObject.content == lastMessageBody) {
                    lastMessageBody = null
                }
            }

            override fun onUserDisconnected(room: ChatRoom, event: DisconnectUserEvent) {
                Timber.d("On user disconnected $userId ${event.userId}")
                if (userId == event.userId) {
                    _onLocalKicked.trySend(Unit)
                    showDisconnectError = false
                } else {
                    _onRemoteKicked.trySend(event.userId)
                }
            }
        }
        chatRoom?.listener = roomListener
    }

    /**
     * Connection must be done in OnResume
     */
    override fun connect() {
        if (!isConnected) {
            chatRoom?.connect()
        }
    }

    override fun sendMessage(chatMessageRequest: ChatMessageRequest) {
        Timber.d("Send message $chatMessageRequest")
        val attributes = mapOf(
            "message_type" to chatMessageRequest.attributes.messageType,
            "sticker_src" to chatMessageRequest.attributes.stickerSource
        )

        chatRoom?.sendMessage(SendMessageRequest(chatMessageRequest.content, attributes), object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId} ${response.content}")
            }

            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(NetworkError.MessageSendFailed(error.errorMessage, error.errorCode))
            }
        })
    }

    override fun deleteMessage(id: String) {
        Timber.d("Deleting message: $id")
        chatRoom?.deleteMessage(DeleteMessageRequest(id), object : DeleteMessageCallback {
            override fun onConfirmed(request: DeleteMessageRequest, response: DeleteMessageEvent) {
                Timber.d("Message deleted: ${request.requestId} ${response.attributes}")
            }

            override fun onRejected(request: DeleteMessageRequest, error: ChatError) {
                Timber.d("Delete message rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(NetworkError.MessageDeleteFailed(error.errorMessage, error.errorCode))
            }
        })
    }

    override fun disconnectUser(userId: String) {
        chatRoom?.disconnectUser(DisconnectUserRequest(userId), object : DisconnectUserCallback {
            override fun onConfirmed(request: DisconnectUserRequest, response: DisconnectUserEvent) {
                Timber.d("User disconnected: ${request.requestId} ${response.attributes}")
            }

            override fun onRejected(request: DisconnectUserRequest, error: ChatError) {
                Timber.d("Disconnect user rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(NetworkError.Error(error.errorMessage, error.errorCode))
            }
        })
    }

    private fun disconnect() {
        showDisconnectError = false
        chatRoom?.disconnect()
        chatRoom = null
        roomListener = null
    }
}
