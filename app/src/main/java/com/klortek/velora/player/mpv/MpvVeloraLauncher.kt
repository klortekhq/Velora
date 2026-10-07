package com.klortek.velora.player.mpv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.BuildConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Helper to launch the external MPV player external player.
 * 
 * external MPV player is a separate app optimized for Android TV that provides:
 * - YouTube TV-style controls
 * - Hardware-accelerated video decoding via MPV
 * - Automatic progress reporting to Jellyfin
 * - Resume playback support
 */
object MpvVeloraLauncher {
    private const val TAG = "MpvVeloraLauncher"
    private const val MPV_EXTERNAL_PACKAGE = "is.xyz.mpv"
    private const val MPV_EXTERNAL_ACTIVITY = "is.xyz.mpv.MpvActivity"

    private fun queryValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    
    /**
     * Check if MPV is available (always true now as it is embedded).
     */
    fun isInstalled(context: Context): Boolean {
        return true
    }
 
    /**
     * Build a direct stream URL for Jellyfin.
     */
    fun buildStreamUrl(
        serverUrl: String,
        itemId: String,
        accessToken: String,
        mediaSourceId: String? = null,
        startTimeTicks: Long? = null
    ): String {
        val baseUrl = serverUrl.removeSuffix("/")
        return buildString {
            append("$baseUrl/Videos/$itemId/stream?")
            if (startTimeTicks != null && startTimeTicks > 0) {
                append("static=false")
                append("&startTimeTicks=$startTimeTicks")
            } else {
                append("static=true")
            }
            append("&mediaSourceId=${queryValue(mediaSourceId ?: itemId)}")
            append("&enableAutoStreamCopy=true")
            append("&allowVideoStreamCopy=true")
            append("&allowAudioStreamCopy=true")
        }
     }
 
     /**
      * Build HTTP headers for Jellyfin authentication.
      */
     fun buildHeaders(
         accessToken: String,
         deviceId: String,
         clientName: String = "Velora",
         version: String = BuildConfig.VERSION_NAME
     ): String = buildString {
        append("User-Agent: $clientName/MPV\r\n")
        append("Authorization: MediaBrowser ")
        append("Client=\"$clientName\", Device=\"AndroidTV\", DeviceId=\"$deviceId\", ")
        append("Token=\"$accessToken\", Version=\"$version\"\r\n")
        append("Accept: */*\r\n")
    }

    /**
     * Launch external MPV player to play a video.
     * 
     * @param context Android context
     * @param itemId Jellyfin item ID
     * @param title Video title
     * @param resumePositionMs Resume position in milliseconds
     * @param config Jellyfin configuration
     * @param subtitleFilePath Optional path to an external subtitle file
     * @return true if launched successfully, false otherwise
     */
    fun play(
        context: Context,
        itemId: String,
        title: String,
        resumePositionMs: Long = 0L,
        config: JellyfinConfig,
        subtitleFilePath: String? = null
    ): Boolean {
        if (!isInstalled(context)) {
            Log.w(TAG, "external MPV player is not installed")
            return false
        }

        val serverUrl = config.serverUrl.removeSuffix("/")
        val accessToken = config.accessToken ?: ""
        val userId = config.userId ?: ""
        val deviceId = config.deviceId

        // Convert resume position to ticks for server-side seeking
        val resumeTicks = if (resumePositionMs > 0) resumePositionMs * 10_000L else null

        // Build stream URL
        val url = buildStreamUrl(
            serverUrl = serverUrl,
            itemId = itemId,
            accessToken = accessToken,
            startTimeTicks = resumeTicks
        )

        // Build headers
        val headers = buildHeaders(
            accessToken = accessToken,
            deviceId = deviceId
        )

        Log.d(TAG, "Launching MPV player with authenticated request headers")
        if (resumePositionMs > 0) {
            Log.d(TAG, "Resume position: ${resumePositionMs}ms")
        }
        if (subtitleFilePath != null) {
            Log.d(TAG, "External subtitle path provided")
        }

        return try {
            val intent = MpvTvPlayerActivity.createIntent(
                context = context,
                url = url,
                headers = headers,
                title = title,
                itemId = itemId,
                resumePositionMs = resumePositionMs
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch external MPV player", e)
            false
        }
    }
}

