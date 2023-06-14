package com.amazon.ivs.chatdemo.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.extensions.collect
import com.amazon.ivs.chatdemo.common.extensions.showInputDialog
import com.amazon.ivs.chatdemo.common.viewBinding
import com.amazon.ivs.chatdemo.databinding.FragmentSettingsBinding
import com.amazon.ivs.chatdemo.ui.MainViewModel

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)
    private val viewModel by activityViewModels<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.useCustomUrl = viewModel.isUsingCustomUrl.value
        binding.customUrl = viewModel.customUrl.value
        binding.isLoggedIn = viewModel.isLoggedIn.value
        binding.useBulletChatMode = viewModel.useBulletChatMode.value
        binding.playbackUrlValue.text = viewModel.customUrl.value

        val activity = (requireActivity() as AppCompatActivity)
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.customStreamSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (viewModel.isUsingCustomUrl.value == isChecked) return@setOnCheckedChangeListener
            viewModel.setIsUsingCustomUrl(isChecked)
            if (isChecked) {
                showPopup(true)
            }
        }
        binding.bulletChatSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (viewModel.useBulletChatMode.value == isChecked) return@setOnCheckedChangeListener
            viewModel.setUseChatBulletMode(isChecked)
        }
        binding.logOutButton.setOnClickListener {
            viewModel.logout()
            findNavController().navigateUp()
        }
        binding.playbackUrlButton.setOnClickListener {
            showPopup(false)
        }

        collect(viewModel.isUsingCustomUrl) { isUsingCustomUrl ->
            binding.useCustomUrl = isUsingCustomUrl
        }

        collect(viewModel.customUrl) { url ->
            binding.customUrl = url
        }
    }

    private fun showPopup(shouldRevert: Boolean) {
        (requireActivity() as AppCompatActivity).showInputDialog(viewModel.customUrl.value,
            onSave = { url ->
                viewModel.setCustomUrl(url)
            },
            onCancel = {
                if (shouldRevert) {
                    viewModel.setIsUsingCustomUrl(false)
                }
            })
    }
}
