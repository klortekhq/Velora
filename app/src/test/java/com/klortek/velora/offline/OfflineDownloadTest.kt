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
}
