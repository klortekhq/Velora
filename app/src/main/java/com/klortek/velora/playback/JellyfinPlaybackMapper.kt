package com.klortek.velora.playback

import com.klortek.velora.jellyfin.MediaSource
import com.klortek.velora.jellyfin.MediaStream

/** Converts Jellyfin's media metadata into the platform-neutral playback model. */
object JellyfinPlaybackMapper {
    fun source(mediaSource: MediaSource?, subtitleStreamIndex: Int? = null): PlaybackSource {
        val streams = mediaSource?.MediaStreams.orEmpty()
        val video = streams.firstOrNull { it.Type.equals("Video", ignoreCase = true) }
        val audio = streams.firstOrNull { it.Type.equals("Audio", ignoreCase = true) }
        val subtitle = subtitleStreamIndex?.let { index ->
            streams.firstOrNull { it.Type.equals("Subtitle", ignoreCase = true) && it.Index == index }
        }
        return PlaybackSource(
            container = mediaSource?.Container,
            videoCodec = video?.Codec,
            videoProfile = video?.Profile,
            videoLevel = video?.Level?.toString(),
            audioCodec = audio?.Codec,
            audioChannels = audio?.ChannelLayout?.let(::audioChannelCount)
                ?: audio?.Channels?.takeIf { it > 0 },
            hdrFormat = hdrFormat(video),
            width = video?.Width,
            height = video?.Height,
            frameRate = video?.RealFrameRate?.toDouble() ?: video?.AverageFrameRate?.toDouble(),
            bitrateKbps = video?.BitRate?.div(1000),
            subtitlesRequireTranscoding = subtitle?.let { requiresSubtitleTranscode(it) } == true
        )
    }

    fun capabilities(
        mediaSource: MediaSource?,
        device: PlaybackCapabilities
    ): PlaybackCapabilities = device.copy(
        directPlay = device.directPlay && mediaSource?.SupportsDirectPlay != false,
        directStream = device.directStream && mediaSource?.SupportsDirectStream != false
    )

    private fun hdrFormat(video: MediaStream?): String? = when {
        video == null -> null
        video.DvProfile != null || video.VideoDoViTitle?.contains("dolby vision", true) == true -> "dolby-vision"
        video.Hdr10PlusPresentFlag == true -> "hdr10+"
        video.VideoRangeType?.contains("HLG", true) == true || video.ColorTransfer?.contains("HLG", true) == true -> "hlg"
        video.VideoRangeType?.contains("HDR", true) == true || video.ColorTransfer?.contains("PQ", true) == true -> "hdr10"
        else -> null
    }

    private fun audioChannelCount(layout: String): Int? = when {
        layout.contains("7.1", true) -> 8
        layout.contains("5.1", true) -> 6
        layout.contains("stereo", true) -> 2
        layout.contains("mono", true) -> 1
        else -> layout.substringBefore('.').toIntOrNull()
    }

    private fun requiresSubtitleTranscode(stream: MediaStream): Boolean =
        stream.IsExternal != true && stream.IsTextSubtitleStream == false
}
