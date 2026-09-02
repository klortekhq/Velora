package com.klortek.velora.offline

import android.content.Context
import android.net.Uri
import com.klortek.velora.platform.PlatformCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Moves completed provider downloads into storage owned by Velora. */
object OfflineStorageEngine {
    private const val MEDIA_DIRECTORY = "offline/media"

    /**
     * Measure every file in Velora's private offline volume, including partial
     * transfers. Counting the directory rather than only completed rows keeps
     * the storage limit safe after process death or an interrupted transfer.
     */
    fun managedBytes(context: Context): Long {
        val root = File(context.filesDir, MEDIA_DIRECTORY)
        if (!root.exists()) return 0L
        var total = 0L
        root.walkTopDown().filter(File::isFile).forEach { file ->
            val size = file.length().coerceAtLeast(0L)
            total = if (Long.MAX_VALUE - total < size) Long.MAX_VALUE else total + size
            if (total == Long.MAX_VALUE) return@forEach
        }
        return total
    }

    suspend fun materialize(context: Context, entry: OfflineDownload): OfflineDownload? = withContext(Dispatchers.IO) {
        if (!PlatformCapabilities.supportsOfflineDownloads || !entry.isComplete) return@withContext null
        val source = entry.localPath ?: return@withContext null
        if (source.startsWith("file://${context.filesDir.absolutePath}/$MEDIA_DIRECTORY/")) return@withContext entry

        val root = File(context.filesDir, MEDIA_DIRECTORY).apply { mkdirs() }
        val fileKey = stableFileKey(entry.stableKey)
        val destination = File(root, "$fileKey.media")
        val temporary = File(root, "$fileKey.part")
        val input = open(context, source) ?: return@withContext null
        try {
            input.use { inputStream ->
                FileOutputStream(temporary).use { output ->
                    inputStream.copyTo(output, DEFAULT_BUFFER_SIZE)
                    output.fd.sync()
                }
            }
            val digest = FileInputStream(temporary).use { OfflineIntegrityVerifier.sha256(it) }
            if (entry.checksumSha256 != null && !entry.checksumSha256.equals(digest, ignoreCase = true)) {
                temporary.delete()
                return@withContext null
            }
            if (destination.exists()) destination.delete()
            check(temporary.renameTo(destination)) { "Could not commit offline media" }
            // The provider row has been copied into Velora's private storage;
            // clear its provider id so a later refresh cannot query or remove
            // an already-migrated DownloadManager row.
            val updated = entry.copy(
                downloadId = 0L,
                localPath = Uri.fromFile(destination).toString(),
                checksumSha256 = digest
            )
            OfflineDownloadManager.persist(context, updated)
            if (entry.downloadId > 0L) {
                androidx.core.content.ContextCompat.getSystemService(context, android.app.DownloadManager::class.java)?.remove(entry.downloadId)
            }
            updated
        } catch (_: Exception) {
            temporary.delete()
            null
        }
    }

    private fun open(context: Context, value: String) = runCatching {
        val uri = Uri.parse(value)
        when (uri.scheme?.lowercase()) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> uri.path?.let(::File)?.inputStream()
            else -> File(value).inputStream()
        }
    }.getOrNull()

    private fun stableFileKey(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
        .take(32)
}
