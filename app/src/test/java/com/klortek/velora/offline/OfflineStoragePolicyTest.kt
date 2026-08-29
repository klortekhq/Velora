package com.klortek.velora.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineStoragePolicyTest {
    @Test
    fun reservesMinimumFreeSpaceAndTemporaryBytes() {
        val limits = OfflineStorageLimits(
            maximumBytes = null,
            minimumFreeBytes = 100L,
            safetyReserveBytes = 25L
        )

        val allowed = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = 225L, managedBytes = 0L),
            limits = limits,
            incomingBytes = 100L,
            temporaryBytes = 0L
        )
        val rejected = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = 224L, managedBytes = 0L),
            limits = limits,
            incomingBytes = 100L,
            temporaryBytes = 0L
        )

        assertTrue(allowed.allowed)
        assertEquals(225L, allowed.requiredFreeBytes)
        assertFalse(rejected.allowed)
        assertEquals(OfflineStorageRejection.INSUFFICIENT_FREE_SPACE, rejected.rejection)
    }

    @Test
    fun enforcesVeloraOfflineLimitAgainstManagedAndTemporaryBytes() {
        val decision = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = 10_000L, managedBytes = 900L),
            limits = OfflineStorageLimits(maximumBytes = 1_000L, minimumFreeBytes = 0L, safetyReserveBytes = 0L),
            incomingBytes = 50L,
            temporaryBytes = 51L
        )

        assertFalse(decision.allowed)
        assertEquals(OfflineStorageRejection.OFFLINE_LIMIT_REACHED, decision.rejection)
        assertEquals(100L, decision.remainingOfflineBytes)
    }

    @Test
    fun rejectsNegativeSizesWithoutOverflowingTheBudget() {
        val decision = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = Long.MAX_VALUE, managedBytes = 0L),
            limits = OfflineStorageLimits(minimumFreeBytes = 0L, safetyReserveBytes = 0L),
            incomingBytes = -1L
        )

        assertFalse(decision.allowed)
        assertEquals(OfflineStorageRejection.INVALID_SIZE, decision.rejection)
    }

    @Test
    fun rejectsPositiveSizeOverflowWithoutAllowingTheDownload() {
        val decision = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = Long.MAX_VALUE, managedBytes = 0L),
            limits = OfflineStorageLimits(minimumFreeBytes = 0L, safetyReserveBytes = 0L),
            incomingBytes = Long.MAX_VALUE,
            temporaryBytes = 1L
        )

        assertFalse(decision.allowed)
        assertEquals(OfflineStorageRejection.INVALID_SIZE, decision.rejection)
    }
}
