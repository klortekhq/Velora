package com.klortek.velora.offline

/** Pure, deterministic rules for the opt-in Smart Downloads cleanup. */
object SmartDownloadPolicy {
    fun cleanupCandidates(
        entries: List<OfflineDownload>,
        removeWatched: Boolean,
        keepUnwatchedEpisodes: Int
    ): List<OfflineDownload> {
        if (!removeWatched) return emptyList()
        val watched = entries.filter { it.isComplete && !it.keepDownload && it.isWatched }
        val unwatchedEpisodes = entries
            .filter { it.isComplete && !it.keepDownload && !it.isWatched && it.type.equals("Episode", true) }
            .sortedWith(compareBy<OfflineDownload> { it.createdAtEpochMs }.thenBy { it.episodeNumber ?: Int.MIN_VALUE })
        return (watched + unwatchedEpisodes.drop(keepUnwatchedEpisodes.coerceAtLeast(0)))
            .distinctBy(::cleanupIdentity)
    }

    private fun cleanupIdentity(entry: OfflineDownload): String =
        entry.workName?.takeIf { it.isNotBlank() }
            ?: if (entry.downloadId > 0L) "download:${entry.downloadId}"
            else "item:${entry.itemId}:quality:${entry.quality}"
}
