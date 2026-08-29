package com.klortek.velora

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import com.klortek.velora.jellyfin.JellyfinApiService
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.AppSettings
import com.klortek.velora.player.mpv.MpvTvPlayerActivity
import com.klortek.velora.player.mpv.MpvUrlBuilder
import com.klortek.velora.screens.JellyfinVideoPlayerScreen
import `is`.xyz.mpv.MPVLib

@UnstableApi
class JellyfinVideoPlayerActivity : ComponentActivity() {
    
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }
    
    companion object {
        private const val EXTRA_ITEM_ID = "item_id"
        private const val EXTRA_ITEM_NAME = "item_name"
        private const val EXTRA_RESUME_POSITION_MS = "resume_position_ms"
        private const val EXTRA_SUBTITLE_STREAM_INDEX = "subtitle_stream_index"
        private const val EXTRA_AUDIO_STREAM_INDEX = "audio_stream_index"
        private const val EXTRA_IS_LIVE_TV = "is_live_tv"
        private const val EXTRA_LOCAL_PATH = "local_path"

        fun createIntent(
            context: Context,
            itemId: String,
            resumePositionMs: Long = 0L,
            subtitleStreamIndex: Int? = null,
            audioStreamIndex: Int? = null,
            itemName: String? = null,
            isLiveTv: Boolean = false,
            localPath: String? = null
        ): Intent {
            return Intent(context, JellyfinVideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
                putExtra(EXTRA_RESUME_POSITION_MS, resumePositionMs)
                subtitleStreamIndex?.let { putExtra(EXTRA_SUBTITLE_STREAM_INDEX, it) }
                audioStreamIndex?.let { putExtra(EXTRA_AUDIO_STREAM_INDEX, it) }
                itemName?.let { putExtra(EXTRA_ITEM_NAME, it) }
                putExtra(EXTRA_IS_LIVE_TV, isLiveTv)
                localPath?.let { putExtra(EXTRA_LOCAL_PATH, it) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun enableHdrMode() {
        // Enable hardware acceleration for HDR output (CRITICAL - must be first)
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        android.util.Log.d("VideoPlayer", "✅ Hardware acceleration enabled for HDR support")
        
        // Request HDR mode on the window (Android 13+)
        // Note: preferredHdrModes is not available in public API, but setting the Surface format
        // to RGBA_1010102 in BaseMPVView.surfaceCreated() is what actually enables HDR output.
        // The hardware acceleration flag above ensures the window can support HDR rendering.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.util.Log.d("VideoPlayer", "✅ Android 13+ detected - HDR support enabled via Surface format (RGBA_1010102)")
        }
        
        // Log HDR capabilities for debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(0)
                if (display != null) {
                    val hdrCapabilities = display.hdrCapabilities
                    if (hdrCapabilities != null) {
                        val supportedTypes = hdrCapabilities.supportedHdrTypes
                        android.util.Log.d("VideoPlayer", "✅ Display HDR capabilities: ${supportedTypes?.joinToString()}")
                        if (supportedTypes != null && supportedTypes.isNotEmpty()) {
                            android.util.Log.d("VideoPlayer", "✅ Display supports HDR types: ${supportedTypes.contentToString()}")
                        } else {
                            android.util.Log.w("VideoPlayer", "⚠️ Display does not support HDR")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("VideoPlayer", "Could not check HDR capabilities", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterImmersivePlayback()
        
        // Enable HDR mode BEFORE creating MPVView
        // This ensures the window is configured for HDR output
        enableHdrMode()
        
        // Request focus so key events work on Android TV
        window.decorView.requestFocus()

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val itemName = intent.getStringExtra(EXTRA_ITEM_NAME) ?: ""
        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
        val isLiveTv = intent.getBooleanExtra(EXTRA_IS_LIVE_TV, false)
        val resumePositionMs = intent.getLongExtra(EXTRA_RESUME_POSITION_MS, 0L)
        val subtitleStreamIndex = if (intent.hasExtra(EXTRA_SUBTITLE_STREAM_INDEX)) {
            intent.getIntExtra(EXTRA_SUBTITLE_STREAM_INDEX, -1).takeIf { it >= 0 }
        } else null
        val audioStreamIndex = if (intent.hasExtra(EXTRA_AUDIO_STREAM_INDEX)) {
            intent.getIntExtra(EXTRA_AUDIO_STREAM_INDEX, -1).takeIf { it >= 0 }
        } else null

        if (!localPath.isNullOrBlank()) {
            // Offline media belongs to Velora and must use the same default
            // Media3/ExoPlayer path as streamed media. DownloadManager may
            // return a content:// URI, which ExoPlayer resolves through the
            // Android content resolver; MPV cannot be assumed to access it.
            // Keep this route server-independent so downloaded media remains
            // playable with no network or Jellyfin session.
            val offlineApiService = JellyfinApiService(
                baseUrl = "http://127.0.0.1",
                accessToken = "",
                userId = ""
            )
            setContent {
                JellyfinAppTheme {
                    JellyfinVideoPlayerScreen(
                        item = JellyfinItem(Id = itemId, Name = itemName),
                        apiService = offlineApiService,
                        onBack = { finish() },
                        resumePositionMs = resumePositionMs,
                        subtitleStreamIndex = subtitleStreamIndex,
                        audioStreamIndex = audioStreamIndex,
                        initialMediaUrl = localPath,
                        offlineOnly = true
                    )
                }
            }
            return
        }

        // Get Jellyfin configuration and API service
        val config = JellyfinConfig(this)
        val settings = AppSettings(this)
        val apiService = if (config.isConfigured()) {
            JellyfinApiService(
                baseUrl = config.serverUrl,
                accessToken = config.accessToken,
                userId = config.userId,
                config = config
            )
        } else {
            finish()
            return
        }

        // ExoPlayer is the default for every playback type, including Live TV.
        // MPV is only selected explicitly in Ajustes. ExoPlayer's existing
        // error listener can still hand off to MPV when fallbackToMpv is enabled.
        if (settings.isMpvEnabled) {
            val serverUrl = config.serverUrl.removeSuffix("/")
            val accessToken = config.accessToken ?: ""
            
            android.util.Log.d("VideoPlayer", "MPV player enabled - launching embedded MpvTvPlayerActivity")
            
            lifecycleScope.launch {
                var finalUrl: String
                var extraSubtitleUrl: String? = null
                val headers = "X-Emby-Token: $accessToken" // Basic header needed

                // Jellyfin must open an M3U/Acestream Live TV source first so
                // it can return the MediaSourceId and LiveStreamId required by
                // its master HLS manifest. VOD keeps the existing source flow.
                val playbackInfo = apiService.getPlaybackInfo(
                    itemId = itemId,
                    mediaSourceId = if (isLiveTv) null else itemId,
                    subtitleStreamIndex = subtitleStreamIndex,
                    // Resolve the channel metadata immediately. Direct Play
                    // can then start from Jellyfin's returned source without
                    // waiting for a server-side HLS allocation.
                    autoOpenLiveStream = !isLiveTv
                )
                
                val mediaSource = playbackInfo?.MediaSources?.firstOrNull()
                val videoStream = mediaSource?.MediaStreams?.firstOrNull { it.Type == "Video" }
                val videoCodec = videoStream?.Codec?.lowercase() ?: ""
                
                // Check user transcoding settings
                val enforceTranscoding = settings.serverTranscodingEnabled && (
                    (settings.transcodeAV1 && (videoCodec.contains("av1") || videoCodec.contains("av01"))) ||
                    (settings.transcodeHEVC && (videoCodec.contains("hevc") || videoCodec.contains("h265")))
                )

                if (isLiveTv) {
                    val liveSource = playbackInfo?.MediaSources?.firstOrNull()
                    val liveMediaSourceId = liveSource?.Id
                    val liveStreamId = liveSource?.LiveStreamId
                    val directSource = liveSource?.Path
                    if (liveSource?.SupportsDirectPlay == true && !directSource.isNullOrBlank() &&
                        (directSource.startsWith("http://") || directSource.startsWith("https://"))) {
                        finalUrl = MpvUrlBuilder.buildLiveTvDirectSourceUrl(directSource)
                        android.util.Log.d("VideoPlayer", "Live TV Direct Play source selected from Jellyfin PlaybackInfo")
                    } else if (!liveMediaSourceId.isNullOrBlank() && !liveStreamId.isNullOrBlank()) {
                        finalUrl = MpvUrlBuilder.buildLiveTvStreamUrl(
                            serverUrl = serverUrl,
                            itemId = itemId,
                            accessToken = accessToken,
                            mediaSourceId = liveMediaSourceId,
                            liveStreamId = liveStreamId
                        )
                        android.util.Log.d("VideoPlayer", "Live TV HLS fallback selected with PlaybackInfo stream identifiers")
                    } else {
                        android.util.Log.e("VideoPlayer", "Live TV PlaybackInfo did not return a playable source")
                        runOnUiThread {
                            android.widget.Toast.makeText(
                                this@JellyfinVideoPlayerActivity,
                                getString(R.string.live_tv_playback_error),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        finish()
                        return@launch
                    }
                } else if (enforceTranscoding) {
                    android.util.Log.d("VideoPlayer", "🔄 Enforcing Transcoding (Codec: $videoCodec)")
                    // Use the TranscodingUrl from PlaybackInfo (includes burned subs if requested)
                    val transcodeUrl = mediaSource?.TranscodingUrl
                    if (transcodeUrl != null) {
                         finalUrl = if (transcodeUrl.startsWith("http")) transcodeUrl else "$serverUrl$transcodeUrl"
                         android.util.Log.d("VideoPlayer", "🔥 Using Transcoding URL (Burn-in active): $finalUrl")
                    } else {
                         android.util.Log.w("VideoPlayer", "⚠️ Transcoding enforced but no URL. Fallback to Direct.")
                         finalUrl = MpvUrlBuilder.buildStreamUrl(serverUrl, itemId, accessToken)
                    }
                } else {
                    // Direct Play Mode (Soft Subs)
                    finalUrl = MpvUrlBuilder.buildStreamUrl(serverUrl, itemId, accessToken)
                    android.util.Log.d("VideoPlayer", "▶️ Direct Play (Video Copy)")

                    // If subtitles selected, handle as Soft Subs (copy/extract)
                    if (subtitleStreamIndex != null) {
                        // Find the selected stream to get details (Codec, etc)
                        val subStream = mediaSource?.MediaStreams?.find { it.Type == "Subtitle" && it.Index == subtitleStreamIndex }
                        if (subStream != null) {
                             // Build soft subtitle URL
                             // Use standard extraction URL which MPV can stream
                             extraSubtitleUrl = apiService.buildJellyfinSubtitleUrl(
                                 itemId = itemId,
                                 mediaSourceId = itemId,
                                 streamIndex = subtitleStreamIndex,
                                 isExternal = subStream.IsExternal == true,
                                 codec = subStream.Codec,
                                 path = subStream.Path
                             )
                             android.util.Log.d("VideoPlayer", "📝 Soft Subtitle URL: $extraSubtitleUrl")
                        }
                    }
                }

                android.util.Log.d("VideoPlayer", "MPV Final URL: $finalUrl")
                
                // Prioritize local cached subtitle (if any), otherwise use remote soft-sub URL
                val cachedSubtitlePath = subtitleStreamIndex?.let { streamIndex ->
                    com.klortek.velora.player.SubtitleDownloader.getCachedSubtitle(itemId, streamIndex)
                }
                
                // Final subtitle source: Cached Local > Remote Soft Sub > None
                val subtitleSource = cachedSubtitlePath ?: extraSubtitleUrl
                if (subtitleSource != null) {
                    android.util.Log.d("VideoPlayer", "Using Subtitle Source: $subtitleSource")
                }
                
                val intent = MpvTvPlayerActivity.createIntent(
                    context = this@JellyfinVideoPlayerActivity,
                    url = finalUrl,
                    headers = headers,
                    title = itemName,
                    itemId = itemId,
                    resumePositionMs = resumePositionMs
                )
                
                // Pass selected streams
                subtitleStreamIndex?.let { intent.putExtra("subtitle_stream_index", it) }
                audioStreamIndex?.let { intent.putExtra("audio_stream_index", it) }
                
                // Pass the subtitle file/URL
                if (subtitleSource != null) {
                    intent.putExtra("subtitle_file", subtitleSource)
                }

                startActivity(intent)
                finish()
            }
            return
        }

        // Live TV uses Jellyfin's resolved PlaybackInfo source. Do not force MPV
        // here: ExoPlayer handles Jellyfin HLS/TS streams natively and keeps the
        // same track, subtitle and aspect-ratio controls as normal playback.
        if (isLiveTv) {
            lifecycleScope.launch {
                val serverUrl = config.serverUrl.removeSuffix("/")
                val accessToken = config.accessToken ?: ""
                val playbackInfo = apiService.getPlaybackInfo(
                    itemId = itemId,
                    mediaSourceId = null,
                    subtitleStreamIndex = subtitleStreamIndex,
                    autoOpenLiveStream = false
                )
                val liveSource = playbackInfo?.MediaSources?.firstOrNull()
                val liveMediaSourceId = liveSource?.Id
                val liveStreamId = liveSource?.LiveStreamId
                val directSource = liveSource?.Path
                val finalUrl = if (liveSource?.SupportsDirectPlay == true &&
                    !directSource.isNullOrBlank() &&
                    (directSource.startsWith("http://") || directSource.startsWith("https://"))) {
                    MpvUrlBuilder.buildLiveTvDirectSourceUrl(directSource)
                } else if (!liveMediaSourceId.isNullOrBlank() && !liveStreamId.isNullOrBlank()) {
                    MpvUrlBuilder.buildLiveTvStreamUrl(
                        serverUrl = serverUrl,
                        itemId = itemId,
                        accessToken = accessToken,
                        mediaSourceId = liveMediaSourceId,
                        liveStreamId = liveStreamId
                    )
                } else {
                    null
                }

                if (finalUrl == null) {
                    android.util.Log.e("VideoPlayer", "Live TV PlaybackInfo did not return a playable ExoPlayer source")
                    runOnUiThread {
                        android.widget.Toast.makeText(
                            this@JellyfinVideoPlayerActivity,
                            getString(R.string.live_tv_playback_error),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                android.util.Log.d("VideoPlayer", "ExoPlayer Live TV source selected: $finalUrl")
                setContent {
                    JellyfinAppTheme {
                        JellyfinVideoPlayerScreen(
                            item = JellyfinItem(Id = itemId, Name = itemName),
                            apiService = apiService,
                            onBack = { finish() },
                            resumePositionMs = resumePositionMs,
                            subtitleStreamIndex = subtitleStreamIndex,
                            audioStreamIndex = audioStreamIndex,
                            initialMediaUrl = finalUrl
                        )
                    }
                }
            }
            return
        }

        // Create a minimal item object (details will be fetched in the screen)
        val item = JellyfinItem(
            Id = itemId,
            Name = itemName
        )

        setContent {
            JellyfinAppTheme {
                // Use ExoPlayer with FFmpeg for comprehensive codec support
                JellyfinVideoPlayerScreen(
                    item = item,
                    apiService = apiService,
                    onBack = {
                        finish()
                    },
                    resumePositionMs = resumePositionMs,
                    subtitleStreamIndex = subtitleStreamIndex,
                    audioStreamIndex = audioStreamIndex
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun enterImmersivePlayback() {
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersivePlayback()
    }

    // Removed onBackPressed - let Compose BackHandler handle it
    // This prevents duplicate finish() calls
    
    // Removed onKeyDown - let PlayerView handle key events directly
    // The PlayerView is configured to be focusable and will handle Enter/OK keys
    // Intercepting here prevents the PlayerView from receiving the events
}


