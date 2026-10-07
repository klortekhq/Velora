package com.klortek.velora.trailer

import com.klortek.velora.tmdb.TmdbVideo

/**
 * Chooses a legitimate trailer without making movie and series screens use
 * subtly different policies. YouTube is accepted only when it is explicitly
 * represented by the TMDB video record; playback remains inside Velora.
 */
object TrailerResolver {
    fun select(videos: List<TmdbVideo>, preferredLanguage: String? = null): TmdbVideo? {
        val youtube = videos.filter { it.site.equals("YouTube", ignoreCase = true) }
        val language = preferredLanguage?.lowercase()?.takeIf { it.isNotBlank() }
        return youtube.firstOrNull { isTrailer(it) && it.official && matchesLanguage(it, language) }
            ?: youtube.firstOrNull { isTrailer(it) && it.official }
            ?: youtube.firstOrNull { isTrailer(it) }
            ?: youtube.firstOrNull()
    }

    private fun isTrailer(video: TmdbVideo): Boolean =
        video.type.equals("Trailer", ignoreCase = true)

    private fun matchesLanguage(video: TmdbVideo, language: String?): Boolean =
        language == null || video.iso6391?.lowercase() == language
}
