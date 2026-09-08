package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class JellyfinClientIdentityTest {
    @Test
    fun `device identity follows the build target`() {
        assertEquals("Android", veloraClientDeviceName(tvBuild = false))
        assertEquals("Android TV", veloraClientDeviceName(tvBuild = true))
    }
}
