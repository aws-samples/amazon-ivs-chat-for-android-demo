package com.amazon.ivs.chatdemo.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.ItemAnimator
import com.amazon.ivs.chatdemo.common.KEYBOARD_DELAY
import com.amazon.ivs.chatdemo.common.SCROLL_DELAY
import com.amazon.ivs.chatdemo.common.extensions.animateVisibility
import com.amazon.ivs.chatdemo.common.extensions.countDownTimer
import com.amazon.ivs.chatdemo.common.extensions.hide
import com.amazon.ivs.chatdemo.common.extensions.hideKeyboard
import com.amazon.ivs.chatdemo.common.extensions.launchUI
import com.amazon.ivs.chatdemo.common.extensions.loadImage
import com.amazon.ivs.chatdemo.common.extensions.onStateChanged
import com.amazon.ivs.chatdemo.common.extensions.setVisible
import com.amazon.ivs.chatdemo.common.extensions.show
import com.amazon.ivs.chatdemo.common.viewBinding
import com.amazon.ivs.chatdemo.databinding.FragmentChatBinding
import com.amazon.ivs.chatdemo.databinding.RowBulletChatBinding
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.networking.models.MessageViewType
import com.amazon.ivs.chatdemo.ui.MainViewModel
import com.amazon.ivs.chatdemo.ui.adapters.ChatAdapter
import com.amazon.ivs.chatdemo.ui.adapters.StickerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val binding by viewBinding(FragmentChatBinding::bind)
    private val viewModel by activityViewModels<MainViewModel>()

    private var pickedMessage: ChatMessageResponse? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChat()
        setupModeration()

        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.to_settings)
        }

        binding.chatButton.setOnClickListener {
            binding.chatButton.visibility = View.INVISIBLE
            findNavController().navigate(R.id.open_introduction)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.isShowingStickers.value -> viewModel.setIsShowingStickers(false)
                    binding.moderatorView.visibility == View.VISIBLE -> binding.moderatorView.animateVisibility(false)
                }
            }
        })

        launchUI {
            viewModel.onKicked.collect {
                viewModel.setIsIntroductionDone(false)
                viewModel.refreshToken()
                binding.moderatorView.animateVisibility(false)
                binding.errorPopup.animateVisibility(true, 100)
                countDownTimer(5000) {
                    binding.errorPopup.animateVisibility(false)
                }.start()
            }
        }

        launchUI {
            viewModel.avatars.collect { avatars ->
                avatars.firstOrNull { it.isSelected }?.let { avatar ->
                    binding.inputIcon.loadImage(avatar.url)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.chatButton.visibility = if (viewModel.isLoggedIn.value) View.INVISIBLE else View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupChat() {
        val bulletRows = mutableListOf<BulletChatRowView>()

        fun setupInitialBulletChat() {
            requireActivity().window.decorView.doOnLayout {
                val rowCount = binding.bulletChatList.measuredHeight / resources.getDimension(R.dimen.bullet_chat_max_row_height).toInt()
                repeat(rowCount) {
                    val bulletRowView = RowBulletChatBinding.inflate(
                        layoutInflater,
                        binding.bulletChatList,
                        true
                    )
                    bulletRows.add(bulletRowView.bulletChatView)
                }

                viewModel.initRows(rowCount)
            }
        }

        fun showRegularChat() {
            binding.chatList.setVisible(true)
            // We are making the view invisible not gone so when switching to bullet chat during the app,
            // we get it's measured height correctly
            binding.bulletChatList.setVisible(false, View.INVISIBLE)
        }

        fun showBulletChat() {
            binding.chatList.setVisible(false)
            binding.bulletChatList.setVisible(true)

            if (bulletRows.isNotEmpty()) return
            setupInitialBulletChat()
        }

        fun sendMessage() {
            binding.inputText.text.toString().takeIf { it.isNotBlank() }?.let { message ->
                viewModel.sendMessage(message)
                binding.inputText.text.clear()
            }
        }

        val chatAdapter by lazy {
            ChatAdapter(
                onItemHold = { position ->
                    viewModel.setIsShowingStickers(false)
                    hideKeyboard()
                    val message = viewModel.messages.value.getOrNull(position) ?: return@ChatAdapter
                    if (viewModel.isIntroductionDone.value && viewModel.isModerator) {
                        pickedMessage = message
                        if (message.viewType == MessageViewType.STICKER) {
                            binding.moderateStickerItem.item = message
                        } else {
                            binding.moderateMessageItem.item = message
                        }
                        if (viewModel.isIntroductionDone.value) {
                            binding.moderatorView.animateVisibility(true, 100)
                            binding.moderateMessageItem.root.setVisible(message.viewType != MessageViewType.STICKER)
                            binding.moderateStickerItem.root.setVisible(message.viewType == MessageViewType.STICKER)
                        }
                    }
                },
                onItemTouched = {
                    viewModel.setIsShowingStickers(false)
                }
            )
        }
        val stickerAdapter by lazy {
            StickerAdapter { sticker ->
                viewModel.sendSticker(sticker)
            }
        }
        val stickerLayout: BottomSheetBehavior<View> by lazy {
            BottomSheetBehavior.from(binding.stickerLayout.root)
        }

        binding.chatList.adapter = chatAdapter
        binding.chatList.itemAnimator = ItemAnimator()
        binding.stickerLayout.stickerList.adapter = stickerAdapter

        stickerLayout.onStateChanged(binding.mainContent) {
            viewModel.setIsShowingStickers(false)
        }

        binding.inputText.setOnClickListener {
            launchUI {
                if (viewModel.isShowingStickers.value) {
                    delay(KEYBOARD_DELAY)
                    viewModel.setIsShowingStickers(false)
                }
            }
        }
        binding.inputText.setOnFocusChangeListener { _, isFocused ->
            launchUI {
                if (viewModel.isShowingStickers.value && isFocused) {
                    delay(KEYBOARD_DELAY)
                    viewModel.setIsShowingStickers(false)
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

        binding.chatList.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    hideKeyboard()
                    viewModel.setIsShowingStickers(false)
                    true
                }
                else -> false
            }
        }

        binding.inputFile.setOnClickListener {
            launchUI {
                hideKeyboard()
                delay(KEYBOARD_DELAY)
                viewModel.setIsShowingStickers(!viewModel.isShowingStickers.value)
            }
        }

        launchUI {
            viewModel.onMessage.collect { message ->
                bulletRows[message.first].sendMessage(message.second)
            }
        }

        launchUI {
            viewModel.messages.collect { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    launchUI {
                        delay(SCROLL_DELAY)
                        binding.chatList.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        launchUI {
            viewModel.isShowingStickers.collect { isShowing ->
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
            viewModel.isChatShown.collectLatest { isChatVisible ->
                binding.chatButton.visibility = if (isChatVisible) View.INVISIBLE else View.VISIBLE
                binding.inputHolder.visibility = if (isChatVisible) View.VISIBLE else View.GONE
                binding.inputSend.visibility = if (isChatVisible) View.VISIBLE else View.GONE
            }
        }

        launchUI {
            viewModel.useBulletChatMode.collectLatest { useBulletChatMode ->
                if (useBulletChatMode) {
                    showBulletChat()
                } else {
                    showRegularChat()
                }
            }
        }

        launchUI {
            viewModel.stickers.collect { stickers ->
                stickerAdapter.submitList(stickers)
            }
        }
    }

    private fun setupModeration() {
        binding.moderateMessageItem.messagePill.setBackgroundResource(R.drawable.bg_pill)
        binding.moderateStickerItem.stickerPill.setBackgroundResource(R.drawable.bg_pill_teal)

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

        launchUI {
            viewModel.showSuccessPopup.collect { resource ->
                binding.successPopupText.text = getString(resource)
                binding.successPopup.animateVisibility(true, 100)
                countDownTimer(5000) {
                    binding.successPopup.animateVisibility(false)
                }.start()
            }
        }
    }
}
