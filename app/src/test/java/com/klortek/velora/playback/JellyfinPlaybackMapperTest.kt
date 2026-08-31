package com.klortek.velora.playback

import com.klortek.velora.jellyfin.MediaSource
import com.klortek.velora.jellyfin.MediaStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinPlaybackMapperTest {
    @Test
    fun mapsHdrAudioDimensionsAndBitrate() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(
                Container = "mkv",
                MediaStreams = listOf(
                    MediaStream(Type = "Video", Codec = "hevc", Width = 3840, Height = 2160, BitRate = 40_000_000, VideoRangeType = "HDR10", RealFrameRate = 23.976f),
                    MediaStream(Type = "Audio", Codec = "truehd", ChannelLayout = "7.1")
                )
            )
        )
        assertEquals("mkv", source.container)
        assertEquals("hdr10", source.hdrFormat)
        assertEquals(8, source.audioChannels)
        assertEquals(40_000, source.bitrateKbps)
        assertEquals(PlaybackPath.DIRECT_PLAY, PlaybackDecisionEngine.decide(source, PlaybackCapabilities(videoCodecs = setOf("hevc"), audioCodecs = setOf("truehd"), hdrFormats = setOf("hdr10"), containers = setOf("mkv"))))
    }

    @Test
    fun bitmapEmbeddedSubtitleRequiresTranscoding() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(MediaStreams = listOf(MediaStream(Type = "Subtitle", Index = 2, IsExternal = false, IsTextSubtitleStream = false))),
            subtitleStreamIndex = 2
        )
        assertTrue(source.subtitlesRequireTranscoding)
    }

    @Test
    fun fallsBackToNumericAudioChannelsWhenLayoutIsMissing() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(MediaStreams = listOf(MediaStream(Type = "Audio", Codec = "eac3", Channels = 6)))
        )
        assertEquals(6, source.audioChannels)
    }

    @Test
    fun mapsTheServerDefaultAudioAndVideoStreamsWhenTheyAreNotFirst() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(
                MediaStreams = listOf(
                    MediaStream(Type = "Video", Codec = "h264", Width = 1920, Height = 1080),
                    MediaStream(Type = "Video", Codec = "hevc", Width = 3840, Height = 2160, IsDefault = true),
                    MediaStream(Type = "Audio", Codec = "aac", Channels = 2),
                    MediaStream(Type = "Audio", Codec = "eac3", ChannelLayout = "5.1", IsDefault = true)
                )
            )
        )

        assertEquals("hevc", source.videoCodec)
        assertEquals("eac3", source.audioCodec)
        assertEquals(6, source.audioChannels)
        assertEquals(3840, source.width)
    }
}
