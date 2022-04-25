package com.amazon.ivs.chatdemo.repository

import com.amazon.ivs.chatdemo.BuildConfig
import com.amazon.ivs.chatdemo.common.extensions.ConsumableSharedFlow
import com.amazon.ivs.chatdemo.common.extensions.asObject
import com.amazon.ivs.chatdemo.common.extensions.launchIO
import com.amazon.ivs.chatdemo.repository.models.*
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.internal.ws.RealWebSocket
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val SOCKET_TIMEOUT = 30L

class SocketClient {

    private val _onSocketConnected = ConsumableSharedFlow<Unit>()
    private val _onUserKicked = ConsumableSharedFlow<String>()
    private val _onDeleteMessage = ConsumableSharedFlow<DeleteMessageResponse>()
    private val _onMessage = ConsumableSharedFlow<ChatMessageResponse>()
    private val _onError = ConsumableSharedFlow<NetworkError>()

    private var socket: RealWebSocket? = null
    private var lastError: NetworkError? = null
    private var lastMessageBody: String? = null
    private var isConnecting = false

    private val client = OkHttpClient.Builder()
        .pingInterval(SOCKET_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
        .connectTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    val onSocketConnected = _onSocketConnected.asSharedFlow()
    val onUserKicked = _onUserKicked.asSharedFlow()
    val onDeleteMessage = _onDeleteMessage.asSharedFlow()
    val onMessage = _onMessage.asSharedFlow()
    val onError = _onError.asSharedFlow()

    fun connect(token: String) {
        launchIO {
            try {
                if (isConnecting) return@launchIO
                isConnecting = true
                if (socket != null) {
                    socket?.close(ErrorCode.SOCKET_RESTART.code, "Restarting")
                    socket = null
                    client.connectionPool.evictAll()
                    Timber.d("Releasing socket")
                }
                Timber.d("Creating a new socket connection")
                socket = client.newWebSocket(getSocketRequest(token), object : WebSocketListener() {
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        super.onClosing(webSocket, code, reason)
                        Timber.d("Socket closing: $code, $reason")
                        reason.asObject<Event> {
                            lastError = NetworkError.RAW_ERROR.apply {
                                rawError = reason
                                rawCode = code
                            }
                        }?.let { error ->
                            lastError = NetworkError.RAW_ERROR.apply {
                                rawError = error.errorMessage
                                rawCode = error.errorCode
                            }
                        }
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        super.onClosed(webSocket, code, reason)
                        Timber.d("Socket closed: $code, $reason")
                        reason.asObject<Event> {
                            lastError = NetworkError.RAW_ERROR.apply {
                                rawError = reason
                                rawCode = code
                            }
                        }?.let { error ->
                            lastError = NetworkError.RAW_ERROR.apply {
                                rawError = error.errorMessage
                                rawCode = error.errorCode
                            }
                        }
                        if (code != ErrorCode.SOCKET_RESTART.code) {
                            _onError.tryEmit(lastError!!)
                        }
                        webSocket.close(code, reason)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        super.onFailure(webSocket, t, response)
                        Timber.d(t, "Socket error: $response")
                        if (lastError != null) {
                            _onError.tryEmit(lastError!!)
                            lastError = null
                        } else {
                            _onError.tryEmit(NetworkError.RAW_ERROR.apply {
                                rawError = t.message
                                rawCode = -1
                            })
                        }
                    }

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        super.onOpen(webSocket, response)
                        Timber.d("Socket connected")
                        _onSocketConnected.tryEmit(Unit)
                        isConnecting = false
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        super.onMessage(webSocket, text)
                        if (!text.contains("Forbidden")) {
                            val message = text.asObject<Event>()
                            when (message?.eventType) {
                                EventTypes.MESSAGE -> handleMessage(text)
                                EventTypes.EVENT -> handleEvent(text)
                                EventTypes.ERROR -> {
                                    _onError.tryEmit(NetworkError.RAW_ERROR.apply {
                                        rawError = message.errorMessage
                                        rawCode = message.errorCode
                                    })
                                }
                                else -> {
                                    _onError.tryEmit(NetworkError.MESSAGE_RECEIVE_FAILED)
                                }
                            }
                        }
                    }
                }) as RealWebSocket
                Timber.d("Created new socket")
            } catch (e: Exception) {
                Timber.d(e, "Failed to connect to socket")
                isConnecting = false
                _onError.tryEmit(NetworkError.CONNECTION_FAILED)
            }
        }
    }

    private fun handleEvent(text: String) {
        when (text.asObject<Event>()?.eventName) {
            EventNames.DELETE_MESSAGE -> {
                text.asObject<DeleteMessageResponse>()?.let { response ->
                    _onDeleteMessage.tryEmit(response)
                }
            }
            EventNames.DELETE_BY_USER -> {
                text.asObject<KickUserResponse>()?.let { response ->
                    _onUserKicked.tryEmit(response.userId ?: "")
                }
            }
            else -> { /* Ignored */ }
        }
    }

    private fun handleMessage(text: String) {
        Timber.d("Message received: $text")
        text.asObject<ChatMessageResponse> {
            _onError.tryEmit(NetworkError.MESSAGE_RECEIVE_FAILED)
        }?.let { chatMessage ->
            _onMessage.tryEmit(chatMessage)
            if (chatMessage.content == lastMessageBody) {
                lastMessageBody = null
            }
        }
    }

    fun sendMessage(request: String) {
        try {
            lastMessageBody = request
            socket!!.send(request)
        } catch (e: Exception) {
            Timber.d(e, "Failed to send message: $request")
            _onError.tryEmit(NetworkError.MESSAGE_SEND_FAILED)
        }
    }

    fun deleteMessage(request: String) {
        try {
            socket!!.send(request)
        } catch (e: Exception) {
            Timber.d(e, "Failed to delete message: $request")
            _onError.tryEmit(NetworkError.MESSAGE_DELETE_FAILED)
        }
    }

    fun kickUser(request: String) {
        try {
            socket!!.send(request)
        } catch (e: Exception) {
            Timber.d(e, "Failed to kick user: $request")
            _onError.tryEmit(NetworkError.MESSAGE_DELETE_FAILED)
        }
    }

    private fun getSocketRequest(token: String) = Request.Builder()
        .url(BuildConfig.SOCKET_URL)
        .header("Sec-WebSocket-Protocol", token)
        .header("Sec-WebSocket-Version", "13")
        .build()
}
