package com.klortek.velora.offline

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.klortek.velora.platform.PlatformCapabilities
import org.json.JSONArray
import java.io.File

data class OfflineDownload(
    val itemId: String,
    val name: String,
    val type: String,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val downloadId: Long,
    val localPath: String? = null,
    val status: Int = DownloadManager.STATUS_PENDING,
    val reason: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L
) {
    val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL && !localPath.isNullOrBlank() && File(localPath).exists()
    val progress: Int get() = if (totalBytes > 0L) ((bytesDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100) else 0
}

object OfflineDownloadManager {
    private const val PREFS = "velora_offline_downloads"
    private const val KEY_ENTRIES = "entries"

    private fun database(context: Context) = OfflineDatabase(context)

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
        episodeNumber: Int? = null
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
            delete(context, existing)
        }

        val base = serverUrl.trimEnd('/')
        val query = mediaSourceId?.let { "?mediaSourceId=${Uri.encode(it)}" } ?: ""
        val request = DownloadManager.Request(Uri.parse("$base/Items/$itemId/Download$query"))
            .addRequestHeader("X-Emby-Token", token)
            .setTitle(name)
            .setDescription(if (type == "Episode") "E${episodeNumber ?: ""} · Descarga de episodio" else "Descarga de película")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("video/*")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, "Velora/$itemId")

        val downloadId = context.getSystemService(DownloadManager::class.java).enqueue(request)
        val entry = OfflineDownload(itemId, name, type, seriesName, seasonNumber, episodeNumber, downloadId)
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
            entry.copy(status = status, reason = reason, bytesDownloaded = bytes, totalBytes = total, localPath = uri?.let { Uri.parse(it).path })
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
        entry.localPath?.let { File(it).delete() }
        deleteEntry(context, entry)
    }

    private fun deleteEntry(context: Context, entry: OfflineDownload) = save(context, load(context).filterNot { it.downloadId == entry.downloadId })

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
                add(OfflineDownload(item.optString("itemId"), item.optString("name"), item.optString("type"), item.optString("seriesName").ifBlank { null }, item.optInt("seasonNumber").takeIf { item.has("seasonNumber") }, item.optInt("episodeNumber").takeIf { item.has("episodeNumber") }, item.optLong("downloadId"), item.optString("localPath").ifBlank { null }, item.optInt("status"), item.optInt("reason"), item.optLong("bytesDownloaded"), item.optLong("totalBytes", -1L)))
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
