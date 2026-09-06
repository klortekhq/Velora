package com.klortek.velora.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun selectsSignedApkForTheInstalledFormFactor() {
        val release = GitHubRelease(
            tagName = "v1.4.0",
            name = "Velora 1.4.0",
            body = null,
            assets = listOf(
                GitHubAsset("Velora-mobile-release-unsigned.apk", "https://example.invalid/mobile-unsigned.apk"),
                GitHubAsset("Velora-mobile-release.apk", "https://example.invalid/mobile.apk"),
                GitHubAsset("Velora-tv-release.apk", "https://example.invalid/tv.apk")
            )
        )

        assertEquals("Velora-mobile-release.apk", UpdateService.apkAssetFor(release, isTv = false)?.name)
        assertEquals("Velora-tv-release.apk", UpdateService.apkAssetFor(release, isTv = true)?.name)
    }

    @Test
    fun doesNotOfferUnsignedOnlyArtifactAsAnUpdate() {
        val release = GitHubRelease(
            tagName = "v1.4.0",
            name = "Velora 1.4.0",
            body = null,
            assets = listOf(GitHubAsset("Velora-mobile-release-unsigned.apk", "https://example.invalid/qa.apk"))
        )

        assertNull(UpdateService.apkAssetFor(release, isTv = false))
    }
}
