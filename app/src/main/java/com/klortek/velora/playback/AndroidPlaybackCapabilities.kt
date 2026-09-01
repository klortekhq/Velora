package com.klortek.velora.playback

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi

/**
 * Conservative Android capability probe used by the shared playback contract.
 * Unknown capabilities stay unspecified instead of being guessed as supported.
 */
object AndroidPlaybackCapabilities {
    fun detect(context: Context): PlaybackCapabilities {
        val decoders: List<MediaCodecInfo> = runCatching {
            java.util.Arrays.asList(*MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos)
                .filterNot(MediaCodecInfo::isEncoder)
        }.getOrDefault(emptyList())
        val mimeTypes = mutableSetOf<String>()
        for (info in decoders) {
            val supportedTypes = java.util.Arrays.asList(*info.supportedTypes)
            for (mime in supportedTypes) mimeTypes += mime
        }

        val hdrFormats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            detectHdrFormats(context)
        } else {
            emptySet()
        }

        return PlaybackCapabilities(
            videoCodecs = mimeTypes.mapNotNull(::videoCodecForMime).toSet(),
            audioCodecs = mimeTypes.mapNotNull(::audioCodecForMime).toSet(),
            hdrFormats = hdrFormats,
            hdrCapabilityKnown = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            // Containers and passthrough are intentionally left open: Android's
            // codec list does not prove container or HDMI-path compatibility.
            containers = emptySet(),
            audioPassthrough = false,
            audioPassthroughCodecs = emptySet()
        )
    }

    private fun videoCodecForMime(mime: String): String? = when (mime.lowercase()) {
        "video/avc" -> "h264"
        "video/hevc" -> "hevc"
        "video/x-vnd.on2.vp9" -> "vp9"
        "video/av01" -> "av1"
        "video/mpeg2" -> "mpeg2video"
        "video/wvc1", "video/x-ms-wmv" -> "vc1"
        else -> null
    }

    private fun audioCodecForMime(mime: String): String? = when (mime.lowercase()) {
        "audio/mp4a-latm" -> "aac"
        "audio/ac3" -> "ac3"
        "audio/eac3", "audio/eac3-joc" -> "eac3"
        "audio/opus" -> "opus"
        "audio/flac" -> "flac"
        "audio/vnd.dts" -> "dts"
        "audio/vnd.dts.hd" -> "dts-hd"
        else -> null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun detectHdrFormats(context: Context): Set<String> {
        val display = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return emptySet()
        val supportedHdrTypes = display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        val formats = mutableSetOf<String>()
        for (typeIndex in supportedHdrTypes.indices) {
            val hdrType = supportedHdrTypes[typeIndex]
            when (hdrType) {
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> formats += "hdr10"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> formats += "hdr10+"
                Display.HdrCapabilities.HDR_TYPE_HLG -> formats += "hlg"
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> formats += "dolby-vision"
            }
        }
        return formats
    }
}
