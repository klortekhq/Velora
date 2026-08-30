package com.klortek.velora.offline

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineIntegrityVerifierTest {
    @Test
    fun calculatesKnownSha256WithoutClosingCallerStream() {
        val input = ByteArrayInputStream("Velora offline media".toByteArray())

        val hash = OfflineIntegrityVerifier.sha256(input)

        assertEquals("ed76dd56ce204c19057912ba0d9ce2943fbd25cfc239eb169bd06850ac74bb2d", hash)
        assertEquals(0, input.available())
    }

    @Test
    fun matchesIsCaseInsensitiveAndRejectsDifferentDigest() {
        val expected = "ed76dd56ce204c19057912ba0d9ce2943fbd25cfc239eb169bd06850ac74bb2d"

        assertTrue(OfflineIntegrityVerifier.matches(ByteArrayInputStream("Velora offline media".toByteArray()), expected.uppercase()))
        assertFalse(OfflineIntegrityVerifier.matches(ByteArrayInputStream("Velora offline media".toByteArray()), "0".repeat(64)))
    }
}
