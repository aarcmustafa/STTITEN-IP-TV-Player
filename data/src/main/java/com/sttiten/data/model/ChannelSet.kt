package com.sttiten.iptv.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChannelSet(
    val playlistUrl: String,
    val query: String? = null,
    val sort: Sort = Sort.UNSPECIFIED,
    val category: String? = null
)
