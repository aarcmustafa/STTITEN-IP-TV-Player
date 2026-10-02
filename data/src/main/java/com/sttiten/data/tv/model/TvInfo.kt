package com.sttiten.iptv.data.tv.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Immutable
data class TvInfo(
    val model: String,
    val version: Int,
    val snapshot: Boolean = false,
    val abi: Abi,
    val allowUpdatedPackage: Boolean = false
)
