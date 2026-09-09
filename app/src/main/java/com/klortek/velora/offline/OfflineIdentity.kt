package com.klortek.velora.offline

/**
 * Account-scoped identity for an offline representation. Item IDs are only
 * unique inside a Jellyfin server, so the server and user must participate in
 * every durable key and WorkManager name.
 */
internal fun offlineAccountMatches(first: OfflineDownload, second: OfflineDownload): Boolean =
    first.serverUrl.orEmpty().removeSuffix("/") == second.serverUrl.orEmpty().removeSuffix("/") &&
        first.userId.orEmpty() == second.userId.orEmpty()

internal fun offlineEntryKey(entry: OfflineDownload): String = listOf(
    entry.serverUrl.orEmpty().removeSuffix("/"),
    entry.userId.orEmpty(),
    entry.itemId,
    entry.quality
).joinToString("\u001f")
