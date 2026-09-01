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
            source.videoCodec.lowercase() in capabilities.videoCodecs.map(String::lowercase)
        val audioOk = source.audioCodec == null || capabilities.audioCodecs.isEmpty() ||
            source.audioCodec.lowercase() in capabilities.audioCodecs.map(String::lowercase)
        val hdrOk = source.hdrFormat == null || capabilities.hdrFormats.isEmpty() ||
            source.hdrFormat.lowercase() in capabilities.hdrFormats.map(String::lowercase)
        val containerOk = source.container == null || capabilities.containers.isEmpty() ||
            source.container.lowercase() in capabilities.containers.map(String::lowercase)
        val channelsOk = capabilities.maxAudioChannels == null || source.audioChannels == null ||
            source.audioChannels <= capabilities.maxAudioChannels
        val passthroughAudioOk = source.audioCodec == null ||
            source.audioCodec.lowercase() !in capabilities.audioPassthroughCodecs.map(String::lowercase) ||
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
            PlaybackQuality.FOUR_K -> bitrateOk && (source.width ?: 0) <= 3840
            PlaybackQuality.FULL_HD_20, PlaybackQuality.FULL_HD_10 -> bitrateOk && (source.width ?: 0) <= 1920
            PlaybackQuality.HD_5 -> bitrateOk && (source.width ?: 0) <= 1280
            PlaybackQuality.SD_2 -> bitrateOk && (source.width ?: 0) <= 854
            else -> bitrateOk
        }
    }
}
