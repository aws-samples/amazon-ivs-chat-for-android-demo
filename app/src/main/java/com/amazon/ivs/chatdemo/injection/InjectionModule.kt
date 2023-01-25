package com.amazon.ivs.chatdemo.injection

import android.content.Context
import com.amazon.ivs.chatdemo.repository.managers.ChatManager
import com.amazon.ivs.chatdemo.repository.managers.ChatManagerImpl
import com.amazon.ivs.chatdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.chatdemo.repository.managers.BulletChatManager
import com.amazon.ivs.chatdemo.repository.managers.VideoPlayerManager
import com.amazon.ivs.chatdemo.repository.networking.NetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InjectionModule {
    @Provides
    @Singleton
    fun providePreferenceProvider(@ApplicationContext context: Context) = PreferenceProvider(context)

    @Provides
    @Singleton
    fun provideChatManager(): ChatManager = ChatManagerImpl()

    @Provides
    @Singleton
    fun provideAPI() = NetworkClient.api

    @Provides
    fun provideBulletChat(
        preferenceProvider: PreferenceProvider,
    ) = BulletChatManager(preferenceProvider)

    @Provides
    fun provideVideoPlayer(
        preferenceProvider: PreferenceProvider,
    ) = VideoPlayerManager(preferenceProvider)

    @Provides
    @Singleton
    @IOScope
    fun provideIOScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IOScope
