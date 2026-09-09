package com.klortek.velora.playback

import com.klortek.velora.jellyfin.MediaSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTvPlaybackPolicyTest {
    @Test
    fun m3uNeverBypassesJellyfinLiveStreamRemux() {
        assertFalse(
            shouldUseLiveTvDirectSource(
                MediaSource(
                    Protocol = "m3u",
                    Path = "http://server.example/live/direct.m3u8",
                    SupportsDirectPlay = true
                )
            )
        )
    }

    @Test
    fun nonM3uDirectSourceCanUseDirectPlayWhenServerAllowsIt() {
        assertTrue(
            shouldUseLiveTvDirectSource(
                MediaSource(Protocol = "Http", SupportsDirectPlay = true)
            )
        )
    }
}
