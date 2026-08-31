package com.klortek.velora.offline

import android.app.DownloadManager
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadStateTest {
    @Test
    fun mapsProviderStatusesToDomainStates() {
        assertEquals(OfflineDownloadState.QUEUED, offlineDownloadState(DownloadManager.STATUS_PENDING, 0))
        assertEquals(OfflineDownloadState.DOWNLOADING, offlineDownloadState(DownloadManager.STATUS_RUNNING, 0))
        assertEquals(OfflineDownloadState.WAITING_FOR_NETWORK, offlineDownloadState(
            DownloadManager.STATUS_PAUSED,
            DownloadManager.PAUSED_WAITING_FOR_NETWORK
        ))
        assertEquals(OfflineDownloadState.PAUSED, offlineDownloadState(
            DownloadManager.STATUS_PAUSED,
            DownloadManager.PAUSED_WAITING_TO_RETRY
        ))
        assertEquals(OfflineDownloadState.COMPLETED, offlineDownloadState(DownloadManager.STATUS_SUCCESSFUL, 0))
        assertEquals(OfflineDownloadState.FAILED, offlineDownloadState(DownloadManager.STATUS_FAILED, 0))
    }
}
