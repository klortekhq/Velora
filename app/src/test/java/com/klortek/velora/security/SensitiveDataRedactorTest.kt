package com.klortek.velora.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataRedactorTest {
    @Test
    fun redactsCredentialQueryParametersRegardlessOfCase() {
        val redacted = SensitiveDataRedactor.url(
            "https://jellyfin.test/Videos/1?Access_Token=secret&quality=1080"
        )

        assertFalse(redacted.contains("secret"))
        assertTrue(redacted.contains("Access_Token=<redacted>"))
        assertTrue(redacted.contains("quality=1080"))
    }

    @Test
    fun redactsAuthorizationStyleQueryParameters() {
        val redacted = SensitiveDataRedactor.url(
            "https://jellyfin.test/video.m3u8?X-Emby-Token=secret&quality=1080"
        )

        assertFalse(redacted.contains("secret"))
        assertTrue(redacted.contains("X-Emby-Token=<redacted>"))
    }

    @Test
    fun redactsCredentialsInUrlUserInfoAndFragments() {
        val redacted = SensitiveDataRedactor.url(
            "https://user:password@jellyfin.test/video.m3u8#access_token=fragment-secret"
        )

        assertFalse(redacted.contains("user:password"))
        assertFalse(redacted.contains("fragment-secret"))
        assertTrue(redacted.startsWith("https://<redacted>@jellyfin.test"))
        assertTrue(redacted.contains("#access_token=<redacted>"))
    }

    @Test
    fun redactsCredentialsInsideErrorMessages() {
        val redacted = SensitiveDataRedactor.message(
            IllegalStateException("request failed: https://server.test/a?password=secret")
        )

        assertFalse(redacted.contains("secret"))
        assertEquals("request failed: https://server.test/a?password=<redacted>", redacted)
    }

    @Test
    fun redactsTokensInsideAuthorizationHeaders() {
        val redacted = SensitiveDataRedactor.message(
            IllegalStateException(
                "request failed: X-Emby-Authorization: MediaBrowser Token=\"header-secret\""
            )
        )

        assertFalse(redacted.contains("header-secret"))
        assertTrue(redacted.contains("Token=\"<redacted>\""))
    }

    @Test
    fun redactsBearerTokensAndPasswordsInErrorText() {
        val redacted = SensitiveDataRedactor.message(
            IllegalStateException("Authorization: Bearer bearer-secret; password='plain-secret'")
        )

        assertFalse(redacted.contains("bearer-secret"))
        assertFalse(redacted.contains("plain-secret"))
        assertTrue(redacted.contains("Authorization: Bearer <redacted>"))
        assertTrue(redacted.contains("password='<redacted>'"))
    }

    @Test
    fun nullAndLocalValuesUseSafeMarkers() {
        assertEquals("<null>", SensitiveDataRedactor.url(null))
        assertEquals("<null>", SensitiveDataRedactor.message(null))
        assertEquals("<local-path>", SensitiveDataRedactor.localPath("C:/private/video.mkv"))
        assertEquals("<local-file>", SensitiveDataRedactor.localFileName("video.mkv"))
    }
}
