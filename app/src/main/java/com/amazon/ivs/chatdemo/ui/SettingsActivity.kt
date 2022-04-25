package com.amazon.ivs.chatdemo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amazon.ivs.chatdemo.App
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.extensions.launchUI
import com.amazon.ivs.chatdemo.common.extensions.lazyViewModel
import com.amazon.ivs.chatdemo.common.extensions.showInputDialog
import com.amazon.ivs.chatdemo.databinding.ActivitySettingsBinding
import com.amazon.ivs.chatdemo.repository.ChatRepository
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var preferenceProvider: PreferenceProvider
    @Inject lateinit var repository: ChatRepository

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel by lazyViewModel({ application as App }, { MainViewModel(preferenceProvider, repository) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.useCustomUrl = viewModel.useCustomUrl
        binding.customUrl = viewModel.customUrl
        binding.isLoggedIn = viewModel.isLoggedIn

        binding.customStreamSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (viewModel.useCustomUrl == isChecked) return@setOnCheckedChangeListener
            viewModel.useCustomUrl = isChecked
            if (isChecked) {
                showPopup(true)
            }
        }

        binding.playbackUrlButton.setOnClickListener {
            showPopup(false)
        }

        binding.playbackUrlValue.text = viewModel.customUrl
        binding.logOutButton.setOnClickListener {
            viewModel.avatarIndex = -1
            viewModel.displayName = ""
            onBackPressed()
        }

        launchUI {
            viewModel.onUseCustomUrl.collect { use ->
                binding.useCustomUrl = use
            }
        }
        launchUI {
            viewModel.onUrlChanged.collect { url ->
                binding.customUrl = url
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun showPopup(shouldRevert: Boolean) {
        showInputDialog(viewModel.customUrl, onSave = { url ->
            viewModel.customUrl = url
        }, onCancel = {
            if (shouldRevert) {
                viewModel.useCustomUrl = false
            }
        })
    }
}
