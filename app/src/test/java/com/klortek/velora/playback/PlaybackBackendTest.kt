package com.klortek.velora.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackBackendTest {
    @Test
    fun freshPreferencesUseMedia3() {
        assertEquals(
            PlaybackBackend.MEDIA3,
            PlaybackBackendSelector.initialBackend(PlaybackBackendPreferences())
        )
    }

    @Test
    fun mpvRequiresExplicitUserChoiceForInitialPlayback() {
        assertEquals(
            PlaybackBackend.MEDIA3,
            PlaybackBackendSelector.initialBackend(
                PlaybackBackendPreferences(mpvExplicitlyEnabled = false)
            )
        )
        assertEquals(
            PlaybackBackend.MPV,
            PlaybackBackendSelector.initialBackend(
                PlaybackBackendPreferences(mpvExplicitlyEnabled = true)
            )
        )
    }

    @Test
    fun mpvFallbackRequiresARealDecoderFailure() {
        val preferences = PlaybackBackendPreferences(allowMpvFallback = true)
        assertNull(PlaybackBackendSelector.fallbackBackend(PlaybackBackend.MEDIA3, preferences, false))
        assertEquals(
            PlaybackBackend.MPV,
            PlaybackBackendSelector.fallbackBackend(PlaybackBackend.MEDIA3, preferences, true)
        )
        assertNull(PlaybackBackendSelector.fallbackBackend(PlaybackBackend.MPV, preferences, true))
    }

    @Test
    fun fallbackCanBeDisabled() {
        assertNull(
            PlaybackBackendSelector.fallbackBackend(
                PlaybackBackend.MEDIA3,
                PlaybackBackendPreferences(allowMpvFallback = false),
                decoderFailed = true
            )
        )
    }
}
