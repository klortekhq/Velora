package com.klortek.velora.security

import com.klortek.velora.jellyfin.ServerUrlValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlValidatorTest {
    @Test
    fun acceptsLocalAndRemoteHttpServers() {
        assertTrue(ServerUrlValidator.isValid("http://192.0.2.10:8096"))
        assertTrue(ServerUrlValidator.isValid("https://jellyfin.example.test/jellyfin"))
        assertTrue(ServerUrlValidator.isValid("HTTP://[::1]:8096"))
    }

    @Test
    fun rejectsCredentialsAndUrlParameters() {
        assertFalse(ServerUrlValidator.isValid("https://user:pass@jellyfin.example.test"))
        assertFalse(ServerUrlValidator.isValid("https://jellyfin.example.test?token=secret"))
        assertFalse(ServerUrlValidator.isValid("https://jellyfin.example.test/#home"))
    }

    @Test
    fun rejectsUnsupportedOrMalformedServers() {
        assertFalse(ServerUrlValidator.isValid("ftp://jellyfin.example.test"))
        assertFalse(ServerUrlValidator.isValid("https://"))
        assertFalse(ServerUrlValidator.isValid("https://jellyfin.example.test:0"))
        assertFalse(ServerUrlValidator.isValid("https://jellyfin.example.test:65536"))
        assertFalse(ServerUrlValidator.isValid("https://jellyfin.example.test/path with spaces"))
    }
}
