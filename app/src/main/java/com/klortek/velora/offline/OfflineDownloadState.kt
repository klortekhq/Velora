package com.klortek.velora.offline

import android.app.DownloadManager

/**
 * Domain-level state for an offline item.
 *
 * DownloadManager remains the Android transfer provider for now, but the rest
 * of Velora should not have to understand its integer status/reason values.
 * Keeping this mapping in one place also makes the later WorkManager-backed
 * engine a provider replacement instead of a UI rewrite.
 */
enum class OfflineDownloadState {
    QUEUED,
    WAITING_FOR_NETWORK,
    WAITING_FOR_STORAGE,
    DOWNLOADING,
    VERIFYING,
    COMPLETED,
    PAUSED,
    FAILED,
    CANCELLED,
    DELETING
}

internal fun offlineDownloadState(status: Int, reason: Int): OfflineDownloadState = when (status) {
    DownloadManager.STATUS_PENDING -> {
        when (reason) {
            DownloadManager.PAUSED_WAITING_FOR_NETWORK,
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> OfflineDownloadState.WAITING_FOR_NETWORK
            DownloadManager.PAUSED_WAITING_TO_RETRY -> OfflineDownloadState.PAUSED
            else -> OfflineDownloadState.QUEUED
        }
    }
    DownloadManager.STATUS_RUNNING -> OfflineDownloadState.DOWNLOADING
    DownloadManager.STATUS_PAUSED -> when (reason) {
        DownloadManager.PAUSED_WAITING_FOR_NETWORK,
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> OfflineDownloadState.WAITING_FOR_NETWORK
        else -> OfflineDownloadState.PAUSED
    }
    DownloadManager.STATUS_SUCCESSFUL -> OfflineDownloadState.COMPLETED
    DownloadManager.STATUS_FAILED -> OfflineDownloadState.FAILED
    else -> OfflineDownloadState.FAILED
}
