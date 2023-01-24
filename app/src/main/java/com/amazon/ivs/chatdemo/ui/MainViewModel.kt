package com.amazon.ivs.chatdemo.ui

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Surface
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.AVATARS
import com.amazon.ivs.chatdemo.common.MAX_QUALITY
import com.amazon.ivs.chatdemo.common.STICKERS
import com.amazon.ivs.chatdemo.common.extensions.*
import com.amazon.ivs.chatdemo.repository.ChatRepository
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.models.*
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.properties.Delegates

class MainViewModel(
    private val preferenceProvider: PreferenceProvider,
    private val repository: ChatRepository
) : ViewModel(), DefaultLifecycleObserver {
    private var _isModerator = false
    private var listener: Player.Listener? = null
    private var player: MediaPlayer? = null
    private var didCurrentUserDeleteMessage = false
    private var didCurrentUserKickRemote = false
    private var rows = mutableListOf<Int>()
    private val rawAvatars = AVATARS
    private val rawStickers = STICKERS
    private val availableRows = mutableListOf<Int>()

    private val _stickers = ConsumableSharedFlow<List<Sticker>>()
    private val _avatars = ConsumableSharedFlow<List<Avatar>>()
    private val _onShowStickers = ConsumableSharedFlow<Boolean>()
    private val _onReadyToChat = ConsumableSharedFlow<Boolean>()
    private val _onBuffering = ConsumableSharedFlow<Boolean>()
    private val _onPlayerError = ConsumableSharedFlow<PlayerError>()
    private val _onSizeChanged = ConsumableSharedFlow<Size>()
    private val _onUseCustomUrl = ConsumableSharedFlow<Boolean>()
    private val _onUrlChanged = ConsumableSharedFlow<String?>()
    private val _onKicked = ConsumableSharedFlow<Unit>()
    private val _showSuccessPopup = ConsumableSharedFlow<Int>()
    private val _showChat = MutableStateFlow(false)
    private val _useBulletChatMode = MutableStateFlow(preferenceProvider.useBulletChat)

    val messages = repository.messages
        .onEach {
            if (didCurrentUserDeleteMessage) {
                _showSuccessPopup.tryEmit(R.string.message_deleted)
                didCurrentUserDeleteMessage = false
            }
        }
        .asStateFlow(viewModelScope, emptyList())
    val onMessage = repository.onMessage
        .map { message -> setRowForBulletChatMessage(message) }
        .filterNotNull()
        .asSharedFlow(viewModelScope)
    val useBulletChatMode = _useBulletChatMode
        .onEach { preferenceProvider.useBulletChat = it }
        .asStateFlow(viewModelScope, preferenceProvider.useBulletChat)
    val avatars = _avatars.asSharedFlow()
    val stickers = _stickers.asSharedFlow()
    val onReadyToChat = _onReadyToChat.asSharedFlow()
    val onShowStickers = _onShowStickers.asSharedFlow()
    val onBuffering = _onBuffering.asSharedFlow()
    val onPlayerError = _onPlayerError.asSharedFlow()
    val onSizeChanged = _onSizeChanged.asSharedFlow()
    val onUseCustomUrl = _onUseCustomUrl.asSharedFlow()
    val onUrlChanged = _onUrlChanged.asSharedFlow()
    val onKicked = _onKicked.asSharedFlow()
    val showSuccessPopup = _showSuccessPopup.asSharedFlow()
    val showChat = _showChat.asStateFlow()
    val isLoggedIn get() = displayName.isNotBlank() && avatarIndex != -1
    val isModerator get() = _isModerator

    var isIntroductionDone: Boolean by Delegates.observable(false) { _, _, isDone ->
        _showChat.tryEmit(isDone && isLoggedIn)
    }
    var isShowingStickers: Boolean by Delegates.observable(false) { _, _, isShowing ->
        _onShowStickers.tryEmit(isShowing)
    }
    var displayName: String by Delegates.observable("") { _, _, _ ->
        _onReadyToChat.tryEmit(isLoggedIn)
    }
    var avatarIndex: Int by Delegates.observable(-1) { _, _, index ->
        _onReadyToChat.tryEmit(isLoggedIn)
        rawAvatars.forEach { it.isSelected = false }
        if (index != -1) {
            rawAvatars[avatarIndex].isSelected = true
        }
        _avatars.tryEmit(rawAvatars.map { it.copy() })
    }
    var useCustomUrl: Boolean by Delegates.observable(preferenceProvider.useCustomUrl) { _, _, use ->
        preferenceProvider.useCustomUrl = use
        _onUseCustomUrl.tryEmit(use)
        _onUrlChanged.tryEmit(preferenceProvider.customPlaybackUrl)
    }
    var customUrl: String? by Delegates.observable(preferenceProvider.customUrl) { _, _, url ->
        preferenceProvider.customUrl = url
        _onUrlChanged.tryEmit(preferenceProvider.customPlaybackUrl)
        if (url == null) {
            _onUseCustomUrl.tryEmit(false)
        }
    }

    init {
        launch {
            repository.onLocalKicked.collect {
                displayName = ""
                avatarIndex = -1
                didCurrentUserKickRemote = false
                _onKicked.tryEmit(Unit)
            }
        }
        launch {
            repository.onRemoteKicked.collect {
                if (didCurrentUserKickRemote) {
                    _showSuccessPopup.tryEmit(R.string.user_kicked)
                    didCurrentUserKickRemote = false
                }
            }
        }
    }

    fun collectAvatarAndStickers() {
        _avatars.tryEmit(rawAvatars.map { it.copy() })
        _stickers.tryEmit(rawStickers.map { it.copy() })
    }

    fun initRows(rowCount: Int) {
        rows = (0 until rowCount).toMutableList()
        availableRows.addAll(rows)
    }

    fun initPlayer(context: Context, surface: Surface) {
        if (player != null) return

        _onBuffering.tryEmit(true)
        player = MediaPlayer(context)
        listener = player!!.init(
            onVideoSizeChanged = { videoSizeState ->
                _onSizeChanged.tryEmit(videoSizeState)
            },
            onStateChanged = { state ->
                when (state) {
                    Player.State.BUFFERING -> _onBuffering.tryEmit(true)
                    Player.State.READY -> {
                        player?.qualities?.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                            player?.setAutoMaxQuality(quality)
                        }
                    }
                    Player.State.PLAYING -> _onBuffering.tryEmit(false)
                    else -> { /* Ignored */ }
                }
            },
            onError = { exception ->
                _onPlayerError.tryEmit(exception)
            }
        )

        player?.setSurface(surface)
        player?.load(Uri.parse(preferenceProvider.playbackUrl))
        player?.play()
        Timber.d("Player initialized: ${preferenceProvider.playbackUrl}")
    }

    fun refreshToken() =
        repository.refreshToken(
            displayName,
            if (avatarIndex == -1) AVATARS.first().url else AVATARS[avatarIndex].url
        )

    fun release() {
        listener?.run {
            Timber.d("Releasing player")
            player?.removeListener(this)
        }
        player?.release()
        player = null
        listener = null
        repository.clearAllLocalMessages()
    }

    fun sendMessage(message: String) {
        val attributes = MessageAttributes(messageType = EVENT_MESSAGE)
        val chatMessage = ChatMessageRequest(content = message, attributes = attributes)
        repository.sendMessage(chatMessage)
    }

    fun deleteMessage(message: ChatMessageResponse) {
        didCurrentUserDeleteMessage = _isModerator
        repository.deleteMessage(message.id)
    }

    fun sendSticker(sticker: Sticker) {
        val attributes = MessageAttributes(messageType = EVENT_STICKER, stickerSource = sticker.resource)
        val chatMessage = ChatMessageRequest(
            attributes = attributes,
            content = sticker.id.toString(),
        )
        repository.sendMessage(chatMessage)
    }

    fun kickUser(message: ChatMessageResponse) {
        didCurrentUserKickRemote = _isModerator
        message.sender?.id?.let { senderId ->
            repository.kickUser(senderId)
        }
    }

    fun updatePermission(grantPermissions: Boolean) {
        _isModerator = grantPermissions
        repository.updatePermissions(grantPermissions)
    }

    fun setUseChatBulletMode(shouldUseChatBulletMode: Boolean) {
        _useBulletChatMode.update { shouldUseChatBulletMode }
    }

    private fun setRowForBulletChatMessage(message: ChatMessageResponse): Pair<Int, ChatMessageResponse>? {
        if (!useBulletChatMode.value || availableRows.isEmpty()) return null

        val row = availableRows.random()
        availableRows.remove(row)

        if (availableRows.isEmpty()) {
            availableRows.addAll(rows)
            availableRows.remove(row)
        }

        return row to message
    }

    override fun onResume(owner: LifecycleOwner) {
        // Currently just trying to connect to room after onPause does not work.
        // Info taken from ChatSDK documentation.
        repository.onResume(
            displayName,
            if (avatarIndex == -1) AVATARS.first().url else AVATARS[avatarIndex].url
        )
    }
}
