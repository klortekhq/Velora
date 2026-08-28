package com.klortek.velora.player.mpv

import com.klortek.velora.BuildConfig

/**
 * Builds Jellyfin-compatible URLs for MPV playback.
 * 
 * IMPORTANT: Jellyfin requires lowercase parameter names!
 */
object MpvUrlBuilder {
    
    /**
     * Build HTTP headers for Jellyfin authentication.
     * Uses CRLF line endings as required by MPV.
     */
    fun buildHeaders(
        accessToken: String,
        deviceId: String,
        clientName: String = "Velora",
        version: String = BuildConfig.VERSION_NAME
    ): String = buildString {
        append("User-Agent: $clientName/MPV\r\n")
        append("Authorization: MediaBrowser Token=\"$accessToken\"\r\n")
        append("X-Emby-Authorization: MediaBrowser ")
        append("Client=\"$clientName\", Device=\"AndroidTV\", DeviceId=\"$deviceId\", ")
        append("Token=\"$accessToken\", Version=\"$version\"\r\n")
        append("Accept: */*\r\n")
    }
    
    /**
     * Build direct stream URL for Jellyfin.
     * 
     * Uses lowercase parameter names as required by Jellyfin.
     * Always uses static=true for direct streaming - resume is handled client-side by MPV.
     */
    fun buildStreamUrl(
        serverUrl: String,
        itemId: String,
        accessToken: String,
        mediaSourceId: String? = null,
        container: String? = null,
        startTimeTicks: Long? = null // Ignored - resume handled client-side
    ): String {
        val baseUrl = serverUrl.removeSuffix("/")
        return buildString {
            append("$baseUrl/Videos/$itemId/stream?")
            // Always use static=true for direct streaming without transcoding
            // Resume position is handled client-side by MPV seeking after load
            append("static=true")
            append("&mediaSourceId=${mediaSourceId ?: itemId}")
            append("&enableAutoStreamCopy=true")
            append("&allowVideoStreamCopy=true")
            append("&allowAudioStreamCopy=true")
            container?.let { append("&container=$it") }
        }
    }

    /**
     * Build the direct Jellyfin Live TV HLS endpoint.
     *
     * Live TV must use the Jellyfin master manifest with the MediaSourceId and
     * LiveStreamId returned by PlaybackInfo. The server creates/opens the
     * upstream stream as part of that request.
     */
    fun buildLiveTvStreamUrl(
        serverUrl: String,
        itemId: String,
        accessToken: String,
        mediaSourceId: String?,
        liveStreamId: String?
    ): String {
        val baseUrl = serverUrl.removeSuffix("/")
        return buildString {
            append("$baseUrl/Videos/$itemId/master.m3u8?")
            // Jellyfin otherwise derives AudioCodec from the M3U source and
            // can emit the invalid `AudioCodec=m3u8` query. Explicit HLS
            // codecs keep the manifest valid while Jellyfin still decides
            // whether the upstream can be copied or must be remuxed.
            mediaSourceId?.takeIf { it.isNotBlank() }?.let { append("&MediaSourceId=$it") }
            liveStreamId?.takeIf { it.isNotBlank() }?.let { append("&LiveStreamId=$it") }
            append("&VideoCodec=h264")
            append("&AudioCodec=aac")
            append("&TranscodingProtocol=hls")
            append("&TranscodingContainer=ts")
            append("&EnableAutoStreamCopy=true")
            append("&AllowVideoStreamCopy=true")
            append("&AllowAudioStreamCopy=true")
        }
    }

    /**
     * Direct Play URL returned by Jellyfin's Live TV PlaybackInfo.
     * We do not read or parse the provider M3U in the app; Jellyfin resolves
     * the selected channel and returns this source URL through its API.
     */
    fun buildLiveTvDirectSourceUrl(sourcePath: String): String = sourcePath

    /**
     * Build direct download URL for Jellyfin.
     * This is the most compatible option.
     */
    fun buildDownloadUrl(
        serverUrl: String,
        itemId: String,
        accessToken: String,
        mediaSourceId: String? = null
    ): String {
        val baseUrl = serverUrl.removeSuffix("/")
        return "$baseUrl/Items/$itemId/Download?mediaSourceId=${mediaSourceId ?: itemId}"
    }
}
