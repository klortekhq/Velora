package com.klortek.velora.player

import com.klortek.velora.jellyfin.MediaStream

enum class PlaybackQuality(val label: String, val maxWidth: Int?, val maxHeight: Int?, val maxBitrateKbps: Int?) {
    ORIGINAL("Original", null, null, null),
    AUTO("Automática", 3840, 2160, 40000),
    UHD_4K("4K / 40 Mbps", 3840, 2160, 40000),
    HD_20("1080p / 20 Mbps", 1920, 1080, 20000),
    HD_10("1080p / 10 Mbps", 1920, 1080, 10000),
    HD_720("720p / 5 Mbps", 1280, 720, 5000),
    SD_480("480p / 2 Mbps", 854, 480, 2000)
}

/**
 * Original-first playback metadata helpers.
 * These helpers never request a lower quality stream; they only classify the source.
 */
object OriginalQualityPolicy {
    fun isDolbyVision(stream: MediaStream?): Boolean {
        if (stream == null) return false
        val doviText = listOfNotNull(stream.VideoDoViTitle, stream.VideoRange, stream.VideoRangeType)
            .joinToString(" ")
            .lowercase()
        return (stream.DvProfile ?: 0) > 0 ||
            (stream.RpuPresentFlag ?: 0) > 0 ||
            doviText.contains("dolby") || doviText.contains("dovi") || doviText.contains("dvhe")
    }

    fun isHdr(stream: MediaStream?): Boolean {
        if (stream == null) return false
        if (isDolbyVision(stream)) return true
        if (stream.Hdr10PlusPresentFlag == true) return true

        val range = listOfNotNull(stream.VideoRange, stream.VideoRangeType, stream.Profile)
            .joinToString(" ")
            .lowercase()
        val transfer = stream.ColorTransfer?.lowercase().orEmpty()

        return range.contains("hdr") ||
            range.contains("hlg") ||
            transfer.contains("smpte2084") || transfer.contains("2084") ||
            transfer.contains("pq") ||
            transfer.contains("arib-std-b67") || transfer.contains("hlg")
    }

    fun hdrLabel(stream: MediaStream?): String {
        if (stream == null) return "SDR"
        if (isDolbyVision(stream)) return "Dolby Vision"
        if (stream.Hdr10PlusPresentFlag == true) return "HDR10+"
        val range = listOfNotNull(stream.VideoRangeType, stream.VideoRange).joinToString(" ").lowercase()
        val transfer = stream.ColorTransfer?.lowercase().orEmpty()
        return when {
            range.contains("hlg") || transfer.contains("arib-std-b67") || transfer.contains("hlg") -> "HLG"
            isHdr(stream) -> "HDR10"
            else -> "SDR"
        }
    }
}
