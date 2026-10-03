@file:Suppress("unused")

package com.sttiten.iptv.data.repository

import com.sttiten.iptv.data.repository.playlist.PlaylistRepository
import com.sttiten.iptv.data.repository.playlist.PlaylistRepositoryImpl
import com.sttiten.iptv.data.repository.channel.ChannelRepository
import com.sttiten.iptv.data.repository.channel.ChannelRepositoryImpl
import com.sttiten.iptv.data.repository.programme.ProgrammeRepository
import com.sttiten.iptv.data.repository.programme.ProgrammeRepositoryImpl
import com.sttiten.iptv.data.repository.media.MediaRepository
import com.sttiten.iptv.data.repository.media.MediaRepositoryImpl
import com.sttiten.iptv.data.repository.tv.TvRepository
import com.sttiten.iptv.data.repository.tv.TvRepositoryImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {
    @Binds
    @Singleton
    fun bindPlaylistRepository(
        repository: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    fun bindChannelRepository(
        repository: ChannelRepositoryImpl
    ): ChannelRepository

    @Binds
    @Singleton
    fun bindProgrammeRepository(
        repositoryImpl: ProgrammeRepositoryImpl
    ): ProgrammeRepository

    @Binds
    @Singleton
    fun bindMediaRepository(
        repository: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    fun bindTvRepository(
        repository: TvRepositoryImpl
    ): TvRepository
}
