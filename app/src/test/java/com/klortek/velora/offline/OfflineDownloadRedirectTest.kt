package com.klortek.velora.offline

import java.net.URL
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class OfflineDownloadRedirectTest {
    @Test
    fun acceptsSameOriginRedirectWithImplicitDefaultPort() {
        assertTrue(
            OfflineDownloadWorker.isSafeRedirect(
                URL("https://jellyfin.example/Items/a/Download"),
                URL("https://JELLYFIN.example/Items/a/Download?redirected=1")
            )
        )
    }

    @Test
    fun rejectsDifferentHostProtocolOrPort() {
        val original = URL("https://jellyfin.example/Items/a/Download")
        assertFalse(OfflineDownloadWorker.isSafeRedirect(original, URL("https://attacker.example/file")))
        assertFalse(OfflineDownloadWorker.isSafeRedirect(original, URL("http://jellyfin.example/file")))
        assertFalse(OfflineDownloadWorker.isSafeRedirect(original, URL("https://jellyfin.example:8443/file")))
    }
}
