package com.klortek.velora.offline

import android.app.DownloadManager
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
}
