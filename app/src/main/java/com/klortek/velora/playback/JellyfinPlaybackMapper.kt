package com.klortek.velora.playback

import com.klortek.velora.jellyfin.MediaSource
import com.klortek.velora.jellyfin.MediaStream

/** Converts Jellyfin's media metadata into the platform-neutral playback model. */
object JellyfinPlaybackMapper {
    fun source(mediaSource: MediaSource?, subtitleStreamIndex: Int? = null): PlaybackSource {
        val streams = mediaSource?.MediaStreams.orEmpty()
        // Jellyfin does not guarantee that the default stream is first. The
        // playback decision must describe the stream the server selected, not
        // an arbitrary stream ordering from the media source.
        val videoStreams = streams.filter { it.Type.equals("Video", ignoreCase = true) }
        val audioStreams = streams.filter { it.Type.equals("Audio", ignoreCase = true) }
        val video = videoStreams.firstOrNull { it.IsDefault == true } ?: videoStreams.firstOrNull()
        val audio = audioStreams.firstOrNull { it.IsDefault == true } ?: audioStreams.firstOrNull()
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

    private fun audioChannelCount(layout: String): Int? {
        val normalized = layout.trim().lowercase()
        when {
            normalized.contains("stereo") -> return 2
            normalized.contains("mono") -> return 1
        }

        // Jellyfin may expose immersive layouts such as 7.1.4. Counting only
        // the bed (7.1 = 8) under-reports the stream and can incorrectly make
        // a receiver appear capable of direct playback. Sum the numeric layout
        // components while keeping the familiar 5.1/7.1 behavior intact.
        val layoutMatch = Regex("^(\\d+)\\s*\\.\\s*(\\d+)(?:\\s*\\.\\s*(\\d+))?")
            .find(normalized)
        if (layoutMatch != null) {
            return layoutMatch.groupValues
                .drop(1)
                .filter(String::isNotBlank)
                .sumOf(String::toInt)
        }

        return normalized.takeWhile(Char::isDigit).toIntOrNull()
    }

    private fun requiresSubtitleTranscode(stream: MediaStream): Boolean =
        stream.IsExternal != true && stream.IsTextSubtitleStream == false
}
