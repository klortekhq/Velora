package com.klortek.velora.offline

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.StatFs
import com.klortek.velora.jellyfin.AppSettings
import com.klortek.velora.platform.PlatformCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.File
import java.security.MessageDigest

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
    val speedBytesPerSecond: Long = 0L,
    val etaSeconds: Long? = null,
    /** SHA-256 of the managed media, calculated on first offline playback. */
    val checksumSha256: String? = null,
    /** Unique WorkManager name for app-managed transfers; null means legacy provider. */
    val workName: String? = null,
    /** Lifecycle timestamps are persisted for cleanup and smart-download policies. */
    val createdAtEpochMs: Long = 0L,
    val completedAtEpochMs: Long? = null,
    val lastPlayedAtEpochMs: Long? = null,
    val isWatched: Boolean = false,
    val keepDownload: Boolean = false,
    /** Jellyfin media source selected for this offline representation. */
    val mediaSourceId: String? = null,
    /** Server and account that own this managed representation. */
    val serverUrl: String? = null,
    val userId: String? = null
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

    /** Stable UI/database identity for both legacy and WorkManager transfers. */
    val stableKey: String get() = workName?.takeIf { it.isNotBlank() }
        ?: if (downloadId > 0L) "download:$downloadId"
        else "item:$itemId:quality:$quality"
}

/** Managed WorkManager rows have no DownloadManager id, so workName is their identity. */
internal fun sameOfflineEntry(first: OfflineDownload, second: OfflineDownload): Boolean =
    if (first.downloadId > 0L && second.downloadId > 0L) {
        first.downloadId == second.downloadId
    } else if (!first.workName.isNullOrBlank() && !second.workName.isNullOrBlank()) {
        first.workName == second.workName && offlineAccountMatches(first, second)
    } else {
        first.itemId == second.itemId && first.quality == second.quality && offlineAccountMatches(first, second)
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
        estimatedBytes: Long? = null,
        userId: String? = null
    ): OfflineDownload {
        check(PlatformCapabilities.supportsOfflineDownloads) {
            "Offline downloads are only supported on mobile and tablet builds"
        }
        val existing = load(context).firstOrNull {
            it.itemId == itemId && it.quality == quality.storageKey &&
                it.serverUrl.orEmpty().removeSuffix("/") == serverUrl.removeSuffix("/") &&
                it.userId.orEmpty() == userId.orEmpty()
        }
        if (existing != null) {
            if (existing.isComplete || existing.status == DownloadManager.STATUS_PENDING ||
                existing.status == DownloadManager.STATUS_RUNNING) {
                return existing
            }
        }

        // Evaluate before removing a previous failed entry, so a rejected
        // replacement never destroys the user's existing offline state.
        val recordedManagedBytes = load(context)
            // Only the representation being replaced is excluded. Other
            // qualities of the same item are valid, independent downloads.
            .filterNot { it.itemId == itemId && it.quality == quality.storageKey &&
                it.serverUrl.orEmpty().removeSuffix("/") == serverUrl.removeSuffix("/") &&
                it.userId.orEmpty() == userId.orEmpty() }
            .sumOf { entry ->
                if (entry.totalBytes > 0L) entry.totalBytes else entry.bytesDownloaded.coerceAtLeast(0L)
            }
        // The worker stores media in filesDir/offline/media. Measure that
        // same app-private filesystem; externalFilesDir can be mounted on a
        // different volume and would make the gate report misleading space.
        val storageRoot = File(context.filesDir, "offline/media").apply { mkdirs() }
        val availableBytes = StatFs(storageRoot.path).availableBytes
        // Use actual bytes on disk and retain the DB estimate as a conservative
        // floor for legacy rows whose provider URI is no longer inspectable.
        val managedBytes = maxOf(OfflineStorageEngine.managedBytes(context), recordedManagedBytes)
        val maxBytes = AppSettings(context).offlineMaxStorageBytes.takeIf { it > 0L }
        val decision = OfflineStoragePolicy.evaluate(
            snapshot = OfflineStorageSnapshot(availableBytes = availableBytes, managedBytes = managedBytes),
            limits = OfflineStorageLimits(maximumBytes = maxBytes),
            incomingBytes = estimatedBytes ?: 0L
        )
        if (!decision.allowed) throw StorageRejectedException(decision)
        if (existing != null) delete(context, existing)

        val workName = workNameFor(itemId, quality, serverUrl, userId)
        val entry = OfflineDownload(
            itemId = itemId,
            name = name,
            type = type,
            seriesName = seriesName,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            downloadId = 0L,
            quality = quality.storageKey,
            mediaSourceId = mediaSourceId,
            serverUrl = serverUrl,
            userId = userId,
            status = DownloadManager.STATUS_PENDING,
            workName = workName,
            createdAtEpochMs = System.currentTimeMillis()
        )
        val requiredNetwork = if (AppSettings(context).offlineWifiOnly) {
            androidx.work.NetworkType.UNMETERED
        } else {
            androidx.work.NetworkType.CONNECTED
        }
        val requiresCharging = AppSettings(context).offlineChargingOnly
        scheduleWork(
            context = context,
            itemId = itemId,
            mediaSourceId = mediaSourceId,
            quality = quality,
            workName = workName,
            requiredNetwork = requiredNetwork,
            requiresCharging = requiresCharging
        )
        save(context, load(context).filterNot {
            it.itemId == itemId && it.quality == quality.storageKey &&
                it.serverUrl.orEmpty().removeSuffix("/") == serverUrl.removeSuffix("/") &&
                it.userId.orEmpty() == userId.orEmpty()
        } + entry)
        return entry

    }

    suspend fun refresh(
        context: Context,
        accountServerUrl: String? = null,
        accountUserId: String? = null
    ): List<OfflineDownload> = withContext(Dispatchers.IO) {
        val manager = androidx.core.content.ContextCompat.getSystemService(context, DownloadManager::class.java)
        val updated = load(context).mapNotNull { entry ->
            if (!entry.workName.isNullOrBlank()) {
                val info = runCatching {
                    androidx.work.WorkManager.getInstance(context)
                        .getWorkInfosForUniqueWork(entry.workName).get().firstOrNull()
                }.getOrNull()
                // WorkManager is durable, but a cancelled/cleaned-up work row
                // can disappear while the SQLite record survives. Recreate
                // the unique work so an app/process restart never strands a
                // queued download forever.
                if (info == null && entry.state != OfflineDownloadState.COMPLETED && entry.state != OfflineDownloadState.PAUSED) {
                    scheduleWork(
                        context = context,
                        itemId = entry.itemId,
                        mediaSourceId = entry.mediaSourceId,
                        quality = OfflineDownloadQuality.fromStorageKey(entry.quality),
                        workName = entry.workName,
                        requiredNetwork = if (AppSettings(context).offlineWifiOnly) {
                            androidx.work.NetworkType.UNMETERED
                        } else androidx.work.NetworkType.CONNECTED,
                        requiresCharging = AppSettings(context).offlineChargingOnly
                    )
                }
                return@mapNotNull entry.copy(
                    status = when (info?.state) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> DownloadManager.STATUS_SUCCESSFUL
                        androidx.work.WorkInfo.State.FAILED, androidx.work.WorkInfo.State.CANCELLED -> DownloadManager.STATUS_FAILED
                        androidx.work.WorkInfo.State.RUNNING -> DownloadManager.STATUS_RUNNING
                        else -> DownloadManager.STATUS_PENDING
                    },
                    bytesDownloaded = info?.progress?.getLong(OfflineDownloadWorker.KEY_BYTES, entry.bytesDownloaded) ?: entry.bytesDownloaded,
                    totalBytes = info?.progress?.getLong(OfflineDownloadWorker.KEY_TOTAL_BYTES, entry.totalBytes) ?: entry.totalBytes,
                    speedBytesPerSecond = info?.progress?.getLong(OfflineDownloadWorker.KEY_SPEED_BPS, entry.speedBytesPerSecond) ?: entry.speedBytesPerSecond,
                    etaSeconds = info?.progress?.getLong(OfflineDownloadWorker.KEY_ETA_SECONDS, entry.etaSeconds ?: -1L)?.takeIf { it >= 0L } ?: entry.etaSeconds,
                    localPath = info?.outputData?.getString(OfflineDownloadWorker.KEY_LOCAL_PATH) ?: entry.localPath,
                    completedAtEpochMs = if (info?.state == androidx.work.WorkInfo.State.SUCCEEDED && entry.completedAtEpochMs == null) {
                        System.currentTimeMillis()
                    } else entry.completedAtEpochMs
                ).let { refreshed ->
                    if (refreshed.state == OfflineDownloadState.COMPLETED && !managedMediaExists(context, refreshed.localPath)) {
                        refreshed.copy(
                            status = DownloadManager.STATUS_FAILED,
                            reason = DownloadManager.ERROR_FILE_ERROR,
                            localPath = null
                        )
                    } else refreshed
                }
            }
            if (manager == null) return@mapNotNull entry
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
        // Complete rows from the pre-WorkManager provider are migrated as
        // soon as the downloads screen refreshes. This keeps DownloadManager
        // only as a compatibility reader for existing installations; new
        // playback and deletion then use Velora-owned storage exclusively.
        updated.filter { it.workName.isNullOrBlank() && it.isComplete }
            .forEach { entry -> OfflineStorageEngine.materialize(context, entry) }
        val refreshed = load(context)
        val settings = AppSettings(context)
        if (settings.smartDownloadsEnabled) {
            SmartDownloadPolicy.cleanupCandidates(
                refreshed,
                removeWatched = settings.smartDownloadsRemoveWatched,
                keepUnwatchedEpisodes = settings.smartDownloadsKeepUnwatchedEpisodes
            ).forEach { delete(context, it) }
        }
        val allEntries = load(context)
        if (accountServerUrl.isNullOrBlank() || accountUserId.isNullOrBlank()) {
            allEntries
        } else {
            val normalizedServer = accountServerUrl.removeSuffix("/")
            allEntries.filter { entry ->
                // Legacy entries have no ownership metadata. Keep them visible
                // so the user can play or replace them and complete migration.
                entry.serverUrl.isNullOrBlank() || entry.userId.isNullOrBlank() ||
                    (entry.serverUrl?.removeSuffix("/") == normalizedServer && entry.userId == accountUserId)
            }
        }
    }

    private fun workNameFor(itemId: String, quality: OfflineDownloadQuality, serverUrl: String, userId: String?): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${serverUrl.removeSuffix("/")}\u001f${userId.orEmpty()}\u001f$itemId".toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(24)
        return "offline-$digest-${quality.storageKey}"
    }

    private fun scheduleWork(
        context: Context,
        itemId: String,
        mediaSourceId: String?,
        quality: OfflineDownloadQuality,
        workName: String,
        requiredNetwork: androidx.work.NetworkType,
        requiresCharging: Boolean
    ) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(requiredNetwork)
            .setRequiresCharging(requiresCharging)
            .build()
        val work = androidx.work.OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .setInputData(androidx.work.workDataOf(
                OfflineDownloadWorker.KEY_ITEM_ID to itemId,
                OfflineDownloadWorker.KEY_MEDIA_SOURCE_ID to mediaSourceId,
                OfflineDownloadWorker.KEY_QUALITY to quality.storageKey,
                OfflineDownloadWorker.KEY_WORK_NAME to workName
            ))
            .addTag(OfflineDownloadWorker.TAG)
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            androidx.work.ExistingWorkPolicy.KEEP,
            work
        )
    }

    private fun managedMediaExists(context: Context, localPath: String?): Boolean {
        if (localPath.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(localPath) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            "file" -> uri.path?.let(::File)?.isFile == true
            else -> true
        }
    }

    fun cancel(context: Context, entry: OfflineDownload) {
        entry.workName?.let { androidx.work.WorkManager.getInstance(context).cancelUniqueWork(it) }
        if (entry.downloadId > 0L) androidx.core.content.ContextCompat.getSystemService(context, DownloadManager::class.java)?.remove(entry.downloadId)
        deleteEntry(context, entry)
    }

    /** Pause without deleting the partial media; the next resume uses Range. */
    fun pause(context: Context, entry: OfflineDownload) {
        entry.workName?.let { androidx.work.WorkManager.getInstance(context).cancelUniqueWork(it) }
        persist(context, entry.copy(
            status = DownloadManager.STATUS_PAUSED,
            reason = DownloadManager.PAUSED_WAITING_TO_RETRY,
            speedBytesPerSecond = 0L,
            etaSeconds = null
        ))
    }

    /** Resume a paused transfer using its durable WorkManager identity. */
    fun resume(context: Context, entry: OfflineDownload) {
        val workName = entry.workName ?: return
        val settings = AppSettings(context)
        val requiredNetwork = if (settings.offlineWifiOnly) {
            androidx.work.NetworkType.UNMETERED
        } else androidx.work.NetworkType.CONNECTED
        scheduleWork(
            context = context,
            itemId = entry.itemId,
            mediaSourceId = entry.mediaSourceId,
            quality = OfflineDownloadQuality.fromStorageKey(entry.quality),
            workName = workName,
            requiredNetwork = requiredNetwork,
            requiresCharging = settings.offlineChargingOnly
        )
        persist(context, entry.copy(
            status = DownloadManager.STATUS_PENDING,
            reason = 0,
            speedBytesPerSecond = 0L,
            etaSeconds = null
        ))
    }

    fun delete(context: Context, entry: OfflineDownload) {
        entry.workName?.let { androidx.work.WorkManager.getInstance(context).cancelUniqueWork(it) }
        if (entry.downloadId > 0L) androidx.core.content.ContextCompat.getSystemService(context, DownloadManager::class.java)?.remove(entry.downloadId)
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
                    if (sameOfflineEntry(current, entry)) current.copy(checksumSha256 = actual) else current
                })
            }
            true
        }
    }

    /** Record playback without changing the provider status or media URI. */
    fun markPlayed(context: Context, entry: OfflineDownload, atEpochMs: Long = System.currentTimeMillis()) {
        require(atEpochMs >= 0L) { "atEpochMs must be non-negative" }
        persist(context, entry.copy(lastPlayedAtEpochMs = atEpochMs))
    }

    fun markWatched(context: Context, entry: OfflineDownload, watched: Boolean = true) {
        persist(context, entry.copy(isWatched = watched))
    }

    /** Explicit protection survives Smart Downloads cleanup and app restarts. */
    fun setKeepDownload(context: Context, entry: OfflineDownload, keep: Boolean) {
        persist(context, entry.copy(keepDownload = keep))
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

    private fun deleteEntry(context: Context, entry: OfflineDownload) =
        save(context, load(context).filterNot { sameOfflineEntry(it, entry) })

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
                add(offlineDownloadFromLegacyJson(item))
            }
        }
        if (migrated.isNotEmpty()) db.replaceAll(migrated)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).apply()
        db.close()
        return migrated
    }

    internal fun persist(context: Context, entry: OfflineDownload) {
        val entries = load(context).let { current ->
            if (current.any { sameOfflineEntry(it, entry) }) {
                current.map { if (sameOfflineEntry(it, entry)) entry else it }
            } else current + entry
        }
        save(context, entries)
    }

    private fun save(context: Context, entries: List<OfflineDownload>) {
        val db = database(context)
        db.replaceAll(entries)
        db.close()
        // The preference is intentionally removed once the SQLite index is live.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).apply()
    }
}

/**
 * Reads both the original camelCase JSON index and the later snake_case
 * exports. Keeping every durable field here makes upgrades lossless before
 * the entry is rewritten into SQLite.
 */
internal fun offlineDownloadFromLegacyJson(item: JSONObject): OfflineDownload {
    val values = item.keys().asSequence().associateWith { key -> item.opt(key) }
    return offlineDownloadFromLegacyValues(values)
}

/** Pure JVM-friendly form of the legacy converter; the Android JSON adapter above stays tiny. */
internal fun offlineDownloadFromLegacyValues(values: Map<String, Any?>): OfflineDownload {
    fun string(vararg keys: String): String? = keys
        .asSequence()
        .mapNotNull { values[it]?.toString() }
        .firstOrNull { it.isNotBlank() }

    fun has(key: String): Boolean = values.containsKey(key)

    fun long(default: Long, vararg keys: String): Long = keys
        .firstOrNull { has(it) }
        ?.let { (values[it] as? Number)?.toLong() ?: values[it]?.toString()?.toLongOrNull() ?: default }
        ?: default

    fun int(default: Int, vararg keys: String): Int = keys
        .firstOrNull { has(it) }
        ?.let { (values[it] as? Number)?.toInt() ?: values[it]?.toString()?.toIntOrNull() ?: default }
        ?: default

    return OfflineDownload(
        itemId = string("itemId", "item_id").orEmpty(),
        name = string("name").orEmpty(),
        type = string("type").orEmpty(),
        seriesName = string("seriesName", "series_name"),
        seasonNumber = if (has("seasonNumber") || has("season_number")) int(0, "seasonNumber", "season_number") else null,
        episodeNumber = if (has("episodeNumber") || has("episode_number")) int(0, "episodeNumber", "episode_number") else null,
        downloadId = long(0L, "downloadId", "download_id"),
        quality = string("quality") ?: OfflineDownloadQuality.ORIGINAL.storageKey,
        localPath = string("localPath", "local_path"),
        status = int(DownloadManager.STATUS_PENDING, "status"),
        reason = int(0, "reason"),
        bytesDownloaded = long(0L, "bytesDownloaded", "bytes_downloaded"),
        totalBytes = long(-1L, "totalBytes", "total_bytes"),
        checksumSha256 = string("checksumSha256", "checksum_sha256"),
        workName = string("workName", "work_name"),
        createdAtEpochMs = long(0L, "createdAtEpochMs", "created_at"),
        completedAtEpochMs = if (has("completedAtEpochMs") || has("completed_at")) long(0L, "completedAtEpochMs", "completed_at") else null,
        lastPlayedAtEpochMs = if (has("lastPlayedAtEpochMs") || has("last_played_at")) long(0L, "lastPlayedAtEpochMs", "last_played_at") else null,
        isWatched = values["isWatched"] as? Boolean ?: values["is_watched"] as? Boolean ?: false,
        keepDownload = values["keepDownload"] as? Boolean ?: values["keep_download"] as? Boolean ?: false,
        mediaSourceId = string("mediaSourceId", "media_source_id"),
        serverUrl = string("serverUrl", "server_url"),
        userId = string("userId", "user_id")
    )
}
