package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerDiscoveryTest {
    @Test
    fun localExplicitPortPrefersHttpBeforeTls() {
        val candidates = ServerDiscovery.buildUrlCandidates("192.168.100.201:8096")

        assertEquals("http://192.168.100.201:8096/System/Info/Public", candidates.first())
        assertTrue(candidates[1].startsWith("http://192.168.100.201:8096/"))
    }

    @Test
    fun fullHttpUrlRemainsFirstAndDoesNotDuplicateScheme() {
        val candidates = ServerDiscovery.buildUrlCandidates("http://192.168.100.201:8096/")

        assertEquals("http://192.168.100.201:8096/System/Info/Public", candidates.first())
        assertTrue(candidates.all { it.startsWith("http://") })
    }
}
