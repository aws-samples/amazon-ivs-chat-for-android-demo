package com.amazon.ivs.chatdemo.ui.chat

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.AVATARS
import com.amazon.ivs.chatdemo.common.extensions.collect
import com.amazon.ivs.chatdemo.common.extensions.setWidthPercent
import com.amazon.ivs.chatdemo.common.viewBinding
import com.amazon.ivs.chatdemo.databinding.DialogIntroductionBinding
import com.amazon.ivs.chatdemo.ui.MainViewModel
import com.amazon.ivs.chatdemo.ui.adapters.AvatarAdapter

class IntroductionDialog : DialogFragment(R.layout.dialog_introduction) {
    private val binding by viewBinding(DialogIntroductionBinding::bind)
    private val viewModel by activityViewModels<MainViewModel>()

    private val avatarAdapter by lazy {
        AvatarAdapter { avatar ->
            viewModel.setAvatarIndex(AVATARS.indexOf(avatar))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // DialogFragment will take up full space if we do not set the width manually
        setWidthPercent(95)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable = false
        setupIntroduction()
    }

    private fun setupIntroduction() = with(binding) {
        introduceName.setText(viewModel.displayName.value)
        introduceAvatars.adapter = avatarAdapter

        introduceName.doOnTextChanged { name, _, _, _ ->
            viewModel.setDisplayName(name.toString())
        }
        chatButton.setOnClickListener {
            viewModel.setIsIntroductionDone(true)
            viewModel.refreshToken()
            dismiss()
        }

        moderationSwitch.setOnCheckedChangeListener { _, grantPermissions ->
            viewModel.updatePermission(grantPermissions)
        }

        collect(viewModel.avatars) { avatars ->
            avatarAdapter.submitList(avatars)
        }

        collect(viewModel.isLoggedIn) { isLoggedIn ->
            chatButton.isEnabled = isLoggedIn
        }
    }
}
