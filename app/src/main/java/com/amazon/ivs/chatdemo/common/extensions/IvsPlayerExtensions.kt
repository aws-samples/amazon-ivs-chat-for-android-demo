package com.amazon.ivs.chatdemo.common.extensions

import android.util.Size
import com.amazon.ivs.chatdemo.repository.models.PlayerError
import com.amazonaws.ivs.player.*
import java.nio.ByteBuffer

inline fun Player.setListener(
    crossinline onAnalyticsEvent: (key: String, value: String) -> Unit = { _, _ -> },
    crossinline onRebuffering: () -> Unit = {},
    crossinline onSeekCompleted: (value: Long) -> Unit = { _ -> },
    crossinline onQualityChanged: (quality: Quality) -> Unit = { _ -> },
    crossinline onVideoSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    crossinline onCue: (cue: Cue) -> Unit = { _ -> },
    crossinline onDurationChanged: (duration: Long) -> Unit = { _ -> },
    crossinline onStateChanged: (state: Player.State) -> Unit = { _ -> },
    crossinline onError: (exception: PlayerException) -> Unit = { _ -> },
    crossinline onMetadata: (data: String, buffer: ByteBuffer) -> Unit = { _, _ -> }
): Player.Listener {
    val listener = playerListener(
        onAnalyticsEvent, onRebuffering, onSeekCompleted, onQualityChanged, onVideoSizeChanged,
        onCue, onDurationChanged, onStateChanged, onError, onMetadata
    )

    addListener(listener)
    return listener
}

fun Player.init(
    onVideoSizeChanged: (playerParamsChanged: Size) -> Unit,
    onStateChanged: (state: Player.State) -> Unit,
    onError: (exception: PlayerError) -> Unit
) = setListener(
    onVideoSizeChanged = { width, height ->
        onVideoSizeChanged(Size(width, height))
    },
    onStateChanged = { state ->
        onStateChanged(state)
    },
    onError = { exception ->
        if (exception.code != 0) {
            onError(PlayerError(exception.code.toString(), exception.errorMessage))
        }
    }
)

inline fun playerListener(
    crossinline onAnalyticsEvent: (key: String, value: String) -> Unit = { _, _ -> },
    crossinline onRebuffering: () -> Unit = {},
    crossinline onSeekCompleted: (value: Long) -> Unit = { _ -> },
    crossinline onQualityChanged: (quality: Quality) -> Unit = { _ -> },
    crossinline onVideoSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    crossinline onCue: (cue: Cue) -> Unit = { _ -> },
    crossinline onDurationChanged: (duration: Long) -> Unit = { _ -> },
    crossinline onStateChanged: (state: Player.State) -> Unit = { _ -> },
    crossinline onError: (exception: PlayerException) -> Unit = { _ -> },
    crossinline onMetadata: (data: String, buffer: ByteBuffer) -> Unit = { _, _ -> }
): Player.Listener = object : Player.Listener() {
    override fun onAnalyticsEvent(key: String, value: String) = onAnalyticsEvent(key, value)
    override fun onRebuffering() = onRebuffering()
    override fun onSeekCompleted(value: Long) = onSeekCompleted(value)
    override fun onQualityChanged(quality: Quality) = onQualityChanged(quality)
    override fun onVideoSizeChanged(width: Int, height: Int) = onVideoSizeChanged(width, height)
    override fun onCue(cue: Cue) = onCue(cue)
    override fun onDurationChanged(duration: Long) = onDurationChanged(duration)
    override fun onStateChanged(state: Player.State) = onStateChanged(state)
    override fun onError(exception: PlayerException) = onError(exception)
    override fun onMetadata(data: String, buffer: ByteBuffer) = onMetadata(data, buffer)
}
