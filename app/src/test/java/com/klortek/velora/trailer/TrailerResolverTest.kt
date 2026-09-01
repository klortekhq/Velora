package com.klortek.velora.trailer

import com.klortek.velora.tmdb.TmdbVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrailerResolverTest {
    @Test
    fun prefersOfficialTrailerInRequestedLanguage() {
        val videos = listOf(
            video("clip", type = "Clip", official = true, language = "es"),
            video("official-en", official = true, language = "en"),
            video("official-es", official = true, language = "es")
        )
        assertEquals("official-es", TrailerResolver.select(videos, "es")?.key)
    }

    @Test
    fun fallsBackToOfficialTrailerThenAnyTrailer() {
        val official = video("official", official = true, language = "en")
        val trailer = video("trailer", language = "es")
        assertEquals("official", TrailerResolver.select(listOf(trailer, official), "de")?.key)
        assertEquals("trailer", TrailerResolver.select(listOf(trailer), "de")?.key)
    }

    @Test
    fun rejectsNonYoutubeAndReturnsNullWhenNoVideoExists() {
        assertNull(TrailerResolver.select(listOf(video("vimeo", site = "Vimeo"))))
        assertNull(TrailerResolver.select(emptyList()))
    }

    private fun video(
        key: String,
        site: String = "YouTube",
        type: String = "Trailer",
        official: Boolean = false,
        language: String? = null
    ) = TmdbVideo("id-$key", language, null, key, key, site, 1080, type, official, null)
}
