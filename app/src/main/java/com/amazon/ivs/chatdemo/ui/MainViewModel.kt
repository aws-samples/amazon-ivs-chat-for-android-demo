package com.amazon.ivs.chatdemo.ui

import android.content.Context
import android.view.Surface
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.AVATARS
import com.amazon.ivs.chatdemo.common.STICKERS
import com.amazon.ivs.chatdemo.common.extensions.asSharedFlow
import com.amazon.ivs.chatdemo.common.extensions.asStateFlow
import com.amazon.ivs.chatdemo.common.extensions.launch
import com.amazon.ivs.chatdemo.repository.ChatRepository
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.managers.BulletChatManager
import com.amazon.ivs.chatdemo.repository.managers.VideoPlayerManager
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageRequest
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.networking.models.EVENT_MESSAGE
import com.amazon.ivs.chatdemo.repository.networking.models.EVENT_STICKER
import com.amazon.ivs.chatdemo.repository.networking.models.MessageAttributes
import com.amazon.ivs.chatdemo.repository.models.Sticker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferenceProvider: PreferenceProvider,
    private val repository: ChatRepository,
    private val videoPlayerManager: VideoPlayerManager,
    private val bulletChatManager: BulletChatManager,
) : ViewModel(), DefaultLifecycleObserver {
    private var didCurrentUserDeleteMessage = false
    private var didCurrentUserKickRemote = false

    private val _isUsingCustomUrl = MutableStateFlow(preferenceProvider.isUsingCustomUrl)
    private val _stickers = MutableStateFlow(STICKERS)
    private val _avatars = MutableStateFlow(AVATARS)
    private val _isShowingStickers = MutableStateFlow(false)
    private val _isIntroductionDone = MutableStateFlow(false)
    private val _displayName = MutableStateFlow("")
    private val _chosenAvatarIndex = MutableStateFlow<Int?>(null)
    private val _customUrl = MutableStateFlow(preferenceProvider.customUrl)
    private val _onKicked = Channel<Unit>()
    private val _showSuccessPopup = Channel<Int>()

    var isModerator = false
        private set

    val isUsingCustomUrl = _isUsingCustomUrl
        .onEach { useCustomUrl -> preferenceProvider.isUsingCustomUrl = useCustomUrl }
        .asStateFlow(viewModelScope, preferenceProvider.isUsingCustomUrl)

    val customUrl = _isUsingCustomUrl
        .combine(_customUrl) { isUsingCustomUrl, customUrl ->
            if (isUsingCustomUrl) customUrl else null
        }
        .onEach { customUrl -> preferenceProvider.customUrl = customUrl }
        .asStateFlow(viewModelScope, preferenceProvider.customPlaybackUrl)

    val messages = repository.messages
        .onEach {
            if (didCurrentUserDeleteMessage) {
                _showSuccessPopup.send(R.string.message_deleted)
                didCurrentUserDeleteMessage = false
            }
        }
        .asStateFlow(viewModelScope, emptyList())

    val avatars = _avatars
        .combine(_chosenAvatarIndex) { avatars, chosenAvatarIndex ->
            avatars.mapIndexed { index, avatar ->
                avatar.copy(isSelected = index == chosenAvatarIndex)
            }
        }
        .asStateFlow(viewModelScope, _avatars.value)

    val isLoggedIn = combine(_displayName, _chosenAvatarIndex) { displayName, chosenAvatarIndex ->
        displayName.isNotBlank() && chosenAvatarIndex != null
    }.asStateFlow(viewModelScope, false)

    val isChatShown = combine(_isIntroductionDone, isLoggedIn) { isDone, isLoggedIn ->
        isDone && isLoggedIn
    }.asStateFlow(viewModelScope, false)

    val onMessage = repository.onMessage
        .map { message -> bulletChatManager.setRowForBulletChatMessage(message) }
        .filterNotNull()
        .asSharedFlow(viewModelScope)

    val isBuffering = videoPlayerManager.isBuffering
    val onPlayerError = videoPlayerManager.onPlayerError
    val playerSize = videoPlayerManager.playerSize
    val displayName = _displayName.asStateFlow()
    val stickers = _stickers.asStateFlow()
    val isShowingStickers = _isShowingStickers.asStateFlow()
    val onKicked = _onKicked.receiveAsFlow()
    val showSuccessPopup = _showSuccessPopup.receiveAsFlow()
    val isIntroductionDone = _isIntroductionDone.asStateFlow()
    val useBulletChatMode = bulletChatManager.useBulletChatMode

    init {
        launch {
            repository.onLocalKicked.collect {
                _displayName.update { "" }
                _chosenAvatarIndex.update { null }
                didCurrentUserKickRemote = false
                _onKicked.send(Unit)
            }
        }
        launch {
            repository.onRemoteKicked.collect {
                if (didCurrentUserKickRemote) {
                    _showSuccessPopup.send(R.string.user_kicked)
                    didCurrentUserKickRemote = false
                }
            }
        }
    }

    fun initPlayer(context: Context, surface: Surface) {
        videoPlayerManager.initPlayer(context, surface)
    }

    fun initRows(rowCount: Int) {
        bulletChatManager.initRows(rowCount)
    }

    fun sendMessage(message: String) {
        val attributes = MessageAttributes(messageType = EVENT_MESSAGE)
        val chatMessage = ChatMessageRequest(content = message, attributes = attributes)
        repository.sendMessage(chatMessage)
    }

    fun deleteMessage(message: ChatMessageResponse) {
        didCurrentUserDeleteMessage = isModerator
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
        didCurrentUserKickRemote = isModerator
        message.sender?.id?.let { senderId ->
            repository.kickUser(senderId)
        }
    }

    fun refreshToken() {
        val avatarIndex = _chosenAvatarIndex.value
        val avatar = if (avatarIndex == null) AVATARS.first().url else AVATARS[avatarIndex].url
        repository.refreshToken(_displayName.value, avatar)
    }

    fun logout() {
        _displayName.update { "" }
        _chosenAvatarIndex.update { null }
    }

    fun updatePermission(grantPermissions: Boolean) {
        isModerator = grantPermissions
        repository.updatePermissions(grantPermissions)
    }

    fun setIsIntroductionDone(isIntroductionDone: Boolean) {
        _isIntroductionDone.update { isIntroductionDone }
    }

    fun setIsUsingCustomUrl(isUsingCustomUrl: Boolean) {
        _isUsingCustomUrl.update { isUsingCustomUrl }
    }

    fun setDisplayName(name: String) {
        _displayName.update { name }
    }

    fun setAvatarIndex(avatarIndex: Int) {
        _chosenAvatarIndex.update { avatarIndex }
    }

    fun setCustomUrl(customUrl: String?) {
        _customUrl.update { customUrl }
    }

    fun setIsShowingStickers(isShowingStickers: Boolean) {
        _isShowingStickers.update { isShowingStickers }
    }

    fun setUseChatBulletMode(useChatBulletMode: Boolean) {
        bulletChatManager.setUseChatBulletMode(useChatBulletMode)
    }

    fun release() {
        videoPlayerManager.release()
        repository.clearAllLocalMessages()
    }

    override fun onResume(owner: LifecycleOwner) {
        // Currently just trying to connect to room after onPause does not work.
        // Info taken from ChatSDK documentation.
        val avatarIndex = _chosenAvatarIndex.value
        val avatar = if (avatarIndex == null) AVATARS.first().url else AVATARS[avatarIndex].url
        repository.reconnectToRoom(_displayName.value, avatar)
    }
}
