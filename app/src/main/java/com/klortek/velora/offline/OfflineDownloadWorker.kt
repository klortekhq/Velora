package com.klortek.velora.offline

import android.app.DownloadManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.klortek.velora.jellyfin.JellyfinConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** App-owned transfer worker. Credentials are read from the Keystore-backed config. */
class OfflineDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!com.klortek.velora.platform.PlatformCapabilities.supportsOfflineDownloads) return@withContext Result.failure()
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return@withContext Result.failure()
        val sourceId = inputData.getString(KEY_MEDIA_SOURCE_ID)
        val quality = OfflineDownloadQuality.fromStorageKey(inputData.getString(KEY_QUALITY) ?: OfflineDownloadQuality.ORIGINAL.storageKey)
        val workName = inputData.getString(KEY_WORK_NAME) ?: return@withContext Result.failure()
        val config = JellyfinConfig(applicationContext)
        if (config.serverUrl.isBlank() || config.accessToken.isBlank()) return@withContext Result.failure()

        val destinationRoot = File(applicationContext.filesDir, "offline/media").apply { mkdirs() }
        val temporary = File(destinationRoot, "${workName.hashCode()}.part")
        val destination = File(destinationRoot, "${workName.hashCode()}.media")
        val requestUrl = OfflineDownloadRequest.url(config.serverUrl, itemId, sourceId, quality)
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-Emby-Token", config.accessToken)
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            if (connection.responseCode == 401 || connection.responseCode == 403) return@withContext Result.failure()
            if (connection.responseCode !in 200..299) return@withContext Result.retry()
            val total = connection.contentLengthLong
            var copied = 0L
            setProgress(androidx.work.workDataOf(KEY_TOTAL_BYTES to total))
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        if (isStopped) return@withContext Result.failure()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        setProgress(androidx.work.workDataOf(KEY_BYTES to copied, KEY_TOTAL_BYTES to total))
                    }
                    output.fd.sync()
                }
            }
            val digest = java.io.FileInputStream(temporary).use { OfflineIntegrityVerifier.sha256(it) }
            if (destination.exists()) destination.delete()
            check(temporary.renameTo(destination))
            val existing = OfflineDownloadManager.load(applicationContext).firstOrNull { it.workName == workName }
            if (existing != null) {
                OfflineDownloadManager.persist(applicationContext, existing.copy(
                    status = DownloadManager.STATUS_SUCCESSFUL,
                    bytesDownloaded = copied,
                    totalBytes = total,
                    localPath = android.net.Uri.fromFile(destination).toString(),
                    checksumSha256 = digest
                ))
            }
            Result.success(androidx.work.workDataOf(KEY_LOCAL_PATH to android.net.Uri.fromFile(destination).toString(), KEY_BYTES to copied, KEY_TOTAL_BYTES to total))
        } catch (_: java.io.IOException) {
            temporary.delete()
            Result.retry()
        } catch (_: Exception) {
            temporary.delete()
            Result.failure()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val TAG = "velora-offline-download"
        const val KEY_ITEM_ID = "item_id"
        const val KEY_MEDIA_SOURCE_ID = "media_source_id"
        const val KEY_QUALITY = "quality"
        const val KEY_WORK_NAME = "work_name"
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
    }
}
