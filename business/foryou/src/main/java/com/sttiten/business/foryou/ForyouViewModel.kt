package com.sttiten.iptv.business.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.sttiten.iptv.core.foundation.architecture.preferences.PreferencesKeys
import com.sttiten.iptv.core.foundation.architecture.preferences.Settings
import com.sttiten.iptv.core.foundation.architecture.preferences.flowOf
import com.sttiten.iptv.core.foundation.wrapper.Resource
import com.sttiten.iptv.core.foundation.wrapper.mapResource
import com.sttiten.iptv.core.foundation.wrapper.resource
import com.sttiten.iptv.data.database.model.Channel
import com.sttiten.iptv.data.database.model.Playlist
import com.sttiten.iptv.data.parser.xtream.XtreamEpisodeInfo
import com.sttiten.iptv.data.repository.channel.ChannelRepository
import com.sttiten.iptv.data.repository.playlist.PlaylistRepository
import com.sttiten.iptv.data.repository.programme.ProgrammeRepository
import com.sttiten.iptv.data.service.PlayerManager
import com.sttiten.iptv.data.worker.SubscriptionWorker
import com.sttiten.iptv.data.worker.playlistWorkTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class ForyouViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    channelRepository: ChannelRepository,
    programmeRepository: ProgrammeRepository,
    private val playerManager: PlayerManager,
    settings: Settings,
    workManager: WorkManager,
) : ViewModel() {
    val playlists: StateFlow<Map<Playlist, Int>> = playlistRepository
        .observeAllCounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyMap()
        )

    val subscribingPlaylistUrls: StateFlow<List<String>> =
        workManager.getWorkInfosFlow(
            WorkQuery.fromStates(
                WorkInfo.State.RUNNING,
                WorkInfo.State.ENQUEUED,
            )
        )
            .combine(playlistRepository.observeAll()) { infos, playlists ->
                val urlByWorkTag = playlists.associate { playlist ->
                    playlistWorkTag(playlist.url) to playlist.url
                }
                val playlistUrls = playlists.mapTo(mutableSetOf(), Playlist::url)
                infos
                    .filter { info -> SubscriptionWorker.TAG in info.tags }
                    .mapNotNull { info ->
                        info.tags.firstNotNullOfOrNull(urlByWorkTag::get)
                            // Compatibility with work enqueued before hashed tags shipped.
                            ?: info.tags.firstOrNull { tag -> tag in playlistUrls }
                    }
                    .distinct()
            }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(5_000L)
            )

    val refreshingEpgUrls: Flow<List<String>> = programmeRepository.refreshingEpgUrls

    private val unseensDuration = settings.flowOf(PreferencesKeys.UNSEENS_MILLISECONDS)
        .map { it.toDuration(DurationUnit.MILLISECONDS) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = Duration.INFINITE
        )

    val specs = combine(
        unseensDuration.flatMapLatest { channelRepository.observeAllUnseenFavorites(it) },
        channelRepository.observePlayedRecently(),
    ) { channels, playedRecently ->
        listOfNotNull<Recommend.Spec>(
            playedRecently?.let { Recommend.CwSpec(it, playerManager.getCwPosition(it.url)) },
            *(channels.map { channel -> Recommend.UnseenSpec(channel) }.take(8).toTypedArray())
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_000L),
            initialValue = emptyList()
        )

    fun onUnsubscribePlaylist(url: String) {
        viewModelScope.launch {
            playlistRepository.unsubscribe(url)
        }
    }

    val series = MutableStateFlow<Channel?>(null)
    val seriesReplay = MutableStateFlow(0)
    val episodes: StateFlow<Resource<List<XtreamEpisodeInfo>>> = series
        .combine(seriesReplay) { series, _ -> series }
        .flatMapLatest { series ->
            if (series == null) flow { }
            else resource { playlistRepository.readEpisodesOrThrow(series) }
                .mapResource { it }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = Resource.Loading,
            // don't lose
            started = SharingStarted.Lazily
        )

    val query = MutableStateFlow<String>("")

    suspend fun getPlaylist(playlistUrl: String): Playlist? =
        playlistRepository.get(playlistUrl)
}
