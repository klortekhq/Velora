package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
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
}
