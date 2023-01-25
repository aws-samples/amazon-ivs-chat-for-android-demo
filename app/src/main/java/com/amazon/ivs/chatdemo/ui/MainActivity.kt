package com.amazon.ivs.chatdemo.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import com.amazon.ivs.chatdemo.common.MEASURE_REPEAT_COUNT
import com.amazon.ivs.chatdemo.common.MEASURE_REPEAT_DELAY
import com.amazon.ivs.chatdemo.common.extensions.hideKeyboard
import com.amazon.ivs.chatdemo.common.extensions.launchUI
import com.amazon.ivs.chatdemo.common.extensions.onReady
import com.amazon.ivs.chatdemo.common.extensions.showSnackBar
import com.amazon.ivs.chatdemo.common.extensions.zoomToFit
import com.amazon.ivs.chatdemo.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycle.addObserver(viewModel)
        setupBackgroundPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun setupBackgroundPlayer() {
        var currentScreenSize = Size(0, 0)
        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val width = binding.mainContent.measuredWidth
            val height = binding.mainContent.measuredHeight
            if (currentScreenSize.width != width || currentScreenSize.height != height) {
                currentScreenSize = Size(width, height)
                remeasureSurface()
            }
        }

        launchUI {
            viewModel.onPlayerError.collect { error ->
                binding.root.showSnackBar(error.errorMessage)
            }
        }

        launchUI {
            viewModel.playerSize.collectLatest { size ->
                if (size == null) return@collectLatest
                fitSurface(size)
            }
        }

        launchUI {
            viewModel.customUrl.collectLatest { url ->
                Timber.d("Playback url changed: $url")
                viewModel.release()
                binding.surfaceView.onReady { surface ->
                    viewModel.initPlayer(this@MainActivity, surface)
                }
            }
        }

        launchUI {
            viewModel.isBuffering.collectLatest { isBuffering ->
                binding.streamBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
            }
        }
    }

    private fun remeasureSurface() = launchUI {
        repeat(MEASURE_REPEAT_COUNT) {
            binding.root.doOnLayout {
                viewModel.playerSize.value?.let { size ->
                    fitSurface(size)
                }
            }
            delay(MEASURE_REPEAT_DELAY)
        }
    }

    private fun fitSurface(size: Size) {
        binding.surfaceView.onReady {
            binding.surfaceView.zoomToFit(size, window.decorView)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.surfaceView.onReady { surface ->
            viewModel.initPlayer(this, surface)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        remeasureSurface()
        hideKeyboard()
    }
}
