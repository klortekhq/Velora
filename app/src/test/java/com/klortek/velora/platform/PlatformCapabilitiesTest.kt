package com.klortek.velora.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformCapabilitiesTest {
    @Test
    fun offlineDownloadsOnlyExistOnMobileSurfaces() {
        assertTrue(supportsOfflineDownloads(PlatformSurface.MOBILE_TABLET))
        assertTrue(supportsOfflineDownloads(PlatformSurface.IOS_MOBILE))

        listOf(
            PlatformSurface.TV,
            PlatformSurface.BROWSER,
            PlatformSurface.TVOS,
            PlatformSurface.TIZEN,
            PlatformSurface.WEBOS,
            PlatformSurface.VIDAA
        ).forEach { surface ->
            assertFalse("Offline downloads must be hidden on $surface", supportsOfflineDownloads(surface))
        }
    }
}
