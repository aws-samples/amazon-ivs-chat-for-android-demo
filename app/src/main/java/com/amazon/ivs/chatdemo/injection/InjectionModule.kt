package com.amazon.ivs.chatdemo.injection

import com.amazon.ivs.chatdemo.App
import com.amazon.ivs.chatdemo.repository.ChatRepository
import com.amazon.ivs.chatdemo.repository.SocketClient
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.networking.NetworkClient
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InjectionModule(private val app: App) {

    @Provides
    @Singleton
    fun provideRepository(socketClient: SocketClient, networkClient: NetworkClient) =
        ChatRepository(socketClient, networkClient)

    @Provides
    @Singleton
    fun providePreferenceProvider() = PreferenceProvider(app)

    @Provides
    @Singleton
    fun provideSocketClient() = SocketClient()

    @Provides
    @Singleton
    fun provideNetworkClient() = NetworkClient()

}
