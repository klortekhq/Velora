package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceModeTest {
    @Test
    fun `unknown or missing values fall back to automatic`() {
        assertEquals(PerformanceMode.AUTOMATIC, PerformanceMode.fromStorageKey(null))
        assertEquals(PerformanceMode.AUTOMATIC, PerformanceMode.fromStorageKey("legacy"))
    }

    @Test
    fun `preset keys round trip`() {
        PerformanceMode.values().forEach { mode ->
            assertEquals(mode, PerformanceMode.fromStorageKey(mode.storageKey))
        }
    }
}
