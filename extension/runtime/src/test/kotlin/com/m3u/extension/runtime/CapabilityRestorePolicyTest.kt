package com.sttiten.iptv.extension.runtime

import com.sttiten.iptv.extension.api.ExtensionApiRange
import com.sttiten.iptv.extension.api.ExtensionApiVersions
import com.sttiten.iptv.extension.api.ExtensionCapabilityIds
import com.sttiten.iptv.extension.api.ExtensionCapabilityRequest
import com.sttiten.iptv.extension.api.ExtensionId
import com.sttiten.iptv.extension.api.ExtensionManifest
import com.sttiten.iptv.extension.api.ExtensionSemanticVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CapabilityRestorePolicyTest {
    @Test
    fun newRequiredCapabilityRequiresExplicitReauthorization() {
        val decision = reconcileCapabilitiesForRestore(
            manifest(
                ExtensionCapabilityRequest(ExtensionCapabilityIds.SearchRead, "Search"),
                ExtensionCapabilityRequest(ExtensionCapabilityIds.MetadataWrite, "Metadata"),
            ),
            previousGrants = setOf(ExtensionCapabilityIds.SearchRead.id),
        )

        assertTrue(decision.requiresReauthorization)
        assertEquals(setOf(ExtensionCapabilityIds.MetadataWrite.id), decision.missingRequired)
        assertEquals(setOf(ExtensionCapabilityIds.SearchRead.id), decision.granted)
    }

    @Test
    fun newOptionalCapabilityIsNotGrantedDuringRestore() {
        val decision = reconcileCapabilitiesForRestore(
            manifest(
                ExtensionCapabilityRequest(ExtensionCapabilityIds.SearchRead, "Search"),
                ExtensionCapabilityRequest(
                    ExtensionCapabilityIds.MetadataWrite,
                    "Optional metadata",
                    required = false,
                ),
            ),
            previousGrants = setOf(ExtensionCapabilityIds.SearchRead.id),
        )

        assertFalse(decision.requiresReauthorization)
        assertEquals(setOf(ExtensionCapabilityIds.SearchRead.id), decision.granted)
    }

    @Test
    fun removedCapabilitiesAreDroppedRatherThanCarriedForward() {
        val decision = reconcileCapabilitiesForRestore(
            manifest(ExtensionCapabilityRequest(ExtensionCapabilityIds.SearchRead, "Search")),
            previousGrants = setOf(
                ExtensionCapabilityIds.SearchRead.id,
                ExtensionCapabilityIds.BackgroundTask.id,
            ),
        )

        assertFalse(decision.requiresReauthorization)
        assertEquals(setOf(ExtensionCapabilityIds.SearchRead.id), decision.granted)
    }

    private fun manifest(
        vararg capabilities: ExtensionCapabilityRequest,
    ): ExtensionManifest = ExtensionManifest(
        id = ExtensionId("com.sttiten.iptv.test.capabilities"),
        displayName = "Capability test",
        extensionVersion = ExtensionSemanticVersion(1, 0, 0),
        apiRange = ExtensionApiRange(ExtensionApiVersions.Current, ExtensionApiVersions.Current),
        hooks = emptySet(),
        capabilities = capabilities.toSet(),
    )
}
