package com.klortek.velora.security

import com.klortek.velora.player.mpv.MpvUrlBuilder
import com.klortek.velora.player.mpv.MpvVeloraLauncher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlSecurityTest {
    @Test
    fun playbackUrlsStripCredentialQueryParameters() {
        val sanitized = MediaUrlHeaderPolicy.stripCredentialQueryParameters(
            "https://jellyfin.example/Videos/movie/master.m3u8?MediaSourceId=source&api_key=secret&token=legacy#fragment"
        )

        org.junit.Assert.assertEquals(
            "https://jellyfin.example/Videos/movie/master.m3u8?MediaSourceId=source#fragment",
            sanitized
        )
    }

    @Test
    fun jellyfinHeadersAreScopedToTheConfiguredServerAndPath() {
        assertTrue(MediaUrlHeaderPolicy.isServerResource(
            "https://jellyfin.test/base",
            "https://jellyfin.test/base/Videos/channel/master.m3u8"
        ))
        assertFalse(MediaUrlHeaderPolicy.isServerResource(
            "https://jellyfin.test/base",
            "https://provider.test/channel/master.m3u8"
        ))
        assertFalse(MediaUrlHeaderPolicy.isServerResource(
            "https://jellyfin.test/base",
            "https://jellyfin.test/other-provider/channel.m3u8"
        ))
        assertFalse(MediaUrlHeaderPolicy.isServerResource(
            "https://jellyfin.test/base",
            "content://velora/offline/item-1"
        ))
    }

    @Test
    fun mpvPlaybackUrlsDoNotEmbedTheAccessToken() {
        val stream = MpvUrlBuilder.buildStreamUrl(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "movie-id",
            accessToken = "secret-token",
            mediaSourceId = "source-id"
        )
        val live = MpvUrlBuilder.buildLiveTvStreamUrl(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "channel-id",
            accessToken = "secret-token",
            mediaSourceId = "source-id",
            liveStreamId = "live-id"
        )

        assertFalse(stream.contains("secret-token"))
        assertFalse(stream.contains("api_key", ignoreCase = true))
        assertFalse(live.contains("secret-token"))
        assertFalse(live.contains("api_key", ignoreCase = true))
        assertTrue(stream.contains("mediaSourceId=source-id"))
        assertTrue(live.contains("LiveStreamId=live-id"))
    }

    @Test
    fun exoPlayerLiveTvUrlPreservesServerCodecDecision() {
        val url = MpvUrlBuilder.buildLiveTvStreamUrlForExoPlayer(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "channel-id",
            accessToken = "secret-token",
            mediaSourceId = "source-id",
            liveStreamId = "live-id"
        )

        assertTrue(url.contains("MediaSourceId=source-id"))
        assertTrue(url.contains("LiveStreamId=live-id"))
        assertTrue(url.contains("EnableAutoStreamCopy=true"))
        assertFalse(url.contains("VideoCodec", ignoreCase = true))
        assertFalse(url.contains("AudioCodec", ignoreCase = true))
        assertFalse(url.contains("secret-token"))
    }

    @Test
    fun mpvAuthenticationIsCarriedByHeadersInsteadOfTheMediaUrl() {
        val headers = MpvUrlBuilder.buildHeaders(
            accessToken = "secret-token",
            deviceId = "test-device"
        )
        val url = MpvVeloraLauncher.buildStreamUrl(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "movie-id",
            accessToken = "secret-token"
        )

        assertTrue(headers.contains("Authorization: MediaBrowser"))
        assertFalse(headers.contains("X-Emby-Authorization"))
        assertTrue(headers.contains("Token=\"secret-token\""))
        assertFalse(url.contains("secret-token"))
        assertFalse(url.contains("token=", ignoreCase = true))
    }

    @Test
    fun mpvDownloadUrlDoesNotEmbedTheAccessToken() {
        val url = MpvUrlBuilder.buildDownloadUrl(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "episode-id",
            accessToken = "secret-token",
            mediaSourceId = "source-id"
        )

        assertFalse(url.contains("secret-token"))
        assertFalse(url.contains("api_key", ignoreCase = true))
        assertTrue(url.contains("mediaSourceId=source-id"))
    }

    @Test
    fun mpvQueryValuesAreEncoded() {
        val url = MpvUrlBuilder.buildStreamUrl(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "movie-id",
            accessToken = "secret-token",
            mediaSourceId = "source id&part"
        )

        assertTrue(url.contains("mediaSourceId=source+id%26part"))
    }

    @Test
    fun launcherQueryValuesAreEncoded() {
        val url = MpvVeloraLauncher.buildStreamUrl(
            serverUrl = "http://jellyfin.test:8096",
            itemId = "movie-id",
            accessToken = "secret-token",
            mediaSourceId = "source id&part"
        )

        assertTrue(url.contains("mediaSourceId=source+id%26part"))
    }

    @Test
    fun directLiveSourceStripsCredentialQueryParameters() {
        val url = MpvUrlBuilder.buildLiveTvDirectSourceUrl(
            "https://stream.test/channel.m3u8?api_key=secret&quality=hd&token=also-secret#live"
        )

        assertFalse(url.contains("secret"))
        assertFalse(url.contains("token="))
        assertTrue(url.contains("quality=hd"))
        assertTrue(url.endsWith("#live"))
    }
}
