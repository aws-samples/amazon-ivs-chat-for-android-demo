package com.amazon.ivs.chatdemo.common

import android.view.animation.AlphaAnimation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation

private const val ITEM_REMOVE_OFFSET = -200f

class ItemAnimator : DefaultItemAnimator() {

    override fun canReuseUpdatedViewHolder(holder: RecyclerView.ViewHolder) = false

    override fun animateAppearance(
        holder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo?,
        postLayoutInfo: ItemHolderInfo
    ): Boolean {
        holder.itemView.alpha = ALPHA_GONE
        val animationSet = AnimationSet(true)
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 2f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        val alpha = AlphaAnimation(ALPHA_GONE, ALPHA_VISIBLE)
        animationSet.addAnimation(translate)
        animationSet.addAnimation(alpha)
        animationSet.duration = ANIMATION_DURATION_NORMAL
        animationSet.startOffset = ANIMATION_START_OFFSET
        animationSet.withEndAction {
            holder.itemView.alpha = ALPHA_VISIBLE
        }
        holder.itemView.startAnimation(animationSet)
        return true
    }

    override fun getSupportsChangeAnimations() = false

    override fun animateDisappearance(
        holder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo?
    ): Boolean {
        val item = holder.itemView
        val oldY = item.y
        item.animate()
            .alpha(ALPHA_GONE)
            .yBy(ITEM_REMOVE_OFFSET)
            .setDuration(ANIMATION_DURATION_NORMAL)
            .withEndAction {
                item.y = oldY
            }.start()
        return true
    }

    private fun AnimationSet.withEndAction(onAnimationEnd: ()-> Unit) {
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                /* Ignored */
            }

            override fun onAnimationEnd(p0: Animation?) {
                onAnimationEnd()
                setAnimationListener(null)
            }

            override fun onAnimationRepeat(p0: Animation?) {
                /* Ignored */
            }
        })
    }
}
