package com.sttiten.iptv.business.setting

import com.sttiten.iptv.extension.api.ExtensionId
import com.sttiten.iptv.extension.api.subscription.ProviderKind

data class ProviderSubmissionOperation(
    val providerId: ExtensionId,
    val providerKind: ProviderKind,
    val reauthenticationPlaylistUrl: String? = null,
)

data class ProviderOperationState(
    val submission: ProviderSubmissionOperation? = null,
    val preparingReauthenticationPlaylistUrl: String? = null,
) {
    val isSubmitting: Boolean
        get() = submission != null

    val isBusy: Boolean
        get() = submission != null || preparingReauthenticationPlaylistUrl != null

    fun isReauthenticating(playlistUrl: String): Boolean =
        preparingReauthenticationPlaylistUrl == playlistUrl ||
            submission?.reauthenticationPlaylistUrl == playlistUrl
}
