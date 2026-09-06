package com.klortek.velora.playback

/** Native playback engines available to the Android client. */
enum class PlaybackBackend {
    MEDIA3,
    MPV
}

data class PlaybackBackendPreferences(
    /** MPV is opt-in; a fresh install must always start on Media3/ExoPlayer. */
    val mpvExplicitlyEnabled: Boolean = false,
    /** MPV may be used only after Media3 reports an actual decoder failure. */
    val allowMpvFallback: Boolean = true
)

/**
 * Keeps the initial-engine and fallback rules in one testable place.  This
 * prevents a legacy preference or a platform-specific entry point from
 * silently making MPV the default.
 */
object PlaybackBackendSelector {
    fun initialBackend(preferences: PlaybackBackendPreferences): PlaybackBackend =
        if (preferences.mpvExplicitlyEnabled) PlaybackBackend.MPV else PlaybackBackend.MEDIA3

    fun fallbackBackend(
        current: PlaybackBackend,
        preferences: PlaybackBackendPreferences,
        decoderFailed: Boolean
    ): PlaybackBackend? =
        if (current == PlaybackBackend.MEDIA3 && preferences.allowMpvFallback && decoderFailed) {
            PlaybackBackend.MPV
        } else {
            null
        }
}
