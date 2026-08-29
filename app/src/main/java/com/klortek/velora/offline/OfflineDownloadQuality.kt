package com.klortek.velora.offline

/** Download profiles exposed to mobile and tablet users. */
enum class OfflineDownloadQuality(
    val storageKey: String,
    val label: String,
    val maxWidth: Int?,
    val maxHeight: Int?,
    val videoBitrate: Int?
) {
    ORIGINAL("original", "Original", null, null, null),
    HIGH("high", "Alta (1080p)", 1920, 1080, 20_000_000),
    MEDIUM("medium", "Media (720p)", 1280, 720, 5_000_000),
    LOW("low", "Baja (480p)", 854, 480, 2_000_000);

    companion object {
        fun fromStorageKey(value: String?): OfflineDownloadQuality =
            entries.firstOrNull { it.storageKey == value } ?: ORIGINAL
    }
}
