package com.amazon.ivs.chatdemo.repository

import com.amazon.ivs.chatdemo.common.REGION_URL
import com.amazon.ivs.chatdemo.common.extensions.ConsumableSharedFlow
import com.amazon.ivs.chatdemo.common.extensions.asObject
import com.amazon.ivs.chatdemo.common.extensions.toJson
import com.amazon.ivs.chatdemo.repository.models.*
import com.amazon.ivs.chatdemo.repository.networking.models.AuthenticationResponse
import com.amazonaws.ivs.chat.messaging.*
import com.amazonaws.ivs.chat.messaging.entities.*
import com.amazonaws.ivs.chat.messaging.requests.DeleteMessageRequest
import com.amazonaws.ivs.chat.messaging.requests.DisconnectUserRequest
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

interface ChatManager {
    val onMessage: SharedFlow<ChatMessageResponse>
    val onError: SharedFlow<NetworkError>
    val onRoomConnected: SharedFlow<Unit>
    val onRemoteKicked: SharedFlow<String>
    val onMessageDeleted: SharedFlow<String>
    val onLocalKicked: SharedFlow<Unit>
    fun initRoom(data: AuthenticationResponse, userId: String)
    fun connect()
    fun sendMessage(chatMessageRequest: ChatMessageRequest)
    fun deleteMessage(id: String)
    fun disconnectUser(userId: String)
    fun onResume()
}

class ChatManagerImpl : ChatManager {

    private var isConnected = false
    private var showDisconnectError = false
    private var lastMessageBody: String? = null
    private var chatRoom: ChatRoom? = null
    private var roomListener: ChatRoomListener? = null

    private val _onMessage = ConsumableSharedFlow<ChatMessageResponse>()
    private val _onRoomConnected = ConsumableSharedFlow<Unit>()
    private val _onRemoteKicked = ConsumableSharedFlow<String>()
    private val _onMessageDeleted = ConsumableSharedFlow<String>()
    private val _onError = ConsumableSharedFlow<NetworkError>()
    private val _onLocalKicked = ConsumableSharedFlow<Unit>()

    override val onMessage = _onMessage.asSharedFlow()
    override val onRoomConnected = _onRoomConnected.asSharedFlow()
    override val onRemoteKicked = _onRemoteKicked.asSharedFlow()
    override val onMessageDeleted = _onMessageDeleted.asSharedFlow()
    override val onError = _onError.asSharedFlow()
    override val onLocalKicked = _onLocalKicked.asSharedFlow()

    override fun initRoom(data: AuthenticationResponse, userId: String) {
        Timber.d("Set token and init room")
        showDisconnectError = false
        chatRoom?.disconnect()
        chatRoom = null
        roomListener = null

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
                _onRoomConnected.tryEmit(Unit)
            }

            override fun onConnecting(room: ChatRoom) {
                Timber.d("On connecting ${room.id}")
            }

            override fun onDisconnected(room: ChatRoom, reason: DisconnectReason) {
                Timber.d("On disconnected ${room.id} ${reason.name}")
                isConnected = false
                if (showDisconnectError) {
                    _onError.tryEmit(NetworkError.RAW_ERROR.apply {
                        rawError = reason.name
                    })
                }
            }

            override fun onEventReceived(room: ChatRoom, event: ChatEvent) {
                Timber.d("On event received ${room.id} ${event.eventName}")
            }

            override fun onMessageDeleted(room: ChatRoom, event: DeleteMessageEvent) {
                Timber.d("On message deleted ${room.id} ${event.attributes}")
                _onMessageDeleted.tryEmit(event.messageId)
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
                    _onLocalKicked.tryEmit(Unit)
                    showDisconnectError = false
                } else {
                    _onRemoteKicked.tryEmit(event.userId)
                }
            }
        }
        chatRoom?.listener = roomListener
    }

    /**
     * Connection must be done in OnResume
     */
    override fun connect() {
        chatRoom?.connect()
    }

    override fun sendMessage(chatMessageRequest: ChatMessageRequest) {
        Timber.d("DEBUGG: Send message $chatMessageRequest")
        val attrs = HashMap<String, String>()
        attrs["message_type"] = chatMessageRequest.attributes.messageType
        attrs["sticker_src"] = chatMessageRequest.attributes.stickerSource

        chatRoom?.sendMessage(SendMessageRequest(chatMessageRequest.content, attrs), object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId} ${response.content}")
            }

            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId} ${error.errorMessage}")
                _onError.tryEmit(NetworkError.MESSAGE_SEND_FAILED.apply {
                    this.rawCode  = error.errorCode
                    this.rawError = error.errorMessage
                })
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
                _onError.tryEmit(NetworkError.MESSAGE_DELETE_FAILED.apply {
                    this.rawCode  = error.errorCode
                    this.rawError = error.errorMessage
                })
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
                _onError.tryEmit(NetworkError.RAW_ERROR.apply {
                    this.rawCode  = error.errorCode
                    this.rawError = error.errorMessage
                })
            }
        })
    }

    override fun onResume() {
        Timber.d("View resumed: connect to room")
        if (!isConnected) {
            chatRoom?.connect()
        }
    }
}
