package com.klortek.velora.offline

/**
 * Device-independent storage gate for managed offline media.
 *
 * The policy deliberately works on numbers rather than Android filesystem
 * classes so queue decisions can be tested deterministically and reused by
 * future Apple implementations.
 */
data class OfflineStorageLimits(
    /** Null means no Velora-specific maximum; the system reserve still applies. */
    val maximumBytes: Long? = null,
    val minimumFreeBytes: Long = 512L * 1024L * 1024L,
    val safetyReserveBytes: Long = 256L * 1024L * 1024L
) {
    init {
        require(maximumBytes == null || maximumBytes >= 0L) { "maximumBytes must be non-negative" }
        require(minimumFreeBytes >= 0L) { "minimumFreeBytes must be non-negative" }
        require(safetyReserveBytes >= 0L) { "safetyReserveBytes must be non-negative" }
    }
}

data class OfflineStorageSnapshot(
    val availableBytes: Long,
    val managedBytes: Long
) {
    init {
        require(availableBytes >= 0L) { "availableBytes must be non-negative" }
        require(managedBytes >= 0L) { "managedBytes must be non-negative" }
    }
}

enum class OfflineStorageRejection {
    INVALID_SIZE,
    INSUFFICIENT_FREE_SPACE,
    OFFLINE_LIMIT_REACHED
}

data class OfflineStorageDecision(
    val allowed: Boolean,
    val rejection: OfflineStorageRejection? = null,
    val requiredFreeBytes: Long = 0L,
    val remainingOfflineBytes: Long? = null
)

object OfflineStoragePolicy {
    /**
     * Check one new download before it is enqueued.
     *
     * `temporaryBytes` covers a server/transcode temporary file when the
     * selected quality needs one. The safety reserve is always added, so the
     * caller cannot consume the last free bytes on the device.
     */
    fun evaluate(
        snapshot: OfflineStorageSnapshot,
        limits: OfflineStorageLimits,
        incomingBytes: Long,
        temporaryBytes: Long = 0L
    ): OfflineStorageDecision {
        if (incomingBytes < 0L || temporaryBytes < 0L) {
            return OfflineStorageDecision(
                allowed = false,
                rejection = OfflineStorageRejection.INVALID_SIZE
            )
        }

        val required = safeAdd(incomingBytes, temporaryBytes)
            ?.let { safeAdd(it, limits.safetyReserveBytes) }
        if (required == null) {
            return OfflineStorageDecision(
                allowed = false,
                rejection = OfflineStorageRejection.INVALID_SIZE
            )
        }

        val requiredFree = safeAdd(required, limits.minimumFreeBytes)
        if (requiredFree == null || snapshot.availableBytes < requiredFree) {
            return OfflineStorageDecision(
                allowed = false,
                rejection = OfflineStorageRejection.INSUFFICIENT_FREE_SPACE,
                requiredFreeBytes = requiredFree ?: Long.MAX_VALUE
            )
        }

        val remaining = limits.maximumBytes?.minus(snapshot.managedBytes)
        val requestedOfflineBytes = safeAdd(incomingBytes, temporaryBytes)
        if (requestedOfflineBytes == null) {
            return OfflineStorageDecision(
                allowed = false,
                rejection = OfflineStorageRejection.INVALID_SIZE,
                requiredFreeBytes = requiredFree,
                remainingOfflineBytes = remaining?.coerceAtLeast(0L)
            )
        }
        if (remaining != null && (remaining < 0L || requestedOfflineBytes > remaining)) {
            return OfflineStorageDecision(
                allowed = false,
                rejection = OfflineStorageRejection.OFFLINE_LIMIT_REACHED,
                requiredFreeBytes = requiredFree,
                remainingOfflineBytes = remaining.coerceAtLeast(0L)
            )
        }

        return OfflineStorageDecision(
            allowed = true,
            requiredFreeBytes = requiredFree,
            remainingOfflineBytes = remaining
        )
    }

    private fun safeAdd(first: Long, second: Long): Long? {
        return if (second > Long.MAX_VALUE - first) null else first + second
    }
}
