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
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val maxFrameRate: Double? = null
)

data class PlaybackSource(
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val hdrFormat: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Double? = null,
    val subtitlesRequireTranscoding: Boolean = false
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
    ): PlaybackPath {
        val codecOk = source.videoCodec == null || capabilities.videoCodecs.isEmpty() ||
            source.videoCodec.lowercase() in capabilities.videoCodecs.map(String::lowercase)
        val audioOk = source.audioCodec == null || capabilities.audioCodecs.isEmpty() ||
            source.audioCodec.lowercase() in capabilities.audioCodecs.map(String::lowercase)
        val hdrOk = source.hdrFormat == null || capabilities.hdrFormats.isEmpty() ||
            source.hdrFormat.lowercase() in capabilities.hdrFormats.map(String::lowercase)
        val dimensionsOk = quality == PlaybackQuality.ORIGINAL || withinPreset(source, quality)
        val deviceLimitsOk = (capabilities.maxWidth == null || source.width == null || source.width <= capabilities.maxWidth) &&
            (capabilities.maxHeight == null || source.height == null || source.height <= capabilities.maxHeight) &&
            (capabilities.maxFrameRate == null || source.frameRate == null || source.frameRate <= capabilities.maxFrameRate)

        if (capabilities.directPlay && codecOk && audioOk && hdrOk && dimensionsOk && deviceLimitsOk && !source.subtitlesRequireTranscoding) {
            return PlaybackPath.DIRECT_PLAY
        }
        if (capabilities.directStream && codecOk && audioOk && hdrOk && !source.subtitlesRequireTranscoding) {
            return PlaybackPath.DIRECT_STREAM
        }
        if (capabilities.remux && codecOk && audioOk) return PlaybackPath.REMUX
        return if (capabilities.directStream || capabilities.directPlay) PlaybackPath.TRANSCODE else PlaybackPath.FALLBACK
    }

    private fun withinPreset(source: PlaybackSource, quality: PlaybackQuality): Boolean {
        val max = when (quality) {
            PlaybackQuality.FOUR_K -> 40_000
            PlaybackQuality.FULL_HD_20 -> 20_000
            PlaybackQuality.FULL_HD_10 -> 10_000
            PlaybackQuality.HD_5 -> 5_000
            PlaybackQuality.SD_2 -> 2_000
            else -> Int.MAX_VALUE
        }
        // Bitrate is intentionally not part of PlaybackSource yet; dimensions
        // are the safe preset constraint until Jellyfin source metadata exposes it.
        return when (quality) {
            PlaybackQuality.FOUR_K -> (source.width ?: 0) <= 3840
            PlaybackQuality.FULL_HD_20, PlaybackQuality.FULL_HD_10 -> (source.width ?: 0) <= 1920
            PlaybackQuality.HD_5 -> (source.width ?: 0) <= 1280
            PlaybackQuality.SD_2 -> (source.width ?: 0) <= 854
            else -> max > 0
        }
    }
}
