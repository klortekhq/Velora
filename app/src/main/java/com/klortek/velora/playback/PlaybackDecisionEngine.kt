package com.klortek.velora.playback

/** Shared playback semantics used by native platform backends. */
enum class PlaybackPath { DIRECT_PLAY, DIRECT_STREAM, REMUX, TRANSCODE, FALLBACK }

enum class PlaybackQuality { ORIGINAL, AUTOMATIC, FOUR_K, FULL_HD_20, FULL_HD_10, HD_5, SD_2 }

data class PlaybackCapabilities(
    val directPlay: Boolean = true,
    val directStream: Boolean = true,
    val remux: Boolean = true,
    val videoCodecs: Set<String> = emptySet(),
    val audioCodecs: Set<String> = emptySet(),
    val hdrFormats: Set<String> = emptySet(),
    val containers: Set<String> = emptySet(),
    val audioPassthroughCodecs: Set<String> = emptySet(),
    val audioPassthrough: Boolean = false,
    /** True when the platform has positively reported its HDR display support. */
    val hdrCapabilityKnown: Boolean = false,
    val maxAudioChannels: Int? = null,
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val maxFrameRate: Double? = null
)

data class PlaybackSource(
    val container: String? = null,
    val videoCodec: String? = null,
    val videoProfile: String? = null,
    val videoLevel: String? = null,
    val audioCodec: String? = null,
    val audioChannels: Int? = null,
    val hdrFormat: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Double? = null,
    val bitrateKbps: Int? = null,
    val subtitlesRequireTranscoding: Boolean = false
)

/**
 * The decision plus a stable, human-readable reason for diagnostics.  The
 * player-facing API remains [decide] for compatibility, while new backends
 * can use this richer result without duplicating negotiation rules.
 */
data class PlaybackDecision(
    val path: PlaybackPath,
    val reason: String
)

/**
 * Chooses the least destructive path. Original quality never adds an artificial
 * cap; limits only come from actual device capability or the selected preset.
 */
object PlaybackDecisionEngine {
    fun decide(
        source: PlaybackSource,
        capabilities: PlaybackCapabilities,
        quality: PlaybackQuality = PlaybackQuality.ORIGINAL
    ): PlaybackPath = decideDetailed(source, capabilities, quality).path

    fun decideDetailed(
        source: PlaybackSource,
        capabilities: PlaybackCapabilities,
        quality: PlaybackQuality = PlaybackQuality.ORIGINAL
    ): PlaybackDecision {
        val codecOk = source.videoCodec == null || capabilities.videoCodecs.isEmpty() ||
            normalizedVideoCodec(source.videoCodec) in capabilities.videoCodecs.map(::normalizedVideoCodec)
        val audioOk = source.audioCodec == null || capabilities.audioCodecs.isEmpty() ||
            normalizedAudioCodec(source.audioCodec) in capabilities.audioCodecs.map(::normalizedAudioCodec)
        val hdrOk = source.hdrFormat == null || !capabilities.hdrCapabilityKnown ||
            normalizedHdr(source.hdrFormat) in capabilities.hdrFormats.map(::normalizedHdr)
        val containerOk = source.container == null || capabilities.containers.isEmpty() ||
            normalizedContainer(source.container) in capabilities.containers.map(::normalizedContainer)
        val channelsOk = capabilities.maxAudioChannels == null || source.audioChannels == null ||
            source.audioChannels <= capabilities.maxAudioChannels
        val passthroughAudioOk = source.audioCodec == null ||
            normalizedAudioCodec(source.audioCodec) !in capabilities.audioPassthroughCodecs.map(::normalizedAudioCodec) ||
            capabilities.audioPassthrough
        val dimensionsOk = quality == PlaybackQuality.ORIGINAL || withinPreset(source, quality)
        val deviceLimitsOk = (capabilities.maxWidth == null || source.width == null || source.width <= capabilities.maxWidth) &&
            (capabilities.maxHeight == null || source.height == null || source.height <= capabilities.maxHeight) &&
            (capabilities.maxFrameRate == null || source.frameRate == null || source.frameRate <= capabilities.maxFrameRate)

        val directCompatible = codecOk && audioOk && hdrOk && containerOk &&
            channelsOk && passthroughAudioOk && deviceLimitsOk

        if (capabilities.directPlay && directCompatible && dimensionsOk && !source.subtitlesRequireTranscoding) {
            return PlaybackDecision(PlaybackPath.DIRECT_PLAY, "source and device support direct play")
        }

        // A requested quality is an explicit user constraint.  Do not bypass
        // it with direct stream/remux: the server must produce the selected
        // profile when the original source is above that preset.
        if (capabilities.directStream && directCompatible && dimensionsOk && !source.subtitlesRequireTranscoding) {
            return PlaybackDecision(PlaybackPath.DIRECT_STREAM, "container or stream negotiation is required")
        }

        if (capabilities.remux && directCompatible && dimensionsOk) {
            return PlaybackDecision(PlaybackPath.REMUX, "remux required for subtitles or container compatibility")
        }

        return if (capabilities.directStream || capabilities.directPlay) {
            PlaybackDecision(PlaybackPath.TRANSCODE, "source exceeds device or selected-quality capabilities")
        } else {
            PlaybackDecision(PlaybackPath.FALLBACK, "no compatible native playback path")
        }
    }

    private fun withinPreset(source: PlaybackSource, quality: PlaybackQuality): Boolean {
        val maxBitrate = when (quality) {
            PlaybackQuality.FOUR_K -> 40_000
            PlaybackQuality.FULL_HD_20 -> 20_000
            PlaybackQuality.FULL_HD_10 -> 10_000
            PlaybackQuality.HD_5 -> 5_000
            PlaybackQuality.SD_2 -> 2_000
            else -> Int.MAX_VALUE
        }
        val bitrateOk = source.bitrateKbps == null || source.bitrateKbps <= maxBitrate
        return when (quality) {
            PlaybackQuality.FOUR_K -> bitrateOk && withinDimensions(source, 3840, 2160)
            PlaybackQuality.FULL_HD_20, PlaybackQuality.FULL_HD_10 -> bitrateOk && withinDimensions(source, 1920, 1080)
            PlaybackQuality.HD_5 -> bitrateOk && withinDimensions(source, 1280, 720)
            PlaybackQuality.SD_2 -> bitrateOk && withinDimensions(source, 854, 480)
            else -> bitrateOk
        }
    }

    private fun withinDimensions(source: PlaybackSource, maxWidth: Int, maxHeight: Int): Boolean =
        (source.width == null || source.width <= maxWidth) &&
            (source.height == null || source.height <= maxHeight)

    /** Jellyfin and Android do not always spell equivalent codecs identically. */
    private fun normalizedVideoCodec(value: String): String = when (value.trim().lowercase()) {
        "h265", "x265" -> "hevc"
        "av01" -> "av1"
        "x264" -> "h264"
        else -> value.trim().lowercase()
    }

    private fun normalizedAudioCodec(value: String): String = when (value.trim().lowercase()) {
        "ec-3" -> "eac3"
        "dts-hd ma", "dtshd" -> "dts-hd"
        else -> value.trim().lowercase()
    }

    private fun normalizedHdr(value: String): String = when (value.trim().lowercase()) {
        "dv", "dolby vision", "dolby_vision" -> "dolby-vision"
        else -> value.trim().lowercase()
    }

    private fun normalizedContainer(value: String): String = when (value.trim().lowercase()) {
        "matroska" -> "mkv"
        "mpeg-ts", "mpegts", "mpeg transport stream" -> "ts"
        else -> value.trim().lowercase()
    }
}
