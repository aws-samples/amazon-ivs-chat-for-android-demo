package com.amazon.ivs.chatdemo.injection

import com.amazon.ivs.chatdemo.App
import com.amazon.ivs.chatdemo.common.chat.ChatManager
import com.amazon.ivs.chatdemo.common.chat.ChatManagerImpl
import com.amazon.ivs.chatdemo.repository.ChatRepository
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.networking.NetworkClient
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InjectionModule(private val app: App) {

    @Provides
    @Singleton
    fun provideRepository(chatManager: ChatManager, networkClient: NetworkClient) =
        ChatRepository(chatManager, networkClient)

    @Provides
    @Singleton
    fun providePreferenceProvider() = PreferenceProvider(app)

    @Provides
    @Singleton
    fun provideChatManager(): ChatManager = ChatManagerImpl()

    @Provides
    @Singleton
    fun provideNetworkClient() = NetworkClient()
}
