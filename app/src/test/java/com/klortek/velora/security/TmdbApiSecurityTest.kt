package com.klortek.velora.security

import com.klortek.velora.tmdb.TmdbApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TmdbApiSecurityTest {
    @Test
    fun tmdbApiKeyIsRedactedFromDiagnosticUrl() {
        val redacted = TmdbApiService.redactApiKey(
            "https://api.themoviedb.org/3/movie/42/videos?api_key=secret-value&language=es"
        )

        assertEquals(
            "https://api.themoviedb.org/3/movie/42/videos?api_key=<redacted>&language=es",
            redacted
        )
        assertFalse(redacted.contains("secret-value"))
    }
}
