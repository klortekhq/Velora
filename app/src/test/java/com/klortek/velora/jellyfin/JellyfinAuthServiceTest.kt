package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinAuthServiceTest {
    @Test
    fun `Jellyfin 400 is presented as invalid credentials`() {
        assertEquals(
            AuthenticationFailure.INVALID_CREDENTIALS,
            authenticationFailureForHttpStatus(400)
        )
    }

    @Test
    fun `authorization failures remain invalid credentials`() {
        assertEquals(AuthenticationFailure.INVALID_CREDENTIALS, authenticationFailureForHttpStatus(401))
        assertEquals(AuthenticationFailure.INVALID_CREDENTIALS, authenticationFailureForHttpStatus(403))
    }

    @Test
    fun `other HTTP failures remain server errors`() {
        assertEquals(AuthenticationFailure.SERVER_ERROR, authenticationFailureForHttpStatus(500))
    }

    @Test
    fun `Jellyfin 12 authorization carries complete client identity`() {
        val header = veloraMediaBrowserAuthorization(
            accessToken = "secret-token",
            deviceName = "Android TV",
            deviceId = "stable-device"
        )

        assertTrue(header.startsWith("MediaBrowser Client=\"Velora\""))
        assertTrue(header.contains("Device=\"Android TV\""))
        assertTrue(header.contains("DeviceId=\"stable-device\""))
        assertTrue(header.contains("Version=\"${com.klortek.velora.BuildConfig.VERSION_NAME}\""))
        assertTrue(header.contains("Token=\"secret-token\""))
        assertFalse(header.contains("X-Emby-"))
    }
}
