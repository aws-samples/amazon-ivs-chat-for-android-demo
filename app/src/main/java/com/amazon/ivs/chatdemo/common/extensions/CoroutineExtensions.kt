package com.amazon.ivs.chatdemo.common.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

fun AppCompatActivity.launchUI(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) = lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Coroutine failed: ${e.localizedMessage}")
    }
) {
    repeatOnLifecycle(state = lifecycleState, block = block)
}

fun Fragment.launchUI(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) = viewLifecycleOwner.lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Coroutine failed: ${e.localizedMessage}")
    }
) {
    repeatOnLifecycle(state = lifecycleState, block = block)
}

fun ViewModel.launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Coroutine failed: ${e.localizedMessage}")
    },
    block = block,
)

fun <T> MutableStateFlow<List<T>>.updateList(block: MutableList<T>.() -> Unit) = update {
    it.toMutableList().apply(block = block)
}

fun <T> Flow<T>.asStateFlow(
    coroutineScope: CoroutineScope,
    initialValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) = stateIn(coroutineScope, started, initialValue)

fun <T> Flow<T>.asSharedFlow(
    coroutineScope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) = shareIn(coroutineScope, started)

fun <T> Fragment.collect(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collectLatest: suspend (T) -> Unit
) {
    launchUI(lifecycleState) {
        flow.collectLatest(collectLatest)
    }
}

fun <T> AppCompatActivity.collect(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collectLatest: suspend (T) -> Unit
) {
    launchUI(lifecycleState) {
        flow.collectLatest(collectLatest)
    }
}
