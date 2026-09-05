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
    fun redactsCredentialsInsideErrorMessages() {
        val redacted = SensitiveDataRedactor.message(
            IllegalStateException("request failed: https://server.test/a?password=secret")
        )

        assertFalse(redacted.contains("secret"))
        assertEquals("request failed: https://server.test/a?password=<redacted>", redacted)
    }

    @Test
    fun nullAndLocalValuesUseSafeMarkers() {
        assertEquals("<null>", SensitiveDataRedactor.url(null))
        assertEquals("<null>", SensitiveDataRedactor.message(null))
        assertEquals("<local-path>", SensitiveDataRedactor.localPath("C:/private/video.mkv"))
        assertEquals("<local-file>", SensitiveDataRedactor.localFileName("video.mkv"))
    }
}
