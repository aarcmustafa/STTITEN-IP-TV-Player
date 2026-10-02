package com.sttiten.iptv.data.service.internal

import androidx.compose.runtime.Immutable
import com.sttiten.iptv.data.service.DPadReactionService
import com.sttiten.iptv.data.tv.model.RemoteDirection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@Immutable
class DPadReactionServiceImpl @Inject constructor() : DPadReactionService {
    private val _incoming = MutableSharedFlow<RemoteDirection>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<RemoteDirection> = _incoming

    override suspend fun emit(remoteDirection: RemoteDirection) {
        _incoming.emit(remoteDirection)
    }
}
