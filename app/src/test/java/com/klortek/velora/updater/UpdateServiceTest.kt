package com.klortek.velora.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateServiceTest {
    @Test
    fun semanticReleaseIsComparedAgainstInstalledVersionName() {
        assertTrue(UpdateService.updateAvailable(10300, 10371, "1.2.86"))
        assertFalse(UpdateService.updateAvailable(10300, 10372, "1.3.0"))
    }

    @Test
    fun releaseTagsParseToComparableVersionCodes() {
        assertTrue(UpdateService.parseVersion("v1.3.0") > UpdateService.parseVersion("1.2.86"))
    }
}
