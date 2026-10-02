package com.sttiten.iptv.extension.runtime

import com.sttiten.iptv.extension.api.Capability
import com.sttiten.iptv.extension.api.ExtensionManifest
import com.sttiten.iptv.extension.api.ExtensionPayload
import com.sttiten.iptv.extension.api.ExtensionSettingsSnapshot
import com.sttiten.iptv.extension.api.Hook
import com.sttiten.iptv.extension.api.security.BrokerScopeHandle

data class ExtensionBrokerScopeRequest(
    val manifest: ExtensionManifest,
    val hook: Hook,
    val payload: ExtensionPayload,
    val settings: ExtensionSettingsSnapshot,
    val grantedCapabilities: Set<Capability>,
)

interface ExtensionBrokerScopeLease : AutoCloseable {
    val handle: BrokerScopeHandle

    override fun close()
}

fun interface ExtensionBrokerScopeProvider {
    /**
     * Opens a scope for one external invocation, or returns null when the Hook is not brokered.
     *
     * Implementations must bind the returned scope to the active extension principal and Hook.
     * [ExtensionRuntime] closes every returned lease after the invocation, including cancellation.
     */
    suspend fun open(request: ExtensionBrokerScopeRequest): ExtensionBrokerScopeLease?
}

object EmptyExtensionBrokerScopeProvider : ExtensionBrokerScopeProvider {
    override suspend fun open(request: ExtensionBrokerScopeRequest): ExtensionBrokerScopeLease? =
        null
}
