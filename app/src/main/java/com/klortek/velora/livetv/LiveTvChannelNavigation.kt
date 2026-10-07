package com.klortek.velora.livetv

/**
 * Returns the adjacent channel while preserving the order supplied by
 * Jellyfin. Wrapping at either end makes remote-control channel surfing
 * predictable and avoids a dead end after the last channel.
 */
fun adjacentLiveTvChannelId(
    channelIds: List<String>,
    currentId: String,
    next: Boolean
): String? {
    if (channelIds.isEmpty()) return null
    val currentIndex = channelIds.indexOf(currentId)
    if (currentIndex < 0) return null
    val offset = if (next) 1 else -1
    return channelIds[(currentIndex + offset + channelIds.size) % channelIds.size]
}
