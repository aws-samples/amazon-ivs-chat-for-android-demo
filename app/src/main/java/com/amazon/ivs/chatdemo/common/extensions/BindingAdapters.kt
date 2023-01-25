package com.amazon.ivs.chatdemo.common.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.amazon.ivs.chatdemo.common.ANIMATION_DURATION_LONG
import com.amazon.ivs.chatdemo.common.ANIMATION_START_OFFSET
import com.amazon.ivs.chatdemo.common.ITEM_SCALE_BIG
import com.amazon.ivs.chatdemo.common.ITEM_SCALE_SMALL
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import timber.log.Timber
import kotlin.math.roundToInt

object BindingAdapters {
    @BindingAdapter("loadImage")
    @JvmStatic
    fun setImage(view: ImageView, resource: String?) {
        resource?.let { res ->
            Glide.with(view.context)
                .load(res)
                .circleCrop()
                .into(view)
        }
    }

    @BindingAdapter("popImage")
    @JvmStatic
    fun popImage(view: ImageView, resource: String?) = with(view) {
        scaleX = ITEM_SCALE_SMALL
        scaleY = ITEM_SCALE_SMALL
        alpha = ALPHA_GONE
        Glide.with(context)
            .load(resource)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?,
                                          isFirstResource: Boolean): Boolean {
                    Timber.d("Failed to load image: $resource")
                    return false
                }

                override fun onResourceReady(drawable: Drawable?, model: Any?, target: Target<Drawable>?,
                                             dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    animate()
                        .scaleX(ITEM_SCALE_BIG)
                        .scaleY(ITEM_SCALE_BIG)
                        .alpha(ALPHA_VISIBLE)
                        .setInterpolator(BounceInterpolator())
                        .setDuration(ANIMATION_DURATION_LONG)
                        .setStartDelay(ANIMATION_START_OFFSET)
                        .start()
                    return false
                }

            })
            .into(this)
    }
}

fun ImageView.loadImage(url: String) {
    BindingAdapters.setImage(this, url)
}

fun ChatMessageResponse.loadSticker(context: Context, dpStickerSize: Int, onLoaded: (Bitmap) -> Unit) {
    Glide.with(context)
        .asBitmap()
        .load(imageResource)
        .apply(RequestOptions().override((dpStickerSize * ITEM_SCALE_BIG).roundToInt()))
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                onLoaded(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                /* Ignored */
            }
        })
}
