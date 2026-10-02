package com.sttiten.iptv.business.playlist.configuration

import com.sttiten.iptv.data.database.model.Playlist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaylistConfigurationStateTest {
    @Test
    fun `blank reference resolves to not found after playlists load`() {
        assertEquals(
            PlaylistConfigurationState.NotFound(
                playlistReference = playlistConfigurationReference(""),
            ),
            resolvePlaylistConfigurationState(
                playlists = listOf(PLAYLIST),
                playlistReference = "",
            ),
        )
    }

    @Test
    fun `stable reference resolves to content`() {
        assertEquals(
            PlaylistConfigurationState.Content(PLAYLIST),
            resolvePlaylistConfigurationState(
                playlists = listOf(PLAYLIST),
                playlistReference = playlistConfigurationReference(PLAYLIST.url),
            ),
        )
    }

    @Test
    fun `unknown reference resolves to not found`() {
        assertEquals(
            PlaylistConfigurationState.NotFound(
                playlistReference = playlistConfigurationReference(
                    "playlist-work:unknown"
                ),
            ),
            resolvePlaylistConfigurationState(
                playlists = listOf(PLAYLIST),
                playlistReference = "playlist-work:unknown",
            ),
        )
    }

    @Test
    fun `playlist title is trimmed before persistence`() {
        assertEquals(
            "Living room",
            normalizePlaylistTitle(" \u202ELiving\u2066 room\n"),
        )
    }

    @Test
    fun `blank playlist title is rejected`() {
        assertNull(normalizePlaylistTitle(" \t\n"))
    }

    @Test
    fun `playlist title is bounded before persistence`() {
        assertEquals(
            256,
            normalizePlaylistTitle("x".repeat(300))?.length,
        )
    }

    @Test
    fun `user agent removes controls and blank value becomes null`() {
        assertEquals(
            "STTITEN IP TV",
            normalizePlaylistUserAgent(" \u202EM3U\n Android "),
        )
        assertNull(normalizePlaylistUserAgent("\u2066\t\n"))
    }

    private companion object {
        val PLAYLIST = Playlist(
            title = "Living room",
            url = "https://example.com/playlist.m3u",
        )
    }
}
