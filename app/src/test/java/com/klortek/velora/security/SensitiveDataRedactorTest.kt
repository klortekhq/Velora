package com.klortek.velora.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SensitiveDataRedactorTest {
    @Test
    fun removesCredentialQueryParametersFromDiagnosticUrls() {
        val redacted = SensitiveDataRedactor.url(
            "https://server/Videos/42/master.m3u8?api_key=secret&mediaSourceId=source"
        )

        assertEquals(
            "https://server/Videos/42/master.m3u8?api_key=<redacted>&mediaSourceId=source",
            redacted
        )
        assertFalse(redacted.contains("secret"))
    }

    @Test
    fun removesQuickConnectSecretsFromDiagnosticUrls() {
        val redacted = SensitiveDataRedactor.url(
            "https://server/QuickConnect/Connect?secret=one-time-secret&next=1"
        )

        assertEquals(
            "https://server/QuickConnect/Connect?secret=<redacted>&next=1",
            redacted
        )
        assertFalse(redacted.contains("one-time-secret"))
    }
}
