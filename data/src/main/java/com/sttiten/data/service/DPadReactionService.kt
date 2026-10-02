package com.sttiten.iptv.data.service

import androidx.compose.runtime.Immutable
import com.sttiten.iptv.data.tv.model.RemoteDirection
import kotlinx.coroutines.flow.SharedFlow

@Immutable
interface DPadReactionService {
    val incoming: SharedFlow<RemoteDirection>
    suspend fun emit(remoteDirection: RemoteDirection)
}
