package com.klortek.velora.trailer

import com.klortek.velora.jellyfin.JellyfinItem

/** Canonical server-first trailer policy shared by movie and series details. */
object JellyfinTrailerResolver {
    fun select(local: List<JellyfinItem>, remote: List<JellyfinItem>): JellyfinItem? =
        local.firstOrNull() ?: remote.firstOrNull()
}
