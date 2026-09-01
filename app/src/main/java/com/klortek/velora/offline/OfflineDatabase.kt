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
    6
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE downloads (
                item_id TEXT PRIMARY KEY NOT NULL,
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
                checksum_sha256 TEXT,
                work_name TEXT,
                created_at INTEGER NOT NULL DEFAULT 0,
                completed_at INTEGER,
                last_played_at INTEGER,
                is_watched INTEGER NOT NULL DEFAULT 0,
                keep_download INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE INDEX downloads_download_id ON downloads(download_id)")
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
                    checksumSha256 = cursor.getStringOrNull("checksum_sha256"),
                    workName = cursor.getStringOrNull("work_name"),
                    createdAtEpochMs = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                    completedAtEpochMs = cursor.getLongOrNull("completed_at"),
                    lastPlayedAtEpochMs = cursor.getLongOrNull("last_played_at"),
                    isWatched = cursor.getInt(cursor.getColumnIndexOrThrow("is_watched")) != 0,
                    keepDownload = cursor.getInt(cursor.getColumnIndexOrThrow("keep_download")) != 0
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
        put("item_id", itemId); put("name", name); put("type", type)
        put("series_name", seriesName); put("season_number", seasonNumber); put("episode_number", episodeNumber)
        put("download_id", downloadId); put("local_path", localPath); put("status", status); put("reason", reason)
        put("quality", quality)
        put("bytes_downloaded", bytesDownloaded); put("total_bytes", totalBytes)
        put("checksum_sha256", checksumSha256); put("work_name", workName)
        put("created_at", createdAtEpochMs); put("completed_at", completedAtEpochMs); put("last_played_at", lastPlayedAtEpochMs)
        put("is_watched", if (isWatched) 1 else 0); put("keep_download", if (keepDownload) 1 else 0)
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
