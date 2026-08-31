package com.klortek.velora.offline

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.klortek.velora.jellyfin.AppSettings
import com.klortek.velora.platform.PlatformCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream
import java.io.File

data class OfflineDownload(
    val itemId: String,
    val name: String,
    val type: String,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val downloadId: Long,
    val quality: String = OfflineDownloadQuality.ORIGINAL.storageKey,
    val localPath: String? = null,
    val status: Int = DownloadManager.STATUS_PENDING,
    val reason: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    /** SHA-256 of the managed media, calculated on first offline playback. */
    val checksumSha256: String? = null
) {
    /** Provider-neutral state used by UI and future managed-transfer engines. */
    val state: OfflineDownloadState get() = offlineDownloadState(status, reason)

    /**
     * DownloadManager may return either a file:// URI or a provider-backed
     * content:// URI. The latter has no meaningful filesystem path, so using
     * File(path).exists() incorrectly hid completed downloads and prevented
     * playback. A successful DownloadManager row with a persisted local URI is
     * the authoritative availability signal; DownloadManager owns the file.
     */
    val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL && !localPath.isNullOrBlank()
    val progress: Int get() = if (totalBytes > 0L) ((bytesDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100) else 0
}

object OfflineDownloadManager {
    private const val PREFS = "velora_offline_downloads"
    private const val KEY_ENTRIES = "entries"

    private fun database(context: Context) = OfflineDatabase(context)

    class StorageRejectedException(val decision: OfflineStorageDecision) :
        IllegalStateException("Offline download rejected: ${decision.rejection}")

    fun enqueue(
        context: Context,
        serverUrl: String,
        token: String,
        itemId: String,
        name: String,
        type: String,
        mediaSourceId: String? = null,
        seriesName: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        quality: OfflineDownloadQuality = OfflineDownloadQuality.ORIGINAL,
        estimatedBytes: Long? = null
    ): OfflineDownload {
        check(PlatformCapabilities.supportsOfflineDownloads) {
            "Offline downloads are only supported on mobile and tablet builds"
        }
        val existing = refresh(context).firstOrNull { it.itemId == itemId }
        if (existing != null) {
            if (existing.isComplete || existing.status == DownloadManager.STATUS_PENDING ||
                existing.status == DownloadManager.STATUS_RUNNING) {
                return existing
            }
        }

        // Evaluate before removing a previous failed entry, so a rejected
        // replacement never destroys the user's existing offline state.
        val managedBytes = load(context)
            .filterNot { it.itemId == itemId }
            .sumOf { entry ->
                if (entry.totalBytes > 0L) entry.totalBytes else entry.bytesDownloaded.coerceAtLeast(0L)
            }
        val storageRoot = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val availableBytes = storageRoot?.let { StatFs(it.path).availableBytes } ?: 0L
        val maxBytes = AppSettings(context).offlineMaxStorageBytes.takeIf { it > 0L }
        val decision = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = availableBytes, managedBytes = managedBytes),
            limits = OfflineStorageLimits(maximumBytes = maxBytes),
            incomingBytes = estimatedBytes ?: 0L
        )
        if (!decision.allowed) throw StorageRejectedException(decision)
        if (existing != null) delete(context, existing)

        val request = DownloadManager.Request(Uri.parse(OfflineDownloadRequest.url(serverUrl, itemId, mediaSourceId, quality)))
            .addRequestHeader("X-Emby-Token", token)
            .setTitle(name)
            .setDescription(if (type == "Episode") "E${episodeNumber ?: ""} · ${quality.label}" else quality.label)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("video/*")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, "Velora/$itemId")

        val downloadId = context.getSystemService(DownloadManager::class.java).enqueue(request)
        val entry = OfflineDownload(itemId, name, type, seriesName, seasonNumber, episodeNumber, downloadId, quality.storageKey)
        save(context, load(context).filterNot { it.itemId == itemId } + entry)
        return entry
    }

    fun refresh(context: Context): List<OfflineDownload> {
        val manager = context.getSystemService(DownloadManager::class.java)
        val updated = load(context).mapNotNull { entry ->
            val cursor = runCatching { manager.query(DownloadManager.Query().setFilterById(entry.downloadId)) }.getOrNull()
            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                return@mapNotNull if (entry.isComplete) entry else null
            }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            cursor.close()
            entry.copy(
                status = status,
                reason = reason,
                bytesDownloaded = bytes,
                totalBytes = total,
                // Keep the complete URI. A content:// URI cannot safely be
                // converted to a filesystem path and ExoPlayer can consume it
                // directly through the Android content resolver.
                localPath = uri?.takeIf { it.isNotBlank() }
            )
        }
        save(context, updated)
        return updated
    }

    fun cancel(context: Context, entry: OfflineDownload) {
        context.getSystemService(DownloadManager::class.java).remove(entry.downloadId)
        deleteEntry(context, entry)
    }

    fun delete(context: Context, entry: OfflineDownload) {
        context.getSystemService(DownloadManager::class.java).remove(entry.downloadId)
        entry.localPath?.let { deleteLocalUri(context, it) }
        deleteEntry(context, entry)
    }

    /** Verify local media before playback, establishing a digest on first use. */
    suspend fun verifyIntegrity(context: Context, entry: OfflineDownload): Boolean = withContext(Dispatchers.IO) {
        val value = entry.localPath ?: return@withContext false
        val input = openLocalStream(context, value) ?: return@withContext false
        input.use {
            val actual = OfflineIntegrityVerifier.sha256(it)
            val expected = entry.checksumSha256
            if (expected != null && !expected.equals(actual, ignoreCase = true)) {
                return@withContext false
            }
            if (expected == null) {
                save(context, load(context).map { current ->
                    if (current.downloadId == entry.downloadId) current.copy(checksumSha256 = actual) else current
                })
            }
            true
        }
    }

    /**
     * DownloadManager can expose provider-backed `content://` URIs. Treating
     * those as filesystem paths silently leaves the provider-owned media
     * behind, so deletion must go through ContentResolver. File URIs and the
     * legacy plain paths remain supported for migrated installations.
     */
    private fun deleteLocalUri(context: Context, value: String) {
        val uri = runCatching { Uri.parse(value) }.getOrNull()
        when (uri?.scheme?.lowercase()) {
            "content" -> runCatching { context.contentResolver.delete(uri, null, null) }
            "file" -> uri.path?.let { runCatching { File(it).delete() } }
            else -> runCatching { File(value).delete() }
        }
    }

    private fun deleteEntry(context: Context, entry: OfflineDownload) = save(context, load(context).filterNot { it.downloadId == entry.downloadId })

    private fun openLocalStream(context: Context, value: String): InputStream? {
        val uri = runCatching { Uri.parse(value) }.getOrNull()
        return when (uri?.scheme?.lowercase()) {
            "content" -> runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            "file" -> uri.path?.let { runCatching { File(it).inputStream() }.getOrNull() }
            else -> runCatching { File(value).inputStream() }.getOrNull()
        }
    }

    fun load(context: Context): List<OfflineDownload> {
        val db = database(context)
        val current = db.readAll()
        if (current.isNotEmpty()) {
            db.close()
            return current
        }

        // One-time migration for installations that used the original JSON index.
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ENTRIES, "[]") ?: "[]"
        val json = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val migrated = buildList {
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                add(OfflineDownload(item.optString("itemId"), item.optString("name"), item.optString("type"), item.optString("seriesName").ifBlank { null }, item.optInt("seasonNumber").takeIf { item.has("seasonNumber") }, item.optInt("episodeNumber").takeIf { item.has("episodeNumber") }, item.optLong("downloadId"), item.optString("quality", OfflineDownloadQuality.ORIGINAL.storageKey), item.optString("localPath").ifBlank { null }, item.optInt("status"), item.optInt("reason"), item.optLong("bytesDownloaded"), item.optLong("totalBytes", -1L)))
            }
        }
        if (migrated.isNotEmpty()) db.replaceAll(migrated)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).apply()
        db.close()
        return migrated
    }

    private fun save(context: Context, entries: List<OfflineDownload>) {
        val db = database(context)
        db.replaceAll(entries)
        db.close()
        // The preference is intentionally removed once the SQLite index is live.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).apply()
    }
}
