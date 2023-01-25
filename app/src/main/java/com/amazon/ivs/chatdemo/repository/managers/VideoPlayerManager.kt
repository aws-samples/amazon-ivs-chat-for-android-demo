package com.amazon.ivs.chatdemo.repository.managers

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Surface
import com.amazon.ivs.chatdemo.common.MAX_QUALITY
import com.amazon.ivs.chatdemo.common.extensions.init
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.models.PlayerError
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class VideoPlayerManager(
    private var preferenceProvider: PreferenceProvider,
) {
    private var listener: Player.Listener? = null
    private var player: MediaPlayer? = null

    private val _isBuffering = MutableStateFlow(true)
    private val _playerSize = MutableStateFlow<Size?>(null)
    private val _onPlayerError = Channel<PlayerError>()

    val isBuffering = _isBuffering.asStateFlow()
    val onPlayerError = _onPlayerError.receiveAsFlow()
    val playerSize = _playerSize.asStateFlow()

    fun initPlayer(context: Context, surface: Surface) {
        if (player != null) return

        _isBuffering.update { true }
        player = MediaPlayer(context)
        listener = player!!.init(
            onVideoSizeChanged = { videoSizeState ->
                _playerSize.update { videoSizeState }
            },
            onStateChanged = { state ->
                when (state) {
                    Player.State.BUFFERING -> _isBuffering.tryEmit(true)
                    Player.State.READY -> {
                        player?.qualities?.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                            player?.setAutoMaxQuality(quality)
                        }
                    }
                    Player.State.PLAYING -> _isBuffering.tryEmit(false)
                    else -> { /* Ignored */ }
                }
            },
            onError = { exception ->
                _onPlayerError.trySend(exception)
            }
        )

        player?.setSurface(surface)
        player?.load(Uri.parse(preferenceProvider.playbackUrl))
        player?.play()
        Timber.d("Player initialized: ${preferenceProvider.playbackUrl}")
    }

    fun release() {
        listener?.run {
            Timber.d("Releasing player")
            player?.removeListener(this)
        }
        player?.release()
        player = null
        listener = null
    }
}
