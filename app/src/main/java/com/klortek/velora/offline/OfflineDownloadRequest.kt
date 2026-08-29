package com.klortek.velora.offline

import java.net.URLEncoder

/** Builds the Jellyfin endpoint used by the managed mobile download queue. */
internal object OfflineDownloadRequest {
    fun url(
        serverUrl: String,
        itemId: String,
        mediaSourceId: String?,
        quality: OfflineDownloadQuality
    ): String {
        val base = serverUrl.trimEnd('/')
        val source = mediaSourceId ?: itemId
        return if (quality == OfflineDownloadQuality.ORIGINAL) {
            "$base/Items/${encode(itemId)}/Download?mediaSourceId=${encode(source)}"
        } else {
            buildString {
                append("$base/Videos/${encode(itemId)}/stream.mp4?")
                append("mediaSourceId=${encode(source)}")
                append("&VideoCodec=h264&AudioCodec=aac&AudioBitrate=384000")
                quality.maxWidth?.let { append("&MaxWidth=$it") }
                quality.maxHeight?.let { append("&MaxHeight=$it") }
                quality.videoBitrate?.let { append("&VideoBitrate=$it&MaxStreamingBitrate=$it") }
                append("&TranscodingMaxAudioChannels=2")
            }
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
