package com.klortek.velora.jellyfin

/** Stable, named performance presets shared by Android phone, tablet and TV. */
enum class PerformanceMode(val storageKey: String) {
    AUTOMATIC("automatic"),
    QUALITY("quality"),
    BALANCED("balanced"),
    PERFORMANCE("performance");

    companion object {
        fun fromStorageKey(value: String?): PerformanceMode =
            values().firstOrNull { it.storageKey == value } ?: AUTOMATIC
    }
}
