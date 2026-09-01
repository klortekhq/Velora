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
import java.security.MessageDigest

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
        val fileKey = stableFileKey(workName)
        val temporary = File(destinationRoot, "$fileKey.part")
        val destination = File(destinationRoot, "$fileKey.media")
        // v1.2.55 used Kotlin hashCode() for these paths. Adopt the stable
        // key without abandoning an interrupted transfer during migration.
        val legacyTemporary = File(destinationRoot, "${workName.hashCode()}.part")
        val legacyDestination = File(destinationRoot, "${workName.hashCode()}.media")
        if (!temporary.exists() && legacyTemporary.exists()) legacyTemporary.renameTo(temporary)
        if (!destination.exists() && legacyDestination.exists()) legacyDestination.renameTo(destination)
        val existingBytes = temporary.length().coerceAtLeast(0L)
        val requestUrl = OfflineDownloadRequest.url(config.serverUrl, itemId, sourceId, quality)
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-Emby-Token", config.accessToken)
            if (existingBytes > 0L) setRequestProperty("Range", "bytes=$existingBytes-")
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            if (connection.responseCode == 401 || connection.responseCode == 403) return@withContext Result.failure()
            if (connection.responseCode == 416) {
                temporary.delete()
                return@withContext Result.retry()
            }
            if (connection.responseCode !in 200..299) {
                return@withContext if (isRetryableResponse(connection.responseCode)) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            val resumed = existingBytes > 0L && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (!resumed && existingBytes > 0L) temporary.delete()
            val startingBytes = if (resumed) existingBytes else 0L
            val total = if (resumed) {
                connection.contentLengthLong.takeIf { it >= 0L }?.plus(startingBytes) ?: -1L
            } else connection.contentLengthLong
            var copied = startingBytes
            setProgress(androidx.work.workDataOf(KEY_TOTAL_BYTES to total))
            connection.inputStream.use { input ->
                FileOutputStream(temporary, resumed).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        // Keep the .part file intact when WorkManager stops
                        // this attempt for a temporary lifecycle/constraint
                        // change; the next run can resume with Range.
                        if (isStopped) return@withContext Result.retry()
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
                    checksumSha256 = digest,
                    completedAtEpochMs = System.currentTimeMillis()
                ))
            }
            Result.success(androidx.work.workDataOf(KEY_LOCAL_PATH to android.net.Uri.fromFile(destination).toString(), KEY_BYTES to copied, KEY_TOTAL_BYTES to total))
        } catch (_: java.io.IOException) {
            // Keep the verified prefix. WorkManager's retry will send a Range
            // request and continue instead of starting the transfer again.
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
        internal fun isRetryableResponse(code: Int): Boolean = code == 408 || code == 429 || code >= 500
    }

    private fun stableFileKey(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
        .take(32)

}
