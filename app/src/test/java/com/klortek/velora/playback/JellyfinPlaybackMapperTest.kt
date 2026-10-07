package com.klortek.velora.playback

import com.klortek.velora.jellyfin.MediaSource
import com.klortek.velora.jellyfin.MediaStream
import org.junit.Assert.assertEquals
import org.junit.Test

class JellyfinPlaybackMapperTest {
    @Test
    fun immersiveLayoutPreservesHeightChannels() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(
                MediaStreams = listOf(
                    MediaStream(Type = "Audio", ChannelLayout = "7.1.4")
                )
            )
        )

        assertEquals(12, source.audioChannels)
    }

    @Test
    fun traditionalLayoutsRemainCompatible() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(
                MediaStreams = listOf(
                    MediaStream(Type = "Audio", ChannelLayout = "5.1")
                )
            )
        )

        assertEquals(6, source.audioChannels)
    }

    @Test
    fun layoutLabelsAfterNumericChannelsDoNotChangeTheCount() {
        val source = JellyfinPlaybackMapper.source(
            MediaSource(
                MediaStreams = listOf(
                    MediaStream(Type = "Audio", ChannelLayout = "7.1.4 (Dolby Atmos)")
                )
            )
        )

        assertEquals(12, source.audioChannels)
    }
}
