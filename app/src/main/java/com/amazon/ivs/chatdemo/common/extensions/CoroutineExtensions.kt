package com.amazon.ivs.chatdemo.common.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber

private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

@Suppress("FunctionName")
fun <T> ConsumableSharedFlow() = MutableSharedFlow<T>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

fun launchIO(block: suspend CoroutineScope.() -> Unit) = ioScope.launch(
    context = CoroutineExceptionHandler { _, e -> Timber.d(e, "Coroutine failed ${e.localizedMessage}") },
    block = block
)

fun AppCompatActivity.launchUI(block: suspend CoroutineScope.() -> Unit) = lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e -> Timber.w(e, "Coroutine failed: ${e.localizedMessage}") },
    block = block
)

fun ViewModel.launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.d(e, "Coroutine failed: ${e.localizedMessage}")
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
