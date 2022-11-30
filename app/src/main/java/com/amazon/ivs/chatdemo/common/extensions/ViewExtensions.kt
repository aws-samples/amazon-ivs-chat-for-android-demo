package com.amazon.ivs.chatdemo.common.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.amazon.ivs.chatdemo.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar

const val ALPHA_VISIBLE = 1F
const val ALPHA_GONE = 0F

fun AppCompatActivity.hideKeyboard() {
    val view = currentFocus ?: window.decorView
    val token = view.windowToken
    view.clearFocus()
    ContextCompat.getSystemService(this, InputMethodManager::class.java)?.hideSoftInputFromWindow(token, 0)
}

fun BottomSheetBehavior<View>.isShowing() = state == BottomSheetBehavior.STATE_EXPANDED

fun BottomSheetBehavior<View>.show() {
    if (!isShowing()) {
        state = BottomSheetBehavior.STATE_EXPANDED
    }
}

fun BottomSheetBehavior<View>.hide() {
    if (state != BottomSheetBehavior.STATE_HIDDEN) {
        state = BottomSheetBehavior.STATE_HIDDEN
    }
}

fun View.setVisible(isVisible: Boolean = true, hideOption: Int = View.GONE) {
    visibility = if (isVisible) View.VISIBLE else hideOption
}

fun BottomSheetBehavior<View>.onStateChanged(content: View, onHidden: () -> Unit) {
    addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                content.setPadding(0, 0, 0, 0)
                onHidden()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val bottomPadding = content.measuredHeight - bottomSheet.top
            content.setPadding(0, 0, 0, bottomPadding)
            if (slideOffset < -0.15f) {
                onHidden()
            }
        }
    })
}

fun View.showSnackBar(message: String) {
    val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    snackBar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.black_70))
    snackBar.view.elevation = 1000f
    snackBar.setTextColor(ContextCompat.getColor(context, R.color.white))
    snackBar.show()
}

fun TextureView.onReady(onReady: (surface: Surface) -> Unit) {
    if (surfaceTexture != null) {
        onReady(Surface(surfaceTexture))
        return
    }
    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            surfaceTextureListener = null
            onReady(Surface(surfaceTexture))
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            /* Ignored */
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            /* Ignored */
        }
    }
}

fun View.zoomToFit(videoSize: Size) {
    (parent as View).doOnLayout { useToScale ->
        val cardWidth = useToScale.measuredWidth
        val cardHeight = useToScale.measuredHeight
        val size = calculateSurfaceSize(cardWidth, cardHeight, videoSize)
        layoutParams = FrameLayout.LayoutParams(size.width, size.height)
    }
}

fun View.animateVisibility(isVisible: Boolean, duration: Long = 250L) {
    if ((visibility == View.VISIBLE && isVisible) || (visibility == View.GONE && !isVisible)) return
    setVisible(true)
    alpha = if (isVisible) ALPHA_GONE else ALPHA_VISIBLE
    animate().setDuration(duration).alpha(if (isVisible) ALPHA_VISIBLE else ALPHA_GONE)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                setVisible(isVisible)
            }
        }).start()
}

private fun calculateSurfaceSize(surfaceWidth: Int, surfaceHeight: Int, videoSize: Size): Size {
    val ratioHeight = videoSize.height.toFloat() / videoSize.width.toFloat()
    val ratioWidth = videoSize.width.toFloat() / videoSize.height.toFloat()
    val isPortrait = videoSize.width < videoSize.height
    val calculatedHeight = if (isPortrait) (surfaceWidth / ratioWidth).toInt() else (surfaceWidth * ratioHeight).toInt()
    val calculatedWidth = if (isPortrait) (surfaceHeight / ratioHeight).toInt() else (surfaceHeight * ratioWidth).toInt()
    return if (calculatedWidth >= surfaceWidth) {
        Size(calculatedWidth, surfaceHeight)
    } else {
        Size(surfaceWidth, calculatedHeight)
    }
}
