package com.amazon.ivs.chatdemo.ui.chat

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.extensions.dp
import com.amazon.ivs.chatdemo.common.extensions.loadSticker
import com.amazon.ivs.chatdemo.common.extensions.sp
import com.amazon.ivs.chatdemo.repository.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.models.MessageViewType
import timber.log.Timber
import kotlin.random.Random

private const val PX_SPEED = 70f
private const val RANDOM_DURATION_VARIANCE_PERCENT = 0.2f

class BulletChatRowView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(1f, 4f, 4f, Color.BLACK)
    }
    private var messages = mutableMapOf<ChatMessageResponse, Float>()
    private var spMessageTextSize = 16.sp
    private var dpStickerSize = 96.dp
    private var stickerBitmaps = mutableMapOf<String, Bitmap>()
    private val textBounds = Rect()

    init {
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.BulletChatRowView, 0, 0)
            .apply {
                try {
                    spMessageTextSize = getDimension(R.styleable.BulletChatRowView_messageTextSize, spMessageTextSize)
                    dpStickerSize = getDimensionPixelSize(R.styleable.BulletChatRowView_stickerSize, dpStickerSize)
                } finally {
                    recycle()
                }
            }
    }

    fun sendMessage(message: ChatMessageResponse) {
        if (message.viewType != MessageViewType.STICKER || stickerBitmaps.containsKey(message.imageResource)) {
            startMessageAnimation(message)
            return
        }

        fetchStickerImageBeforeAnimatingMessage(message)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.textSize = spMessageTextSize

        for ((message, positionX) in messages) {
            when (message.viewType) {
                MessageViewType.STICKER -> {
                    val stickerBitmap = stickerBitmaps[message.imageResource]!!
                    Timber.d("DRAW: ${stickerBitmap.height}, $height")
                    canvas.drawBitmap(
                        stickerBitmap,
                        positionX,
                        (height.toFloat() - stickerBitmap.height) / 2f,
                        paint
                    )
                }
                MessageViewType.MESSAGE -> {
                    val text = message.content
                    paint.getTextBounds(text, 0, text.length, textBounds)
                    canvas.drawText(
                        message.content,
                        positionX,
                        (height / 2f) + (textBounds.height() / 2f) - textBounds.bottom,
                        paint
                    )
                }
                else -> {
                    /* Ignored */
                }
            }
        }
    }

    private fun fetchStickerImageBeforeAnimatingMessage(message: ChatMessageResponse) {
        try {
            message.loadSticker(context, dpStickerSize) { bitmap ->
                stickerBitmaps[message.imageResource] = bitmap
                startMessageAnimation(message)
            }
        } catch (e: IllegalArgumentException) {
            /* Ignored - this happens when the screen is rotated and an image load is cancelled */
        }
    }

    private fun startMessageAnimation(message: ChatMessageResponse) {
        if (width == 0) return

        messages[message] = 0f
        val messageWidth = when (message.viewType) {
            MessageViewType.STICKER -> stickerBitmaps[message.imageResource]!!.width.toFloat()
            else -> paint.measureText(message.content)
        }

        ValueAnimator.ofFloat(width.toFloat(), -messageWidth).apply {
            val durationBeforeRandomVariance = ((width / PX_SPEED) * 1000).toLong()
            val randomDurationVariance = Random.nextLong(
                from = 0,
                until = (durationBeforeRandomVariance * RANDOM_DURATION_VARIANCE_PERCENT).toLong()
            )
            duration = durationBeforeRandomVariance + randomDurationVariance

            interpolator = LinearInterpolator()
            addUpdateListener {
                val newPositionX = it.animatedValue as Float
                messages[message] = newPositionX
                invalidate()
            }
            doOnEnd { onMessageAnimationEnd(message) }
            start()
        }
    }

    private fun onMessageAnimationEnd(message: ChatMessageResponse) {
        messages.remove(message)
        invalidate()
    }
}
