package com.klortek.velora.offline

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Durable, app-private index for managed offline media. */
internal class OfflineDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "velora_offline.db",
    null,
    12
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE downloads (
                entry_key TEXT NOT NULL PRIMARY KEY,
                item_id TEXT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                series_name TEXT,
                season_number INTEGER,
                episode_number INTEGER,
                download_id INTEGER NOT NULL,
                quality TEXT NOT NULL DEFAULT 'original',
                local_path TEXT,
                status INTEGER NOT NULL,
                reason INTEGER NOT NULL,
                bytes_downloaded INTEGER NOT NULL,
                total_bytes INTEGER NOT NULL,
                speed_bps INTEGER NOT NULL DEFAULT 0,
                eta_seconds INTEGER,
                media_source_id TEXT,
                checksum_sha256 TEXT,
                work_name TEXT,
                created_at INTEGER NOT NULL DEFAULT 0,
                completed_at INTEGER,
                last_played_at INTEGER,
                is_watched INTEGER NOT NULL DEFAULT 0,
                keep_download INTEGER NOT NULL DEFAULT 0,
                server_url TEXT,
                user_id TEXT
            )"""
        )
        db.execSQL("CREATE INDEX downloads_download_id ON downloads(download_id)")
        db.execSQL("CREATE INDEX downloads_item_quality ON downloads(item_id, quality)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN quality TEXT NOT NULL DEFAULT 'original'")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN checksum_sha256 TEXT")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN work_name TEXT")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE downloads ADD COLUMN completed_at INTEGER")
            db.execSQL("ALTER TABLE downloads ADD COLUMN last_played_at INTEGER")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN is_watched INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE downloads ADD COLUMN keep_download INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN media_source_id TEXT")
        }
        if (oldVersion < 8) {
            // The original schema keyed rows only by item_id. Rebuild it so
            // an item can safely have separate Original/Medium/etc. entries.
            db.execSQL(
                """CREATE TABLE downloads_v8 (
                    item_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    series_name TEXT,
                    season_number INTEGER,
                    episode_number INTEGER,
                    download_id INTEGER NOT NULL,
                    quality TEXT NOT NULL DEFAULT 'original',
                    local_path TEXT,
                    status INTEGER NOT NULL,
                    reason INTEGER NOT NULL,
                    bytes_downloaded INTEGER NOT NULL,
                    total_bytes INTEGER NOT NULL,
                    media_source_id TEXT,
                    checksum_sha256 TEXT,
                    work_name TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    completed_at INTEGER,
                    last_played_at INTEGER,
                    is_watched INTEGER NOT NULL DEFAULT 0,
                    keep_download INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (item_id, quality)
                )"""
            )
            db.execSQL(
                """INSERT INTO downloads_v8 (
                    item_id, name, type, series_name, season_number,
                    episode_number, download_id, quality, local_path, status,
                    reason, bytes_downloaded, total_bytes, media_source_id,
                    checksum_sha256, work_name, created_at, completed_at,
                    last_played_at, is_watched, keep_download
                ) SELECT item_id, name, type, series_name, season_number,
                    episode_number, download_id, quality, local_path, status,
                    reason, bytes_downloaded, total_bytes, media_source_id,
                    checksum_sha256, work_name, created_at, completed_at,
                    last_played_at, is_watched, keep_download
                    FROM downloads"""
            )
            db.execSQL("DROP TABLE downloads")
            db.execSQL("ALTER TABLE downloads_v8 RENAME TO downloads")
            db.execSQL("CREATE INDEX downloads_download_id ON downloads(download_id)")
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN server_url TEXT")
        }
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN user_id TEXT")
        }
        if (oldVersion < 11) {
            db.execSQL(
                """CREATE TABLE downloads_v11 (
                    entry_key TEXT NOT NULL PRIMARY KEY,
                    item_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    series_name TEXT,
                    season_number INTEGER,
                    episode_number INTEGER,
                    download_id INTEGER NOT NULL,
                    quality TEXT NOT NULL DEFAULT 'original',
                    local_path TEXT,
                    status INTEGER NOT NULL,
                    reason INTEGER NOT NULL,
                    bytes_downloaded INTEGER NOT NULL,
                    total_bytes INTEGER NOT NULL,
                    media_source_id TEXT,
                    checksum_sha256 TEXT,
                    work_name TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    completed_at INTEGER,
                    last_played_at INTEGER,
                    is_watched INTEGER NOT NULL DEFAULT 0,
                    keep_download INTEGER NOT NULL DEFAULT 0,
                    server_url TEXT,
                    user_id TEXT
                )"""
            )
            db.execSQL(
                """INSERT INTO downloads_v11 (
                    entry_key, item_id, name, type, series_name, season_number,
                    episode_number, download_id, quality, local_path, status,
                    reason, bytes_downloaded, total_bytes, media_source_id,
                    checksum_sha256, work_name, created_at, completed_at,
                    last_played_at, is_watched, keep_download, server_url, user_id
                ) SELECT COALESCE(server_url, '') || char(31) ||
                    COALESCE(user_id, '') || char(31) || item_id || char(31) || quality,
                    item_id, name, type, series_name, season_number,
                    episode_number, download_id, quality, local_path, status,
                    reason, bytes_downloaded, total_bytes, media_source_id,
                    checksum_sha256, work_name, created_at, completed_at,
                    last_played_at, is_watched, keep_download, server_url, user_id
                    FROM downloads"""
            )
            db.execSQL("DROP TABLE downloads")
            db.execSQL("ALTER TABLE downloads_v11 RENAME TO downloads")
            db.execSQL("CREATE INDEX downloads_download_id ON downloads(download_id)")
            db.execSQL("CREATE INDEX downloads_item_quality ON downloads(item_id, quality)")
        }
        if (oldVersion < 12) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN speed_bps INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE downloads ADD COLUMN eta_seconds INTEGER")
        }
    }

    fun readAll(): List<OfflineDownload> {
        val result = mutableListOf<OfflineDownload>()
        readableDatabase.query(
            "downloads", null, null, null, null, null, "rowid ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += OfflineDownload(
                    itemId = cursor.getString(cursor.getColumnIndexOrThrow("item_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    seriesName = cursor.getStringOrNull("series_name"),
                    seasonNumber = cursor.getIntOrNull("season_number"),
                    episodeNumber = cursor.getIntOrNull("episode_number"),
                    downloadId = cursor.getLong(cursor.getColumnIndexOrThrow("download_id")),
                    quality = cursor.getString(cursor.getColumnIndexOrThrow("quality")),
                    localPath = cursor.getStringOrNull("local_path"),
                    status = cursor.getInt(cursor.getColumnIndexOrThrow("status")),
                    reason = cursor.getInt(cursor.getColumnIndexOrThrow("reason")),
                    bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow("bytes_downloaded")),
                    totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow("total_bytes")),
                    speedBytesPerSecond = cursor.getLong(cursor.getColumnIndexOrThrow("speed_bps")),
                    etaSeconds = cursor.getLongOrNull("eta_seconds"),
                    mediaSourceId = cursor.getStringOrNull("media_source_id"),
                    checksumSha256 = cursor.getStringOrNull("checksum_sha256"),
                    workName = cursor.getStringOrNull("work_name"),
                    createdAtEpochMs = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                    completedAtEpochMs = cursor.getLongOrNull("completed_at"),
                    lastPlayedAtEpochMs = cursor.getLongOrNull("last_played_at"),
                    isWatched = cursor.getInt(cursor.getColumnIndexOrThrow("is_watched")) != 0,
                    keepDownload = cursor.getInt(cursor.getColumnIndexOrThrow("keep_download")) != 0,
                    serverUrl = cursor.getStringOrNull("server_url"),
                    userId = cursor.getStringOrNull("user_id")
                )
            }
        }
        return result
    }

    fun replaceAll(entries: List<OfflineDownload>) {
        writableDatabase.transaction {
            delete("downloads", null, null)
            entries.forEach { entry -> insertOrThrow("downloads", null, entry.values()) }
        }
    }

    fun deleteByDownloadId(downloadId: Long) {
        writableDatabase.delete("downloads", "download_id = ?", arrayOf(downloadId.toString()))
    }

    private fun OfflineDownload.values() = ContentValues().apply {
        put("entry_key", offlineEntryKey(this@values))
        put("item_id", itemId); put("name", name); put("type", type)
        put("series_name", seriesName); put("season_number", seasonNumber); put("episode_number", episodeNumber)
        put("download_id", downloadId); put("local_path", localPath); put("status", status); put("reason", reason)
        put("quality", quality)
        put("bytes_downloaded", bytesDownloaded); put("total_bytes", totalBytes)
        put("speed_bps", speedBytesPerSecond); put("eta_seconds", etaSeconds)
        put("media_source_id", mediaSourceId)
        put("checksum_sha256", checksumSha256); put("work_name", workName)
        put("created_at", createdAtEpochMs); put("completed_at", completedAtEpochMs); put("last_played_at", lastPlayedAtEpochMs)
        put("is_watched", if (isWatched) 1 else 0); put("keep_download", if (keepDownload) 1 else 0)
        put("server_url", serverUrl); put("user_id", userId)
    }

    private fun android.database.Cursor.getStringOrNull(column: String): String? =
        getString(getColumnIndexOrThrow(column))?.takeIf { it.isNotBlank() }

    private fun android.database.Cursor.getIntOrNull(column: String): Int? =
        if (isNull(getColumnIndexOrThrow(column))) null else getInt(getColumnIndexOrThrow(column))

    private fun android.database.Cursor.getLongOrNull(column: String): Long? =
        if (isNull(getColumnIndexOrThrow(column))) null else getLong(getColumnIndexOrThrow(column))
}

private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}
