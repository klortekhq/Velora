package com.klortek.velora.offline

import android.app.DownloadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadTest {
    @Test
    fun successfulContentUriIsPlayableWithoutFilesystemPath() {
        val download = OfflineDownload(
            itemId = "movie-1",
            name = "Película",
            type = "Movie",
            downloadId = 1L,
            localPath = "content://downloads/my_downloads/1",
            status = DownloadManager.STATUS_SUCCESSFUL
        )

        assertTrue(download.isComplete)
    }

    @Test
    fun incompleteOrUriMissingDownloadIsNotPlayable() {
        val pending = OfflineDownload(
            itemId = "movie-1",
            name = "Película",
            type = "Movie",
            downloadId = 1L,
            localPath = "content://downloads/my_downloads/1",
            status = DownloadManager.STATUS_RUNNING
        )
        val missingUri = pending.copy(status = DownloadManager.STATUS_SUCCESSFUL, localPath = null)

        assertFalse(pending.isComplete)
        assertFalse(missingUri.isComplete)
    }

    @Test
    fun qualityProfilesUseOriginalOrExplicitTranscodeEndpoint() {
        val original = OfflineDownloadRequest.url("http://server:8096/", "movie/1", "source 1", OfflineDownloadQuality.ORIGINAL)
        val medium = OfflineDownloadRequest.url("http://server:8096/", "movie/1", "source 1", OfflineDownloadQuality.MEDIUM)

        assertTrue(original.contains("/Items/movie%2F1/Download?mediaSourceId=source%201"))
        assertTrue(medium.contains("/Videos/movie%2F1/stream.mp4?"))
        assertTrue(medium.contains("MaxWidth=1280"))
        assertTrue(medium.contains("MaxHeight=720"))
        assertTrue(medium.contains("VideoBitrate=5000000"))
        assertFalse(original.contains("api_key"))
        assertFalse(medium.contains("api_key"))
    }

    @Test
    fun playbackTimestampIsIndependentFromDownloadCompletion() {
        val entry = OfflineDownload(
            itemId = "episode-1",
            name = "Episodio",
            type = "Episode",
            downloadId = 2L,
            createdAtEpochMs = 100L,
            completedAtEpochMs = 200L
        )

        val played = entry.copy(lastPlayedAtEpochMs = 300L)

        assertTrue(played.createdAtEpochMs == 100L)
        assertTrue(played.completedAtEpochMs == 200L)
        assertTrue(played.lastPlayedAtEpochMs == 300L)
    }

    @Test
    fun smartCleanupNeverRemovesProtectedDownloads() {
        val watched = OfflineDownload("movie-1", "Vista", "Movie", downloadId = 1L,
            status = DownloadManager.STATUS_SUCCESSFUL, localPath = "file:///movie", isWatched = true)
        val protected = watched.copy(itemId = "movie-2", downloadId = 2L, keepDownload = true)
        val episode = OfflineDownload("episode-1", "Episodio", "Episode", downloadId = 3L,
            status = DownloadManager.STATUS_SUCCESSFUL, localPath = "file:///episode", createdAtEpochMs = 1L)

        val candidates = SmartDownloadPolicy.cleanupCandidates(listOf(watched, protected, episode), true, 1)

        assertTrue(candidates.map { it.downloadId }.contains(1L))
        assertFalse(candidates.map { it.downloadId }.contains(2L))
        assertFalse(candidates.map { it.downloadId }.contains(3L))
    }

    @Test
    fun managedDownloadsWithZeroProviderIdRemainDistinct() {
        val first = OfflineDownload("episode-1", "E1", "Episode", downloadId = 0L, workName = "offline-one")
        val second = OfflineDownload("episode-2", "E2", "Episode", downloadId = 0L, workName = "offline-two")

        assertFalse(sameOfflineEntry(first, second))
        assertTrue(sameOfflineEntry(first, first.copy(lastPlayedAtEpochMs = 10L)))
    }

    @Test
    fun smartCleanupKeepsDistinctManagedWatchedEntries() {
        val first = OfflineDownload(
            "episode-1", "E1", "Episode", downloadId = 0L,
            workName = "offline-one", status = DownloadManager.STATUS_SUCCESSFUL,
            localPath = "file:///episode-1", isWatched = true
        )
        val second = first.copy(
            itemId = "episode-2", name = "E2", workName = "offline-two",
            localPath = "file:///episode-2"
        )

        val candidates = SmartDownloadPolicy.cleanupCandidates(listOf(first, second), true, 2)

        assertEquals(2, candidates.size)
        assertEquals(setOf("offline-one", "offline-two"), candidates.mapNotNull { it.workName }.toSet())
    }
}
