package com.sttiten.iptv.data.repository.provider

import android.content.Context
import android.content.res.Configuration
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sttiten.iptv.data.database.M3UDatabase
import com.sttiten.iptv.data.database.model.DataSource
import com.sttiten.iptv.data.database.model.Playlist
import com.sttiten.iptv.data.database.model.ProviderAccount
import com.sttiten.iptv.data.extension.SubscriptionProviderImporter
import com.sttiten.iptv.data.extension.security.ActiveExtensionPrincipalRegistry
import com.sttiten.iptv.data.extension.security.AndroidKeystoreCredentialVault
import com.sttiten.iptv.data.extension.security.ProviderBrokerScopeStore
import com.sttiten.iptv.data.repository.extension.ExtensionContributionScheduler
import com.sttiten.iptv.data.repository.extension.ExtensionContributionRunCoordinator
import com.sttiten.iptv.extension.api.ExtensionApiRange
import com.sttiten.iptv.extension.api.ExtensionApiVersions
import com.sttiten.iptv.extension.api.ExtensionCallContext
import com.sttiten.iptv.extension.api.ExtensionEntrypoint
import com.sttiten.iptv.extension.api.ExtensionError
import com.sttiten.iptv.extension.api.ExtensionErrorCodes
import com.sttiten.iptv.extension.api.ExtensionHandler
import com.sttiten.iptv.extension.api.ExtensionHookDeclaration
import com.sttiten.iptv.extension.api.ExtensionId
import com.sttiten.iptv.extension.api.ExtensionManifest
import com.sttiten.iptv.extension.api.ExtensionSemanticVersion
import com.sttiten.iptv.extension.api.ExtensionSettingField
import com.sttiten.iptv.extension.api.ExtensionSettingSchema
import com.sttiten.iptv.extension.api.ExtensionSettingType
import com.sttiten.iptv.extension.api.HookResult
import com.sttiten.iptv.extension.api.subscription.ProviderKind
import com.sttiten.iptv.extension.api.subscription.SubscriptionHookSpecs
import com.sttiten.iptv.extension.api.subscription.SubscriptionProviderDescriptor
import com.sttiten.iptv.extension.api.subscription.SubscriptionProviderDiscoverRequest
import com.sttiten.iptv.extension.api.subscription.SubscriptionProviderDiscoverResult
import com.sttiten.iptv.extension.api.subscription.SubscriptionProviderSettingKeys
import com.sttiten.iptv.extension.api.subscription.SubscriptionProviderVariant
import com.sttiten.iptv.extension.runtime.ExtensionRegistrationResult
import com.sttiten.iptv.extension.runtime.ExtensionRuntime
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionProviderDiscoveryTest {
    @Test
    fun partialFailureKeepsValidProviders() = withRepository { repository, runtime, _ ->
        register(runtime, providerEntrypoint(VALID_PROVIDER_ID, validDescriptor(VALID_PROVIDER_ID)))
        register(
            runtime,
            providerEntrypoint(
                id = INVALID_PROVIDER_ID,
                descriptor = validDescriptor(ExtensionId("com.example.wrong-provider")),
            ),
        )

        val providers = repository.discoverProviders()

        assertEquals(1, providers.size)
        assertEquals(VALID_PROVIDER_ID, providers.single().descriptor.providerId)
        assertEquals(
            SubscriptionProviderExecutionKind.BUILT_IN,
            providers.single().executionKind,
        )
    }

    @Test
    fun everyFailedOrInvalidProviderProducesStableAggregateFailure() =
        withRepository { repository, runtime, _ ->
            register(
                runtime,
                providerEntrypoint(
                    id = INVALID_PROVIDER_ID,
                    descriptor = validDescriptor(ExtensionId("com.example.wrong-provider")),
                ),
            )
            register(runtime, failingProviderEntrypoint(FAILED_PROVIDER_ID))

            val failure = runCatching { repository.discoverProviders() }.exceptionOrNull()

            assertTrue(failure is ProviderDiscoveryException)
            failure as ProviderDiscoveryException
            assertEquals(2, failure.failureCount)
            assertEquals("provider.discovery_failed", failure.code)
            assertFalse(failure.message.orEmpty().contains("private provider detail"))
        }

    @Test
    fun noRegisteredProviderIsAnEmptySuccessfulDiscovery() =
        withRepository { repository, _, _ ->
            assertTrue(repository.discoverProviders().isEmpty())
        }

    @Test
    fun bidiControlOnlyProviderLabelIsRejectedBeforeItReachesTheUi() =
        withRepository { repository, runtime, _ ->
            register(
                runtime,
                providerEntrypoint(
                    id = VALID_PROVIDER_ID,
                    descriptor = validDescriptor(VALID_PROVIDER_ID).copy(
                        variants = listOf(
                            SubscriptionProviderVariant(
                                kind = PROVIDER_KIND,
                                displayName = "\u061C",
                            )
                        )
                    ),
                ),
            )

            val failure = runCatching { repository.discoverProviders() }.exceptionOrNull()

            assertTrue(failure is ProviderDiscoveryException)
            assertEquals(1, (failure as ProviderDiscoveryException).failureCount)
        }

    @Test
    fun discoverForwardsCurrentLocaleTag() =
        withRepository(localeTag = "zh-CN") { repository, runtime, _ ->
            var receivedLocaleTag: String? = null
            register(
                runtime,
                entrypoint(VALID_PROVIDER_ID) { _, request ->
                    receivedLocaleTag = request.localeTag
                    HookResult.Success(
                        SubscriptionProviderDiscoverResult(
                            validDescriptor(VALID_PROVIDER_ID),
                        )
                    )
                },
            )

            repository.discoverProviders()

            assertEquals("zh-CN", receivedLocaleTag)
        }

    @Test
    fun discoverUsesExplicitLocaleAfterAnInPlaceConfigurationChange() =
        withRepository(localeTag = "en-US") { repository, runtime, _ ->
            var receivedLocaleTag: String? = null
            register(
                runtime,
                entrypoint(VALID_PROVIDER_ID) { _, request ->
                    receivedLocaleTag = request.localeTag
                    HookResult.Success(
                        SubscriptionProviderDiscoverResult(
                            validDescriptor(VALID_PROVIDER_ID),
                        )
                    )
                },
            )

            repository.discoverProviders(localeTag = "zh-CN")

            assertEquals("zh-CN", receivedLocaleTag)
        }

    @Test
    fun accountSummaryContainsReauthenticationFieldsButNoCredential() =
        withRepository { repository, _, database ->
            val playlistUrl = "m3u-provider://account/summary/live"
            database.playlistDao().insertOrReplace(
                Playlist(
                    title = "Living room",
                    url = playlistUrl,
                    source = DataSource.Provider,
                )
            )
            database.providerDao().insertOrReplace(
                ProviderAccount(
                    id = "summary",
                    providerId = VALID_PROVIDER_ID.value,
                    providerKind = PROVIDER_KIND.value,
                    baseUrl = "https://media.example.test",
                    serverId = "server-id",
                    serverName = "Home server",
                    serverVersion = "1",
                    userId = "user-id",
                    username = "viewer",
                    playlistUrl = playlistUrl,
                    requiresReauthentication = true,
                )
            )

            val summary = repository.observeAccountSummaries().first().single()

            assertEquals("Living room", summary.playlistTitle)
            assertEquals(playlistUrl, summary.playlistUrl)
            assertEquals(VALID_PROVIDER_ID, summary.providerId)
            assertEquals(PROVIDER_KIND, summary.providerKind)
            assertEquals("https://media.example.test", summary.baseUrl)
            assertEquals("viewer", summary.username)
            assertEquals("Home server", summary.serverName)
            assertTrue(summary.requiresReauthentication)
        }

    private fun withRepository(
        localeTag: String? = null,
        block: suspend (
            SubscriptionProviderRepositoryImpl,
            ExtensionRuntime,
            M3UDatabase,
        ) -> Unit,
    ) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repositoryContext = localeTag?.let { requestedLocaleTag ->
            val configuration = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(requestedLocaleTag))
            }
            context.createConfigurationContext(configuration)
        } ?: context
        val database = Room.inMemoryDatabaseBuilder(context, M3UDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val runtime = ExtensionRuntime(ExtensionApiVersions.Current)
            val credentialVault = AndroidKeystoreCredentialVault(
                context = context,
                keyAlias = "m3u.discovery.test.${System.nanoTime()}",
            )
            val principalRegistry = ActiveExtensionPrincipalRegistry()
            val repository = SubscriptionProviderRepositoryImpl(
                context = repositoryContext,
                runtime = runtime,
                providerDao = database.providerDao(),
                playlistDao = database.playlistDao(),
                importer = SubscriptionProviderImporter(
                    database = database,
                    playlistDao = database.playlistDao(),
                    channelDao = database.channelDao(),
                    providerDao = database.providerDao(),
                    programmeDao = database.programmeDao(),
                    credentialVault = credentialVault,
                ),
                credentialVault = credentialVault,
                extensionContributionScheduler = NoOpExtensionContributionScheduler,
                extensionContributionRunCoordinator = ExtensionContributionRunCoordinator(),
                activePrincipalRegistry = principalRegistry,
                providerBrokerScopeStore = ProviderBrokerScopeStore(
                    credentialVault = credentialVault,
                    principalRegistry = principalRegistry,
                ),
                lifecycleCoordinator = ProviderLifecycleCoordinator(),
            )
            block(repository, runtime, database)
        } finally {
            database.close()
        }
    }

    private fun register(runtime: ExtensionRuntime, entrypoint: ExtensionEntrypoint) {
        assertTrue(runtime.register(entrypoint) is ExtensionRegistrationResult.Registered)
    }

    private fun providerEntrypoint(
        id: ExtensionId,
        descriptor: SubscriptionProviderDescriptor,
    ): ExtensionEntrypoint = entrypoint(id) { _, _ ->
        HookResult.Success(SubscriptionProviderDiscoverResult(descriptor))
    }

    private fun failingProviderEntrypoint(id: ExtensionId): ExtensionEntrypoint = entrypoint(id) { _, _ ->
        HookResult.Failure(
            ExtensionError(
                code = ExtensionErrorCodes.InvocationFailed,
                message = "private provider detail",
                recoverable = true,
            )
        )
    }

    private fun entrypoint(
        id: ExtensionId,
        handlerBlock: suspend (
            ExtensionCallContext,
            SubscriptionProviderDiscoverRequest,
        ) -> HookResult<SubscriptionProviderDiscoverResult>,
    ): ExtensionEntrypoint = object : ExtensionEntrypoint {
        override val manifest = ExtensionManifest(
            id = id,
            displayName = id.value,
            extensionVersion = ExtensionSemanticVersion(1, 0, 0),
            apiRange = ExtensionApiRange(ExtensionApiVersions.Current, ExtensionApiVersions.Current),
            hooks = setOf(
                ExtensionHookDeclaration(
                    hook = SubscriptionHookSpecs.Discover.hook,
                    schemaVersion = SubscriptionHookSpecs.Discover.schemaVersion,
                )
            ),
            capabilities = emptySet(),
        )
        override val handlers = listOf(
            object : ExtensionHandler<
                SubscriptionProviderDiscoverRequest,
                SubscriptionProviderDiscoverResult,
                > {
                override val spec = SubscriptionHookSpecs.Discover

                override suspend fun invoke(
                    context: ExtensionCallContext,
                    request: SubscriptionProviderDiscoverRequest,
                ): HookResult<SubscriptionProviderDiscoverResult> = handlerBlock(context, request)
            }
        )
    }

    private fun validDescriptor(providerId: ExtensionId) = SubscriptionProviderDescriptor(
        providerId = providerId,
        displayName = "Provider",
        variants = listOf(SubscriptionProviderVariant(PROVIDER_KIND, "Provider")),
        settingsSchema = ExtensionSettingSchema(
            version = 1,
            fields = listOf(
                ExtensionSettingField(
                    key = SubscriptionProviderSettingKeys.BaseUrl,
                    label = "Server",
                    type = ExtensionSettingType.TEXT,
                    required = true,
                )
            ),
        ),
    )

    private data object NoOpExtensionContributionScheduler : ExtensionContributionScheduler {
        override suspend fun enqueue(playlistUrl: String) = Unit

        override suspend fun cancel(playlistUrl: String) = Unit
    }

    private companion object {
        val VALID_PROVIDER_ID = ExtensionId("com.example.valid-provider")
        val INVALID_PROVIDER_ID = ExtensionId("com.example.invalid-provider")
        val FAILED_PROVIDER_ID = ExtensionId("com.example.failed-provider")
        val PROVIDER_KIND = ProviderKind("example")
    }
}
