package com.sttiten.iptv.extension.runtime

import com.sttiten.iptv.extension.api.ExtensionManifest
import com.sttiten.iptv.extension.api.InvocationId
import com.sttiten.iptv.extension.api.SerializedExtensionEnvelope
import com.sttiten.iptv.extension.api.SerializedExtensionResult

/**
 * Signals that the host refused to begin extension execution because its deadline was exhausted.
 *
 * This is a host admission outcome, not an extension failure.
 */
class HostInvocationDeadlineExceededException :
    RuntimeException("The host invocation deadline expired before extension execution")

interface ExtensionTransport {
    val manifest: ExtensionManifest

    suspend fun invoke(request: SerializedExtensionEnvelope): SerializedExtensionResult

    suspend fun cancel(invocationId: InvocationId)

    suspend fun health(): ExtensionTransportHealth
}

enum class ExtensionTransportHealth {
    HEALTHY,
    DEGRADED,
    UNAVAILABLE,
}
