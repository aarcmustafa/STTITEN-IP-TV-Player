package com.sttiten.iptv.data.repository.extension

import com.sttiten.iptv.data.database.model.Channel
import com.sttiten.iptv.extension.api.ExtensionId
import com.sttiten.iptv.extension.api.ExtensionProgramme
import com.sttiten.iptv.extension.api.ChannelMetadataPatch
import com.sttiten.iptv.extension.api.ChannelMetadataSnapshot

interface ExtensionContributionRepository {
    suspend fun search(query: String, limit: Int = 50): List<ExtensionSearchContribution>

    suspend fun enrichChannels(
        channels: List<ChannelMetadataSnapshot>,
        playlistUrl: String? = null,
    ): List<ExtensionMetadataRefreshContribution>

    suspend fun refreshEpg(
        channelReferences: List<String>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
        playlistUrl: String? = null,
    ): List<ExtensionEpgRefreshContribution>
}

data class ExtensionSearchContribution(
    val extensionId: ExtensionId,
    val channel: Channel,
)

data class ExtensionMetadataRefreshContribution(
    val extensionId: ExtensionId,
    val patches: List<ChannelMetadataPatch>,
)

data class ExtensionEpgRefreshContribution(
    val extensionId: ExtensionId,
    val programmes: List<ExtensionProgramme>,
)
