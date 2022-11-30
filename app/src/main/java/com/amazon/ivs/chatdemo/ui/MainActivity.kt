package com.amazon.ivs.chatdemo.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import com.amazon.ivs.chatdemo.App
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.*
import com.amazon.ivs.chatdemo.common.extensions.*
import com.amazon.ivs.chatdemo.databinding.ActivityMainBinding
import com.amazon.ivs.chatdemo.databinding.ViewIntroductionPopupBinding
import com.amazon.ivs.chatdemo.repository.ChatRepository
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.models.MessageViewType
import com.amazon.ivs.chatdemo.ui.adapters.AvatarAdapter
import com.amazon.ivs.chatdemo.ui.adapters.ChatAdapter
import com.amazon.ivs.chatdemo.ui.adapters.StickerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject lateinit var preferenceProvider: PreferenceProvider
    @Inject lateinit var repository: ChatRepository

    private var pickedMessage: ChatMessageResponse? = null
    private var messagesList = emptyList<ChatMessageResponse>()
    private lateinit var binding: ActivityMainBinding
    private lateinit var popupBinding: ViewIntroductionPopupBinding
    private val viewModel by lazyViewModel({ application as App }, { MainViewModel(preferenceProvider, repository) })
    private val chatAdapter by lazy {
        ChatAdapter(
            onItemHold = { position ->
                viewModel.isShowingStickers = false
                hideKeyboard()
                val message = messagesList[position]
                if (viewModel.isIntroductionDone && viewModel.isModerator) {
                    pickedMessage = message
                    if (message.viewType == MessageViewType.STICKER) {
                        binding.moderateStickerItem.item = message
                    } else {
                        binding.moderateMessageItem.item = message
                    }
                    if (viewModel.isIntroductionDone) {
                        binding.moderatorView.animateVisibility(true, 100)
                        binding.moderateMessageItem.root.setVisible(message.viewType != MessageViewType.STICKER)
                        binding.moderateStickerItem.root.setVisible(message.viewType == MessageViewType.STICKER)
                    }
                }
            },
            onItemTouched = {
                viewModel.isShowingStickers = false
            }
        )
    }

    private val avatarAdapter by lazy {
        AvatarAdapter { avatar ->
            viewModel.avatarIndex = AVATARS.indexOf(avatar)
        }
    }
    private val stickerAdapter by lazy {
        StickerAdapter { sticker ->
            viewModel.sendSticker(sticker)
        }
    }
    private val stickerLayout: BottomSheetBehavior<View> by lazy {
        BottomSheetBehavior.from(binding.stickerLayout.root)
    }

    private var introPopup: AlertDialog? = null
    private var currentScreenSize = Size(0, 0)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        lifecycle.addObserver(viewModel)
        binding = ActivityMainBinding.inflate(layoutInflater)
        popupBinding = ViewIntroductionPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.moderateMessageItem.messagePill.setBackgroundResource(R.drawable.bg_pill)
        binding.moderateStickerItem.stickerPill.setBackgroundResource(R.drawable.bg_pill_teal)
        binding.stickerLayout.stickerList.adapter = stickerAdapter

        stickerLayout.onStateChanged(binding.mainContent) {
            viewModel.isShowingStickers = false
        }

        binding.chatList.adapter = chatAdapter
        binding.chatList.itemAnimator = ItemAnimator()

        binding.chatButton.setOnClickListener {
            showIntroductionPopup()
        }
        binding.moderatorView.setOnClickListener {
            binding.moderatorView.setVisible(false)
        }
        binding.cancelRow.setOnClickListener {
            binding.moderatorView.setVisible(false)
        }
        binding.deleteRow.setOnClickListener {
            pickedMessage?.let { message ->
                viewModel.deleteMessage(message)
            }
        }
        binding.kickRow.setOnClickListener {
            pickedMessage?.let { message ->
                viewModel.kickUser(message)
            }
        }
        binding.inputText.setOnClickListener {
            launchUI {
                if (viewModel.isShowingStickers) {
                    delay(KEYBOARD_DELAY)
                    viewModel.isShowingStickers = false
                }
            }
        }
        binding.inputText.setOnFocusChangeListener { _, isFocused ->
            launchUI {
                if (viewModel.isShowingStickers && isFocused) {
                    delay(KEYBOARD_DELAY)
                    viewModel.isShowingStickers = false
                }
            }
        }
        binding.inputText.setOnEditorActionListener { _, event, _ ->
            if (event == EditorInfo.IME_ACTION_GO) {
                sendMessage()
            }
            true
        }

        binding.inputSend.setOnClickListener {
            sendMessage()
        }

        binding.inputFile.setOnClickListener {
            launchUI {
                hideKeyboard()
                delay(KEYBOARD_DELAY)
                viewModel.isShowingStickers = !viewModel.isShowingStickers
            }
        }

        popupBinding.introduceAvatars.adapter = avatarAdapter
        popupBinding.introduceName.doOnTextChanged { text, _, _, _ ->
            viewModel.displayName = text.toString()
        }
        popupBinding.chatButton.setOnClickListener {
            viewModel.isIntroductionDone = true
            viewModel.refreshToken()
            introPopup?.dismiss()
            introPopup = null
        }
        popupBinding.moderationSwitch.setOnCheckedChangeListener { _, grantPermissions ->
            viewModel.updatePermission(grantPermissions)
        }

        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val width = binding.mainContent.measuredWidth
            val height = binding.mainContent.measuredHeight
            if (currentScreenSize.width != width || currentScreenSize.height != height) {
                currentScreenSize = Size(width, height)
                remeasureSurface()
            }
        }

        binding.settings.setOnClickListener {
            binding.settings.isEnabled = false
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.chatList.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    hideKeyboard()
                    viewModel.isShowingStickers = false
                    true
                }
                else -> false
            }
        }

        launchUI {
            viewModel.onPlayerError.collect { error ->
                binding.root.showSnackBar(error.errorMessage)
            }
        }

        launchUI {
            viewModel.onPlayerError.collect { error ->
                binding.root.showSnackBar(error.errorMessage)
            }
        }

        launchUI {
            viewModel.onKicked.collect {
                viewModel.isIntroductionDone = false
                viewModel.refreshToken()
                binding.moderatorView.animateVisibility(false)
                binding.errorPopup.animateVisibility(true, 100)
                countDownTimer(5000) {
                    binding.errorPopup.animateVisibility(false)
                }.start()

            }
        }

        launchUI {
            viewModel.onBuffering.collect { buffering ->
                binding.streamBuffering.visibility = if (buffering) View.VISIBLE else View.GONE
            }
        }

        launchUI {
            viewModel.showSuccessPopup.collect { resource ->
                binding.successPopupText.text = getString(resource)
                binding.successPopup.animateVisibility(true, 100)
                countDownTimer(5000) {
                    binding.successPopup.animateVisibility(false)
                }.start()
            }
        }

        launchUI {
            viewModel.stickers.collect { stickers ->
                stickerAdapter.stickers = stickers
            }
        }

        launchUI {
            viewModel.avatars.collect { avatars ->
                avatarAdapter.avatars = avatars
                avatars.firstOrNull { it.isSelected }?.let { avatar ->
                    binding.inputIcon.loadImage(avatar.url)
                }
            }
        }

        launchUI {
            viewModel.messages.collect { messages ->
                messagesList = messages
                chatAdapter.messages = messages
                if (messages.isNotEmpty()) {
                    launchUI {
                        delay(SCROLL_DELAY)
                        binding.chatList.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        launchUI {
            viewModel.onShowStickers.collect { isShowing ->
                if (isShowing) {
                    binding.inputFile.setImageResource(R.drawable.ic_file_yellow)
                    stickerLayout.show()
                } else {
                    binding.inputFile.setImageResource(R.drawable.ic_file)
                    stickerLayout.hide()
                }
            }
        }

        launchUI {
            viewModel.onReadyToChat.collect { isReady ->
                popupBinding.chatButton.isEnabled = isReady
            }
        }

        launchUI {
            viewModel.showChat.collect { isChatVisible ->
                binding.chatButton.visibility = if (isChatVisible) View.INVISIBLE else View.VISIBLE
                binding.inputHolder.visibility = if (isChatVisible) View.VISIBLE else View.GONE
                binding.inputSend.visibility = if (isChatVisible) View.VISIBLE else View.GONE
            }
        }

        launchUI {
            viewModel.onSizeChanged.collect { size ->
                fitSurface(size)
            }
        }

        launchUI {
            viewModel.onUrlChanged.collect { url ->
                Timber.d("Playback url changed: $url")
                viewModel.release()
                binding.surfaceView.onReady { surface ->
                    viewModel.initPlayer(this@MainActivity, surface)
                }
            }
        }
        viewModel.collectAvatarAndStickers()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.isShowingStickers -> viewModel.isShowingStickers = false
                    binding.moderatorView.visibility == View.VISIBLE -> binding.moderatorView.animateVisibility(false)
                    else -> finish()
                }
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        remeasureSurface()
    }

    override fun onResume() {
        super.onResume()
        if (introPopup == null) {
            binding.chatButton.visibility = if (viewModel.isLoggedIn) View.INVISIBLE else View.VISIBLE
        }
        binding.settings.isEnabled = true
        binding.surfaceView.onReady { surface ->
            viewModel.initPlayer(this, surface)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
    }

    private fun sendMessage() {
        binding.inputText.text.toString().takeIf { it.isNotBlank() }?.let { message ->
            viewModel.sendMessage(message)
            binding.inputText.text.clear()
        }
    }

    private fun remeasureSurface() = launchUI {
        repeat(MEASURE_REPEAT_COUNT) {
            binding.root.doOnLayout {
                viewModel.onSizeChanged.replayCache.lastOrNull()?.let { size ->
                    fitSurface(size)
                }
            }
            delay(MEASURE_REPEAT_DELAY)
        }
    }

    private fun fitSurface(size: Size) {
        binding.surfaceView.onReady {
            binding.surfaceView.zoomToFit(size)
        }
    }

    private fun showIntroductionPopup() {
        binding.chatButton.visibility = View.INVISIBLE
        (popupBinding.root.parent as? ViewGroup)?.removeView(popupBinding.root)
        popupBinding.introduceName.setText(viewModel.displayName)
        introPopup = AlertDialog.Builder(this)
            .setView(popupBinding.root)
            .setCancelable(false)
            .create()
        introPopup?.show()
        introPopup?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}
