@file:Suppress("unused")

package com.sttiten.iptv.data.repository

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
