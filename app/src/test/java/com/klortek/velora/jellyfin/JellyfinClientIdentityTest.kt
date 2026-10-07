package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class JellyfinClientIdentityTest {
    @Test
    fun `device identity follows the build target`() {
        assertEquals("Android", veloraClientDeviceName(tvBuild = false))
        assertEquals("Android TV", veloraClientDeviceName(tvBuild = true))
    }

    @Test
    fun `missing device id gets a stable fallback`() {
        assertEquals("velora-android", veloraClientDeviceId(null))
        assertEquals("velora-android", veloraClientDeviceId("  "))
        assertEquals("device-123", veloraClientDeviceId("device-123"))
    }
}
