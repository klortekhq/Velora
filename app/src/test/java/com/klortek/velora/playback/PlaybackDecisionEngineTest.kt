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
    fun glEnhancementsRequireKnownNonAv1Codec() {
        assertEquals(false, VideoRenderPolicy.shouldUseGlEnhancements(true, null))
        assertEquals(false, VideoRenderPolicy.shouldUseGlEnhancements(true, "av01"))
        assertEquals(true, VideoRenderPolicy.shouldUseGlEnhancements(true, "h264"))
        assertEquals(false, VideoRenderPolicy.shouldUseGlEnhancements(false, "h264"))
    }

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
    fun selectedPresetTranscodesSourceAboveItsBitrate() {
        assertEquals(
            PlaybackPath.TRANSCODE,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "h264", audioCodec = "aac", width = 1920, height = 1080, bitrateKbps = 30_000),
                capable,
                PlaybackQuality.FULL_HD_10
            )
        )
    }

    @Test
    fun detailedDecisionExplainsQualityFallback() {
        val decision = PlaybackDecisionEngine.decideDetailed(
            PlaybackSource(videoCodec = "h264", audioCodec = "aac", width = 1920, height = 1080, bitrateKbps = 30_000),
            capable,
            PlaybackQuality.FULL_HD_10
        )

        assertEquals(PlaybackPath.TRANSCODE, decision.path)
        assertEquals("source exceeds device or selected-quality capabilities", decision.reason)
    }

    @Test
    fun deviceResolutionLimitPreventsDirectStream() {
        val limited = capable.copy(maxWidth = 1920, maxHeight = 1080)
        assertEquals(
            PlaybackPath.TRANSCODE,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "hevc", audioCodec = "eac3", width = 3840, height = 2160),
                limited
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
            PlaybackPath.TRANSCODE,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "hevc", audioCodec = "truehd", width = 1920, height = 1080),
                withPassthrough
            )
        )
    }

    @Test
    fun knownSdrDisplayDoesNotDirectPlayHdrSource() {
        val sdr = capable.copy(hdrFormats = emptySet(), hdrCapabilityKnown = true)
        assertEquals(
            PlaybackPath.TRANSCODE,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "hevc", audioCodec = "eac3", hdrFormat = "hdr10", width = 1920, height = 1080),
                sdr
            )
        )
    }

    @Test
    fun qualityPresetChecksBothWidthAndHeight() {
        assertEquals(
            PlaybackPath.TRANSCODE,
            PlaybackDecisionEngine.decide(
                PlaybackSource(videoCodec = "h264", audioCodec = "aac", width = 1280, height = 1080),
                capable,
                PlaybackQuality.HD_5
            )
        )
    }

    @Test
    fun equivalentJellyfinAndAndroidCodecNamesRemainDirectPlayable() {
        val source = PlaybackSource(
            container = "matroska",
            videoCodec = "H265",
            audioCodec = "EC-3",
            hdrFormat = "Dolby Vision",
            width = 1920,
            height = 1080
        )
        val capabilities = PlaybackCapabilities(
            videoCodecs = setOf("hevc"),
            audioCodecs = setOf("eac3"),
            hdrFormats = setOf("dolby-vision"),
            containers = setOf("mkv")
        )

        assertEquals(PlaybackPath.DIRECT_PLAY, PlaybackDecisionEngine.decide(source, capabilities))
    }

    @Test
    fun commonJellyfinCodecAliasesRemainDirectPlayable() {
        val source = PlaybackSource(
            videoCodec = "AVC",
            audioCodec = "AC-3",
            hdrFormat = "HDR10Plus",
            container = "mpegts"
        )
        val capabilities = PlaybackCapabilities(
            videoCodecs = setOf("h264"),
            audioCodecs = setOf("ac3"),
            hdrFormats = setOf("hdr10+"),
            containers = setOf("ts")
        )

        assertEquals(PlaybackPath.DIRECT_PLAY, PlaybackDecisionEngine.decide(source, capabilities))
    }

    @Test
    fun disabledDirectPlayStillUsesDirectStreamBeforeRemuxOrTranscode() {
        val source = PlaybackSource(videoCodec = "h264", audioCodec = "aac")
        val capabilities = capable.copy(directPlay = false)

        assertEquals(PlaybackPath.DIRECT_STREAM, PlaybackDecisionEngine.decide(source, capabilities))
    }
}
