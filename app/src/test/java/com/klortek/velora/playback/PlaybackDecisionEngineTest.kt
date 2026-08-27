package com.klortek.velora.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDecisionEngineTest {
    private val capable = PlaybackCapabilities(
        videoCodecs = setOf("h264", "hevc"),
        audioCodecs = setOf("aac", "eac3"),
        hdrFormats = setOf("hdr10")
    )

    @Test
    fun originalKeepsCompatibleSourceOnDirectPlay() {
        assertEquals(
            PlaybackPath.DIRECT_PLAY,
            PlaybackDecisionEngine.decide(
                PlaybackSource("hevc", "eac3", "hdr10", 3840, 2160, 24.0),
                capable
            )
        )
    }

    @Test
    fun originalDoesNotApplyPresetResolutionCap() {
        assertEquals(
            PlaybackPath.DIRECT_PLAY,
            PlaybackDecisionEngine.decide(
                PlaybackSource("hevc", "eac3", "hdr10", 7680, 4320, 24.0),
                capable,
                PlaybackQuality.ORIGINAL
            )
        )
    }

    @Test
    fun unsupportedVideoUsesTranscodeAsLastResort() {
        assertEquals(
            PlaybackPath.TRANSCODE,
            PlaybackDecisionEngine.decide(
                PlaybackSource("av1", "aac", null, 1920, 1080),
                capable
            )
        )
    }

    @Test
    fun subtitleTranscodingAvoidsDirectPlay() {
        assertEquals(
            PlaybackPath.REMUX,
            PlaybackDecisionEngine.decide(
                PlaybackSource("h264", "aac", null, 1920, 1080, subtitlesRequireTranscoding = true),
                capable
            )
        )
    }
}
