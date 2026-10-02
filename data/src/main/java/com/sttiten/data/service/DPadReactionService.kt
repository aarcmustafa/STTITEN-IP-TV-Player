package com.sttiten.iptv.data.service

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.SharedFlow

@Immutable
interface DPadReactionService {
    val incoming: SharedFlow<RemoteDirection>
    suspend fun emit(remoteDirection: RemoteDirection)
}
