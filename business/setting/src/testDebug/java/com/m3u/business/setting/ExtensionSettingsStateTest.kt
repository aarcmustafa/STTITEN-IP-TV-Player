package com.sttiten.iptv.business.setting

import com.sttiten.iptv.data.repository.extension.ExtensionSettingsConfiguration
import com.sttiten.iptv.extension.api.ExtensionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ExtensionSettingsStateTest {
    @Test
    fun closedHasNoExtensionIdentity() {
        assertNull(ExtensionSettingsState.Closed.extensionId)
    }

    @Test
    fun inFlightAndTerminalFailureStatesKeepRequestedIdentity() {
        val extensionId = ExtensionId("dev.example.extension")

        assertEquals(
            extensionId,
            ExtensionSettingsState.Loading(extensionId).extensionId,
        )
        assertEquals(
            extensionId,
            ExtensionSettingsState.Unavailable(extensionId).extensionId,
        )
        assertEquals(
            extensionId,
            ExtensionSettingsState.Error(extensionId).extensionId,
        )
    }

    @Test
    fun missingConfigurationIsUnavailableRatherThanClosed() {
        val extensionId = ExtensionId("dev.example.extension")
        val configuration: ExtensionSettingsConfiguration? = null

        val state = configuration.toExtensionSettingsState(extensionId)

        assertEquals(
            extensionId,
            assertIs<ExtensionSettingsState.Unavailable>(state).extensionId,
        )
    }
}
