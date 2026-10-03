package com.sttiten.iptv.data.repository.plugin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

internal fun interface ExtensionPluginDiscovery {
    fun discover(): List<InstalledExtensionService>
}

internal class AndroidExtensionPluginDiscovery @Inject constructor(
) : ExtensionPluginDiscovery {
    override fun discover(): List<InstalledExtensionService> = discovery.discover()
}

internal interface ExtensionPluginTransport : ExtensionTransport {
    val isConnectionAvailable: Boolean
    fun close()
}

internal fun interface ExtensionPluginTransportConnector {
    suspend fun connect(service: InstalledExtensionService): ExtensionPluginTransport
}

internal class AndroidExtensionPluginTransportConnector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val hostNetworkBroker: ProviderHostNetworkBroker,
    private val invocationGate: ExtensionInvocationGate,
) : ExtensionPluginTransportConnector {
    override suspend fun connect(service: InstalledExtensionService): ExtensionPluginTransport =
        AndroidExtensionPluginTransport(
            delegate = AndroidBoundExtensionTransport.connect(
                context = context,
                installed = service,
                hostBridgeFactory = { manifest, envelope, brokerProtocolVersion ->
                    ExtensionHostBridge(
                        context = context,
                        broker = hostNetworkBroker,
                        principal = service.toPrincipal(manifest.id),
                        manifest = manifest,
                        envelope = envelope,
                        brokerProtocolVersion = brokerProtocolVersion,
                    )
                },
            ),
            service = service,
            invocationGate = invocationGate,
        )
}

private class AndroidExtensionPluginTransport(
    private val delegate: AndroidBoundExtensionTransport,
    private val service: InstalledExtensionService,
    private val invocationGate: ExtensionInvocationGate,
) : ExtensionPluginTransport {
    override val manifest: ExtensionManifest
        get() = delegate.manifest

    override val isConnectionAvailable: Boolean
        get() = delegate.isConnectionAvailable

    override suspend fun invoke(request: SerializedExtensionEnvelope): SerializedExtensionResult {
        invocationGate.requireAuthorized(service, request.extensionId)
        return delegate.invoke(request)
    }

    override suspend fun cancel(invocationId: InvocationId) {
        delegate.cancel(invocationId)
    }

    override suspend fun health(): ExtensionTransportHealth {
        invocationGate.requireAuthorized(service, manifest.id)
        return delegate.health()
    }

    override fun close() {
        delegate.close()
    }
}

/**
 * Re-checks the user-controlled kill switch at the last in-process boundary before Binder IPC.
 * This closes the window between a Worker restoring plugins and the asynchronous settings
 * observer tearing an existing connection down.
 */
@Singleton
internal class ExtensionInvocationGate @Inject constructor(
    private val settings: Settings,
    private val principalRegistry: ActiveExtensionPrincipalRegistry,
) {
    suspend fun requireAuthorized(
        service: InstalledExtensionService,
        extensionId: ExtensionId,
    ) {
        if (!settings[PreferencesKeys.EXTERNAL_EXTENSIONS]) {
            throw SecurityException("External extensions are disabled")
        }
        if (
            !trustStore.isTrusted(service) ||
            !trustStore.isEnabled(service) ||
            trustStore.extensionId(service) != extensionId.value ||
            principalRegistry.active(extensionId) != service.toPrincipal(extensionId)
        ) {
            throw SecurityException("Extension invocation is no longer authorized")
        }
    }
}
