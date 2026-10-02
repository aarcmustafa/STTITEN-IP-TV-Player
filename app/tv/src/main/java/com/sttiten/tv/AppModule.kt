@file:Suppress("unused")

package com.sttiten.iptv

import com.sttiten.iptv.core.foundation.architecture.Publisher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Binds
    @Singleton
    fun bindPublisher(provider: AppPublisher): Publisher
}
