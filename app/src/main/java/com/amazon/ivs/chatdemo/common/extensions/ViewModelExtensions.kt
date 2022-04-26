package com.amazon.ivs.chatdemo.common.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amazon.ivs.chatdemo.App

inline fun <reified T : ViewModel> lazyViewModel(
    noinline owner: (() -> App),
    noinline creator: (() -> T)? = null
) = lazy {
    if (creator == null)
        ViewModelProvider(owner())[T::class.java]
    else
        ViewModelProvider(owner(), BaseViewModelFactory(creator))[T::class.java]
}

class BaseViewModelFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}
