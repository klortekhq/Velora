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
                PlaybackSource(videoCodec = "hevc", audioCodec = "eac3", hdrFormat = "hdr10", width = 3840, height = 2160, frameRate = 24.0),
                capable
            )
        )
    }

    @Test
    fun originalDoesNotApplyPresetResolutionCap() {
        assertEquals(
            PlaybackPath.DIRECT_PLAY,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "hevc", audioCodec = "eac3", hdrFormat = "hdr10", width = 7680, height = 4320, frameRate = 24.0),
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
                PlaybackSource(videoCodec = "av1", audioCodec = "aac", width = 1920, height = 1080),
                capable
            )
        )
    }

    @Test
    fun subtitleTranscodingAvoidsDirectPlay() {
        assertEquals(
            PlaybackPath.REMUX,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "h264", audioCodec = "aac", width = 1920, height = 1080, subtitlesRequireTranscoding = true),
                capable
            )
        )
    }

    @Test
    fun selectedPresetRejectsSourceAboveItsBitrate() {
        assertEquals(
            PlaybackPath.DIRECT_STREAM,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "h264", audioCodec = "aac", width = 1920, height = 1080, bitrateKbps = 30_000),
                capable,
                PlaybackQuality.FULL_HD_10
            )
        )
    }

    @Test
    fun passthroughCodecRequiresExplicitDeviceSupport() {
        val withPassthrough = capable.copy(
            audioCodecs = setOf("aac", "eac3", "truehd"),
            audioPassthroughCodecs = setOf("truehd"),
            audioPassthrough = false
        )
        assertEquals(
            PlaybackPath.REMUX,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "hevc", audioCodec = "truehd", width = 1920, height = 1080),
                withPassthrough
            )
        )
    }
}
