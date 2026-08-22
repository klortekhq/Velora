Warning: truncated output (original token count: 89769)
Total output lines: 5889

package com.flex.elefin.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.ColorInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.common.ParserException
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.flex.elefin.jellyfin.JellyfinApiService
import com.flex.elefin.jellyfin.JellyfinItem
import com.flex.elefin.jellyfin.MediaStream
import com.flex.elefin.jellyfin.SkipMarkers
import com.flex.elefin.player.SubtitleMapper
import com.flex.elefin.player.GLVideoSurfaceView
import com.flex.elefin.player.PlaybackQuality
import android.widget.FrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.flex.elefin.ui.DeviceUtils
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.material.icons.Icons
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import androidx.tv.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Tv
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.media3.common.TrackSelectionOverride
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import com.flex.elefin.theme.*

// Picture mode / aspect ratio options
enum class AspectMode(val label: String) {
    FIT("Fit"),              // Natural letterbox - fits video in screen with black bars
    FILL("Fill"),            // Crop to fill screen - removes black bars by cropping
    LETTERBOX("16:9"),       // Force 16:9 letterbox - maintains aspect ratio in 16:9 frame
    CINEMA("Cinema"),        // Cinema scope 2.39:1 - movie theater style with wide black bars
    STRETCH("Stretch"),      // Stretch both axes - distorts to fill screen
    ORIGINAL("Original");    // Display at native resolution without scaling

    fun next(): AspectMode {
        val modes = values()
        return modes[(ordinal + 1) % modes.size]
    }
}

@UnstableApi
@Composable
fun JellyfinVideoPlayerScreen(
    item: JellyfinItem,
    apiService: JellyfinApiService,
    onBack: () -> Unit = {},
    resumePositionMs: Long = 0L,
    subtitleStreamIndex: Int? = null,
    audioStreamIndex: Int? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { com.flex.elefin.jellyfin.AppSettings(context) }
    
    val themeColor = remember(settings.themeColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.themeColorHex))
        } catch (e: Exception) {
            Color(0xFF9C27B0) // Fallback to purple
        }
    }
    
    val themeColorInt = remember(settings.themeColorHex) {
        try {
            android.graphics.Color.parseColor(settings.themeColorHex)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#9C27B0")
        }
    }
    
    val transparentThemeColorInt = remember(themeColorInt) {
        android.graphics.Color.argb(
            150,
            android.graphics.Color.red(themeColorInt),
            android.graphics.Color.green(themeColorInt),
            android.graphics.Color.blue(themeColorInt)
        )
    }
    
    // Track itemDetails state for codec detection (will be populated by LaunchedEffect)
    var itemDetails by remember { mutableStateOf<JellyfinItem?>(null) }
    var playbackQuality by remember(item.Id) {
        mutableStateOf(
            runCatching { PlaybackQuality.valueOf(settings.playbackQuality) }
                .getOrDefault(PlaybackQuality.ORIGINAL)
        )
    }
    
    // GL Enhancement settings
    // IMPORTANT: GL mode causes black screen with AV1 on devices without hardware AV1 decoder
    // Shield TV has NO hardware AV1 - software decode + GL surface = black screen
    // 
    // Since Jellyfin often returns Codec=null, we CANNOT rely on metadata detection.
    // Instead, we detect AV1 at RUNTIME from ExoPlayer's track info and force safe mode.
    val glSettingEnabled = remember { settings.useGLEnhancements }
    
    // Track if AV1 was detected at runtime (will be set by ExoPlayer listener)
    var runtimeAV1Detected by remember { mutableStateOf(false) }
    
    // Show error dialog for AV1 decoding failure
    var showAV1Error by remember { mutableStateOf(false) }
    
    // Auto-transcode fallback state
    val autoTranscodeOnError = remember { settings.autoTranscodeOnError }
    var hasTriedTranscodeFallback by remember { mutableStateOf(false) }
    var isUsingTranscodeFallback by remember { mutableStateOf(false) }
    
    // MPV fallback state
    val fallbackToMpv = remember { settings.fallbackToMpv }
    var hasTriedMpvFallback by remember { mutableStateOf(false) }
    val isMpvInstalled = remember { com.flex.elefin.player.mpv.MpvVeloraLauncher.isInstalled(context) }
    
    // Helper function to launch MPV as fallback
    val jellyfinConfig = remember { com.flex.elefin.jellyfin.JellyfinConfig(context) }
    val launchMpvFallback: () -> Unit = {
        Log.d("JellyfinPlayer", "🎬 Launching MPV player as fallback...")
        
        if (jellyfinConfig.isConfigured()) {
            // Try to get cached subtitle if one was selected
            val subtitlePath = subtitleStreamIndex?.let { streamIndex ->
                com.flex.elefin.player.SubtitleDownloader.getCachedSubtitle(item.Id, streamIndex)
            }
            if (subtitlePath != null) {
                Log.d("JellyfinPlayer", "🎬 Found cached subtitle for MPV: $subtitlePath")
            }
            
            val success = com.flex.elefin.player.mpv.MpvVeloraLauncher.play(
                context = context,
                itemId = item.Id,
                title = item.Name ?: "Video",
                resumePositionMs = resumePositionMs,
                config = jellyfinConfig,
                subtitleFilePath = subtitlePath
            )
            
            if (success) {
                Log.d("JellyfinPlayer", "✅ MPV launched successfully - closing ExoPlayer")
                // Go back since we're switching to MPV
                onBack()
            } else {
                Log.e("JellyfinPlayer", "❌ Failed to launch MPV")
                showAV1Error = true
            }
        } else {
            Log.e("JellyfinPlayer", "❌ No Jellyfin config available for MPV fallback")
            showAV1Error = true
        }
    }
    
    // Start with user's GL setting, but will be overridden if AV1 detected at runtime
    // Note: The actual enforcement happens in the player listener below
    val useGLEnhancements = remember { 
        Log.d("JellyfinPlayer", "🎨 GL enhancements mode: $glSettingEnabled (will be disabled if AV1 detected at runtime)")
        glSettingEnabled 
    }
    val enableFakeHDR = remember { settings.enableFakeHDR }
    val enableSharpening = remember { settings.enableSharpening }
    val hdrStrength = remember { settings.hdrStrength }
    val sharpenStrength = remember { settings.sharpenStrength }
    
    // New video enhancement settings
    val enableDenoise = remember { settings.enableDenoise }
    val denoiseStrength = remember { settings.denoiseStrength }
    val enableDeband = remember { settings.enableDeband }
    val debandStrength = remember { settings.debandStrength }
    val enableFXAA = remember { settings.enableFXAA }
    val videoBrightness = remember { settings.videoBrightness }
    val videoContrast = remember { settings.videoContrast }
    val videoSaturation = remember { settings.videoSaturation }
    val videoColorTemperature = remember { settings.videoColorTemperature }
    
    // Load stored audio preference if not provided
    val storedAudioPreference = remember(item.Id) {
        if (audioStreamIndex == null) {
            val pref = settings.getAudioPreference(item.Id)
            Log.d("JellyfinPlayer", "Loaded stored audio preference for ${item.Id}: $pref")
            pref
        } else {
            Log.d("JellyfinPlayer", "Using provided audioStreamIndex: $audioStreamIndex")
            audioStreamIndex
        }
    }
    // Create player with enhanced codec support and LoadControl configured based on settings
    // Enable extension renderers (including FFmpeg) and decoder fallback
    // FFmpeg supports: DTS, DTS-HD, TrueHD, AC3, E-AC3, FLAC, ALAC, Vorbis, Opus
    val renderersFactory = remember {
        DefaultRenderersFactory(context).apply {
            // Original First: prefer Android platform/MediaCodec; keep FFmpeg as compatibility fallback
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
            Log.d("JellyfinPlayer", "🎬 ExoPlayer initialized with FFmpeg extension support")
            Log.d("JellyfinPlayer", "   Original First: platform renderer first, FFmpeg fallback, decoder fallback ENABLED")
        }
    }
    
    // Configure track selector with better track selection
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    // Audio preference - use system default language
                    .setPreferredAudioLanguage(java.util.Locale.getDefault().language)
                    // Subtitle preferences - disable ALL auto-selection but allow manual control
                    .setSelectUndeterminedTextLanguage(false)  // Don't auto-select unknown language subs
                    .setDisabledTextTrackSelectionFlags(C.SELECTION_FLAG_FORCED or C.SELECTION_FLAG_DEFAULT)  // Disable forced AND default auto-selection
                    // ❌ DO NOT use setTrackTypeDisabled - it prevents "None" from working in ExoPlayer UI
                    // Only select subtitles explicitly chosen by user or saved preference
                    .setPreferredTextLanguage(null)  // No auto language preference
                    .setPreferredTextRoleFlags(0)  // No role-based auto-selection
                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED or C.SELECTION_FLAG_DEFAULT)  // Ignore forced/default flags completely
            )
        }
    }
    
    // Configure audio attributes for media playback
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MOVIE)
        .build()
    
    val player = remember {
        // Configure LoadControl to prevent OOM on high-bitrate content (especially HLS H.265)
        val loadControl = if (settings.minimalBuffer4K) {
            // "Minimal" but robust buffering
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2000,   // minBufferMs - 2 seconds
                    50000,  // maxBufferMs - 50 seconds
                    1000,   // bufferForPlaybackMs - 1 second
                    2000    // bufferForPlaybackAfterRebufferMs - 2 seconds
                )
                .setTargetBufferBytes(50 * 1024 * 1024) // 50MB max buffer (reduced from 100MB to prevent OOM)
                .build()
        } else {
            // Robust buffering for high bitrate content
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    10000,  // minBufferMs - 10 seconds
                    120000, // maxBufferMs - 120 seconds
                    5000,   // bufferForPlaybackMs - 5 seconds
                    10000   // bufferForPlaybackAfterRebufferMs - 10 seconds
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setTargetBufferBytes(128 * 1024 * 1024) // 128MB max buffer (reduced to prevent OOM)
                .build()
        }
            
            ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setAudioAttributes(audioAttributes, true)
                .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
                .setLoadControl(loadControl)
                .setSeekBackIncrementMs(15000)
                .setSeekForwardIncrementMs(15000)
                .build()
                .also { 
                if (settings.minimalBuffer4K) {
                    Log.d("JellyfinPlayer", "Created player with minimal buffering (50MB limit) for 4K content")
        } else {
                    Log.d("JellyfinPlayer", "Created player with standard buffering (250MB limit)")
                }
                    Log.d("JellyfinPlayer", "Original First: platform renderer first, FFmpeg fallback, decoder fallback enabled")
        }
    }
    
    // Create MediaSession to handle system media keys and focus
    DisposableEffect(player) {
        val mediaSession = MediaSession.Builder(context, player)
            .setId("VeloraVideoSession_${item.Id}")
            .build()
            
        Log.d("JellyfinPlayer", "MediaSession created for item ${item.Id}")
        
        onDispose {
            Log.d("JellyfinPlayer", "Releasing MediaSession")
            mediaSession.release()
        }
    }
    
    // Lifecycle Observer to pause playback when app goes to background (Home button)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                Log.d("JellyfinPlayer", "Lifecycle PAUSE/STOP detected. Pausing player.")
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    val glSurfaceViewRef = remember { mutableStateOf<GLVideoSurfaceView?>(null) }
    var mediaUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var playerInitialized by remember { mutableStateOf(false) }

    // Change quality with the existing player instance. Jellyfin negotiates the selected
    // preset; Original keeps static=true and therefore does not impose artificial limits.
    LaunchedEffect(playbackQuality) {
        if (playerInitialized && itemDetails != null) {
            val sourceId = itemDetails?.MediaSources?.firstOrNull()?.Id
            val position = player.currentPosition
            val url = withContext(Dispatchers.IO) {
                apiService.getVideoPlaybackUrl(
                    itemId = item.Id,
                    mediaSourceId = sourceId,
                    preserveQuality = playbackQuality == PlaybackQuality.ORIGINAL,
                    quality = playbackQuality
                )
            }
            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(url), position)
            player.prepare()
            player.play()
            Log.d("JellyfinPlayer", "Quality changed in-place to ${playbackQuality.label}")
        }
    }
    var progressReportingJob by remember { mutableStateOf<Job?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    // itemDetails is declared earlier for codec detection
    var hasSeekedToResume by remember { mutableStateOf(false) } // Track if we've already seeked to resume position
    var hasRetriedWithoutRange by remember { mutableStateOf(false) } // Track if we've retried without range requests for 416 errors
    var hasRetriedWithHls by remember { mutableStateOf(false) } // Track if we've retried with HLS for parser errors
    var currentMediaSource by remember { mutableStateOf<MediaSource?>(null) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var settingsMenuInitialLevel by remember { mutableStateOf("main") } // Track which submenu to open to
    var showControls by remember { mutableStateOf(false) } // Custom Compose controls overlay
    var currentPosition by remember { mutableStateOf(0L) } // Current playback position in ms
    var duration by remember { mutableStateOf(0L) } // Total duration in ms
    var currentSubtitleIndex by remember { mutableStateOf<Int?>(subtitleStreamIndex) }
    var lastSelectedSubtitleIndex by remember { mutableStateOf<Int?>(subtitleStreamIndex) } // Track last selected subtitle from controller
    var hasAppliedInitialSubtitlePreference by remember { mutableStateOf(false) } // Track if we've applied the saved preference once
    var hasRegisteredTracks by remember { mutableStateOf(false) } // Track if we've registered ExoPlayer tracks with SubtitleMapper
    var currentAudioIndex by remember { mutableStateOf<Int?>(storedAudioPreference) }
    var lastSelectedAudioIndex by remember { mutableStateOf<Int?>(storedAudioPreference) } // Track last selected audio from controller
    var is4KContent by remember { mutableStateOf(false) } // Track if current content is 4K
    // Store subtitle streams list for composite key registration in onTracksChanged
    var jellyfinSubtitleStreams by remember { mutableStateOf<List<MediaStream>>(emptyList()) }
    
    // Downloaded subtitles from OpenSubtitles
    var downloadedSubtitles by remember { mutableStateOf<List<com.flex.elefin.subtitles.DownloadedSubtitle>>(emptyList()) }
    var nextEpisodeId by remember { mutableStateOf<String?>(null) } // Next episode ID for autoplay
    var nextEpisodeDetails by remember { mutableStateOf<JellyfinItem?>(null) } // Next episode details
    var currentAspectMode by remember { mutableStateOf(AspectMode.FIT) } // Picture mode / aspect ratio
    var videoResolution by remember { mutableStateOf("") } // Current video resolution string
    
    
    // Portrait mobile detail state variables
    var seriesDetails by remember { mutableStateOf<JellyfinItem?>(null) }
    var seasons by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var selectedSeasonIndex by remember { mutableStateOf(0) }
    var episodes by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    LaunchedEffect(itemDetails, selectedSeasonIndex) {
        val details = itemDetails ?: return@LaunchedEffect
        if (details.Type == "Episode" && details.SeriesId != null) {
            withContext(Dispatchers.IO) {
                try {
                    if (seriesDetails == null) {
                        seriesDetails = apiService.getItemDetails(details.SeriesId)
                    }
                    if (seasons.isEmpty()) {
                        val fetchedSeasons = apiService.getSeasons(details.SeriesId)
                        seasons = fetchedSeasons
                        // Find matching season index
                        val currentSeasonNum = details.ParentIndexNumber
                        val initialIndex = fetchedSeasons.indexOfFirst { it.IndexNumber == currentSeasonNum }
                        if (initialIndex >= 0) {
                            selectedSeasonIndex = initialIndex
                        }
                    }
                    if (seasons.isNotEmpty() && selectedSeasonIndex < seasons.size) {
                        isLoadingEpisodes = true
                        val currentSeasonId = seasons[selectedSeasonIndex].Id
                        episodes = apiService.getEpisodes(details.SeriesId, currentSeasonId)
                        isLoadingEpisodes = false
                    }
                } catch (e: Exception) {
                    Log.e("JellyfinPlayer", "Error fetching seasons/episodes for player layout", e)
                }
            }
        }
    }


    // ===================================================================================
    // CLEAN AUTOPLAY STATE - Single source of truth
    // ===================================================================================
    var showNextUpOverlay by remember { mutableStateOf(false) }
    var autoplayCountdown by remember { mutableStateOf(settings.autoplayCountdownSeconds) }
    var autoplayCancelled by remember { mutableStateOf(false) }
    var isAutoPlayingNext by remember { mutableStateOf(false) } // Guard against double triggers
    var countdownJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val playClickedEpisode: (JellyfinItem, Long) -> Unit = playClickedEpisode@{ clickedEp, resumePos ->
        if (isAutoPlayingNext) return@playClickedEpisode
        val activity = context as? android.app.Activity
        if (activity != null && !activity.isFinishing) {
            isAutoPlayingNext = true
            showNextUpOverlay = false
            countdownJob?.cancel()
            countdownJob = null
            
            progressReportingJob?.cancel()
            progressReportingJob = null
            
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                Log.w("JellyfinPlayer", "Error stopping player on episode click", e)
            }
            
            val intent = com.flex.elefin.JellyfinVideoPlayerActivity.createIntent(
                context = activity,
                itemId = clickedEp.Id,
                resumePositionMs = resumePos,
                subtitleStreamIndex = null,
                audioStreamIndex = null
            )
            activity.startActivity(intent)
            activity.finish()
        }
    }
    
    // ===================================================================================
    // AUTOPLAY HELPER FUNCTION - Single exit point for starting next episode
    // Based on Jellyfin Android TV approach: Stop current player FIRST, then start new activity
    // ===================================================================================
    val startNextEpisode: () -> Unit = startNextEpisode@{
        val nextEp = nextEpisodeDetails
        if (isAutoPlayingNext || nextEp == null) {
            Log.d("JellyfinPlayer", "🎬 Ignoring autoplay trigger (already playing=$isAutoPlayingNext, nextEp=${nextEp?.Name})")
            return@startNextEpisode
        }
        
        val activity = context as? android.app.Activity
        if (activity == null || activity.isFinishing) {
            Log.e("JellyfinPlayer", "🎬 ERROR: Activity is null or finishing")
            return@startNextEpisode
        }
        
        isAutoPlayingNext = true
        showNextUpOverlay = false
        countdownJob?.cancel()
        countdownJob = null
        
        Log.d("JellyfinPlayer", "🎬 ===== STARTING NEXT EPISODE (Jellyfin TV approach) =====")
        Log.d("JellyfinPlayer", "🎬 Next: ${nextEp.Name} (ID: ${nextEp.Id})")
        
        // Step 1: Cancel progress reporting
        progressReportingJob?.cancel()
        progressReportingJob = null
        Log.d("JellyfinPlayer", "🎬 Step 1: Progress reporting cancelled")
        
        // Step 2: Stop and release current player COMPLETELY (like Jellyfin TV does)
        val currentPositionMs = try { player.currentPosition } catch (e: Exception) { 0L }
        val positionTicks = currentPositionMs * 10_000L
        try {
            player.stop()
            player.release()
            Log.d("JellyfinPlayer", "🎬 Step 2: Player stopped and released")
        } catch (e: Exception) {
            Log.w("JellyfinPlayer", "🎬 Step 2: Error stopping player", e)
        }
        
        // Step 3: Report playback stopped (fire and forget)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                apiService.reportPlaybackStopped(item.Id, positionTicks)
                apiService.markAsWatched(item.Id)
                Log.d("JellyfinPlayer", "🎬 Step 3: Reported playback stopped")
            } catch (e: Exception) {
                Log.w("JellyfinPlayer", "🎬 Step 3: Error reporting", e)
            }
        }
        
        // Step 4: Create and start intent for next episode
        // We start the new activity WITHOUT FLAG_ACTIVITY_NEW_TASK to maintain the same task
        // and back stack (pointing back to the details screen).
        val intent = com.flex.elefin.JellyfinVideoPlayerActivity.createIntent(
            context = activity,
            itemId = nextEp.Id,
            resumePositionMs = 0L,
            subtitleStreamIndex = null,
            audioStreamIndex = null
        )
        
        Log.d("JellyfinPlayer", "🎬 Step 4: Starting next episode activity...")
        activity.startActivity(intent)
        
        // Step 5: Finish current activity
        // Finishing after starting the next one ensures the back stack remains correct:
        // [DetailsActivity] -> [New PlayerActivity]
        Log.d("JellyfinPlayer", "🎬 Step 5: Finishing current activity...")
        activity.finish()
        
        Log.d("JellyfinPlayer", "🎬 ===== AUTOPLAY COMPLETE =====")
    }
    
    // ===================================================================================
    // AUTOPLAY COUNTDOWN FUNCTION - Owns the transition, fires exactly once
    // Must be defined right after startNextEpisode so it can reference it
    // ===================================================================================
    val startAutoplayCountdown: () -> Unit = {
        countdownJob?.cancel()
        
        countdownJob = scope.launch {
            val seconds = settings.autoplayCountdownSeconds
            Log.d("JellyfinPlayer", "⏱️ Starting ${seconds}s countdown...")
            
            for (i in seconds downTo 1) {
                // Check BEFORE updating state - exit early if conditions changed
                if (autoplayCancelled || isAutoPlayingNext) {
                    Log.d("JellyfinPlayer", "⛔ Countdown aborted (cancelled=$autoplayCancelled, autoplaying=$isAutoPlayingNext)")
                    showNextUpOverlay = false
                    return@launch
                }
                
                autoplayCountdown = i
                Log.d("JellyfinPlayer", "⏱️ Countdown: $i")
                delay(1_000)
            }
            
            // Final check before triggering
            if (autoplayCancelled || isAutoPlayingNext) {
                Log.d("JellyfinPlayer", "⛔ Countdown finished but conditions changed, not starting")
                return@launch
            }
            
            // 🔥 COUNTDOWN FINISHED - START NEXT EPISODE
            autoplayCountdown = 0
            Log.d("JellyfinPlayer", "⏱️ Countdown finished — starting next episode!")
            startNextEpisode()
        }
    }
    
    // Skip intro/credits state
    var skipMarkers by remember { mutableStateOf(SkipMarkers()) }
    var showSkipIntroButton by remember { mutableStateOf(false) }
    var showSkipCreditsButton by remember { mutableStateOf(false) }
    val skipIntroEnabled = remember { settings.skipIntroEnabled }
    val skipCreditsEnabled = remember { settings.skipCreditsEnabled }

    // Load downloaded subtitles from OpenSubtitles
    LaunchedEffect(item.Id) {
        downloadedSubtitles = com.flex.elefin.subtitles.OpenSubtitlesApi.getDownloadedSubtitles(context, item.Id)
        Log.d("JellyfinPlayer", "📁 Loaded ${downloadedSubtitles.size} downloaded subtitle(s) for item ${item.Id}")
    }
    
    // Fetch skip markers for intro/credits (only for episodes)
    LaunchedEffect(item.Id, apiService) {
        if (item.Type == "Episode" && (skipIntroEnabled || skipCreditsEnabled)) {
            withContext(Dispatchers.IO) {
                try {
                    val markers = apiService.getMediaSegments(item.Id)
                    skipMarkers = markers
                    Log.d("JellyfinPlayer", "Skip markers loaded: intro=${markers.introStartMs}-${markers.introEndMs}ms, credits=${markers.creditsStartMs}ms")
                } catch (e: Exception) {
                    Log.d("JellyfinPlayer", "Skip markers not available: ${e.message}")
                }
            }
        }
    }
    
    // Fetch item details and prepare video URL
    // Quality changes are handled by the dedicated in-place reload effect above.
    // Keeping quality out of this metadata/track effect avoids a second reload and
    // prevents visible pauses or duplicate player preparation.
    LaunchedEffect(item.Id, apiService, subtitleStreamIndex) {
        withContext(Dispatchers.IO) {
            try {
                // Get full item details with MediaSources
                val details = apiService.getItemDetails(item.Id)
                if (details != null) {
                    itemDetails = details
                    // Get the first media source
                    val mediaSource = details.MediaSources?.firstOrNull()
                    val mediaSourceId = mediaSource?.Id

                    // Original First: classify HDR from Jellyfin HDR/Dolby Vision metadata, not from a 4K+HEVC guess.
                    val videoStream = mediaSource?.MediaStreams?.firstOrNull { it.Type == "Video" }
                    val width = videoStream?.Width ?: 0
                    val height = videoStream?.Height ?: 0
                    val is4KOrHigher = width >= 3840 || height >= 2160
                    val isNativeHdr = com.flex.elefin.player.OriginalQualityPolicy.isHdr(videoStream)
                    val hdrLabel = com.flex.elefin.player.OriginalQualityPolicy.hdrLabel(videoStream)
                    val isHDROrHighQuality = isNativeHdr || is4KOrHigher
                    
                    // Store 4K status for buffering control
                    is4KContent = is4KOrHigher
                    
                    // Detect if audio codec is unsupported by Android (requires transcoding)
                    val audioStream = mediaSource?.MediaStreams?.firstOrNull { it.Type == "Audio" }
                    val audioCodec = audioStream?.Codec?.lowercase() ?: ""
                    // TrueHD, DTS-HD, and other lossless/high-end audio codecs aren't supported by Android AudioTrack
                    val isUnsupportedAudio = audioCodec.contains("truehd", ignoreCase = true) ||
                                            audioCodec.contains("dts-hd", ignoreCase = true) ||
                                            audioCodec.contains("dtshd", ignoreCase = true) ||
                                            audioCodec.contains("dtsx", ignoreCase = true) ||
                                            audioCodec.contains("atmos", ignoreCase = true) && audioCodec.contains("truehd", ignoreCase = true)
                    
                    // Check if AAC to AC3 transcoding is enabled
                    val shouldTranscodeAacToAc3 = settings.transcodeAacToAc3 && audioCodec.contains("aac", ignoreCase = true)
                    // Original First: do not pre-transcode TrueHD/DTS-HD.
                    // Let Android passthrough/MediaCodec or the bundled FFmpeg renderer try first.
                    // Server-side audio conversion is only requested for an explicit user option.
                    val needsAudioTranscoding = shouldTranscodeAacToAc3
                    val targetAudioCodec = if (shouldTranscodeAacToAc3) "ac3" else null
                    
                    if (isNativeHdr) {
                        Log.d("JellyfinPlayer", "Original First: native $hdrLabel detected (${videoStream?.Codec}, ${width}x${height}, ${videoStream?.BitDepth ?: 0}-bit) - preserving original video")
                    } else if (is4KOrHigher) {
                        Log.d("JellyfinPlayer", "Original First: 4K SDR/high-quality source detected (${videoStream?.Codec}, ${width}x${height}) - preserving original video")
                    }
                    if (isUnsupportedAudio) {
                        Log.d("JellyfinPlayer", "Original First: ${audioStream?.Codec} detected - preserving original audio for local passthrough/MediaCodec/FFmpeg fallback")
                    }
                    if (shouldTranscodeAacToAc3) {
                        Log.d("JellyfinPlayer", "AAC to AC3 transcoding enabled - transcoding audio from AAC to AC3 for universal device compatibility")
                    }
                    
                    // USER SELECTED SUBTITLE OVERRIDE
                    // Check if user explicitly selected a subtitle (from series/movie page or Jellyfin UI)
                    val selectedSubtitleStream = if (subtitleStreamIndex != null) {
                        details?.MediaSources?.firstOrNull()?.MediaStreams
                            ?.find { it.Type == "Subtitle" && it.Index == subtitleStreamIndex }
                    } else {
                        null
                    }
                    
                    // HLS (master.m3u8) does NOT support external subtitles via SubtitleConfiguration!
                    // This is a known Media3 limitation - ExoPlayer ignores SubtitleConfiguration for HLS streams
                    // Solution: If user selected an EXTERNAL subtitle, force direct streaming (no HLS)
                    val userSelectedExternalSubtitle = selectedSubtitleStream?.IsExternal == true
                    val forceDirectStreamForSubtitles = userSelectedExternalSubtitle && needsAudioTranscoding
                    
                    if (userSelectedExternalSubtitle) {
                        Log.d("JellyfinPlayer", "📌 USER SELECTED EXTERNAL SUBTITLE")
                        Log.d("JellyfinPlayer", "   Selected: ${selectedSubtitleStream?.DisplayTitle ?: selectedSubtitleStream?.Language}")
                        Log.d("JellyfinPlayer", "   Index: ${selectedSubtitleStream?.Index}, IsExternal: ${selectedSubtitleStream?.IsExternal}")
                        
                        if (needsAudioTranscoding) {
                            Log.w("JellyfinPlayer", "⚠️ SUBTITLE PRIORITY MODE ACTIVATED")
                            Log.w("JellyfinPlayer", "   External subtitle selected + audio transcoding needed")
                            Log.w("JellyfinPlayer", "   Disabling HLS transcoding to use direct streaming")
                            Log.w("JellyfinPlayer", "   WHY: HLS playlists do NOT include external subtitles")
                            Log.w("JellyfinPlayer", "   WHY: ExoPlayer ignores SubtitleConfiguration for HLS (Media3 limitation)")
                            Log.w("JellyfinPlayer", "   RESULT: Selected subtitle will work, audio codec may not be optimal")
                            Log.w("JellyfinPlayer", "   ALTERNATIVE: Use MPV player for both subtitle + audio transcoding support")
                        } else {
                            Log.d("JellyfinPlayer", "   ✅ Direct streaming - external subtitle will load successfully")
                        }
                    }

                    // Generate video playback URL
                    // If user selected external subtitle, disable HLS transcoding to force direct streaming
                    val effectiveNeedsTranscoding = if (forceDirectStreamForSubtitles) false else needsAudioTranscoding
                    val effectiveAudioCodec = if (forceDirectStreamForSubtitles) null else targetAudioCodec
                    
                    // Check if server-side transcoding is enabled
                    val serverTranscodingEnabled = settings.serverTranscodingEnabled
                    val transcodeAV1Setting = settings.transcodeAV1
                    val transcodeHEVCSetting = settings.transcodeHEVC
                    val transcodeTargetCodec = settings.transcodeTargetCodec
                    val transcodeMaxBitrate = settings.transcodeMaxBitrateMbps
                    
                    // Detect video codec for transcoding decision
                    val videoCodecName = videoStream?.Codec?.lowercase() ?: ""
                    val isAV1Video = videoCodecName.contains("av1") || videoCodecName.contains("av01")
                    val isHEVCVideo = videoCodecName.contains("hevc") || videoCodecName.contains("h265") || videoCodecName.contains("h.265")
                    
                    // Determine if we should request server-side transcoding
                    val shouldRequestTranscoding = serverTranscodingEnabled && (
                        (transcodeAV1Setting && isAV1Video) ||
                        (transcodeHEVCSetting && isHEVCVideo)
                    )
                    
                    val videoUrl = if (shouldRequestTranscoding && !forceDirectStreamForSubtitles) {
                        Log.d("JellyfinPlayer", "🔄 SERVER-SIDE TRANSCODING ENABLED")
                        Log.d("JellyfinPlayer", "   Source codec: $videoCodecName")
                        Log.d("JellyfinPlayer", "   Target codec: $transcodeTargetCodec @ ${transcodeMaxBitrate}Mbps")
                        Log.d("JellyfinPlayer", "   Reason: ${if (isAV1Video) "AV1" else "HEVC"} transcoding requested")
                        
                        apiService.getTranscodedVideoUrl(
                            itemId = item.Id,
                            mediaSourceId = mediaSourceId,
                            subtitleStreamIndex = null,
                            targetVideoCodec = transcodeTargetCodec,
                            maxBitrateMbps = transcodeMaxBitrate,
                            audioCodec = "aac"
                        )
                    } else {
                        apiService.getVideoPlaybackUrl(
                            itemId = item.Id,
                            mediaSourceId = mediaSourceId,
                            subtitleStreamIndex = null,
                            preserveQuality = isHDROrHighQuality,
                            transcodeAudio = effectiveNeedsTranscoding, // Disabled if subtitles exist
                            audioCodec = effectiveAudioCodec,
                            quality = playbackQuality
                        )
                    }
                    Log.d("JellyfinPlayer", "Video URL: $videoUrl")
                    mediaUrl = videoUrl
                    
                    // Check for next episode if this is an episode
                    // Use the simpler StartIndex approach: /Shows/{seriesId}/Episodes?StartIndex={currentIndex + 1}&Limit=1
                    if (details.Type == "Episode") {
                        Log.d("JellyfinPlayer", "Episode detected. NextEpisodeId from API: ${details.NextEpisodeId}")
                        Log.d("JellyfinPlayer", "Episode info: SeriesId=${details.SeriesId}, Season=${details.ParentIndexNumber}, Episode=${details.IndexNumber}")
                        
                        // Try to get next episode ID from API response first
                        var foundNextEpisode: JellyfinItem? = null
                        
                        if (details.NextEpisodeId != null) {
                            // API provided NextEpisodeId, fetch the episode
                            val nextDetails = apiService.getItemDetails(details.NextEpisodeId)
                            if (nextDetails != null) {
                                foundNextEpisode = nextDetails
                                Log.d("JellyfinPlayer", "✅ Found next episode via NextEpisodeId: ${nextDetails.Name}")
                            }
                        }
                        
                        // If NextEpisodeId is not available, find next episode in same season first
                        if (foundNextEpisode == null && details.SeriesId != null && details.IndexNumber != null && details.ParentIndexNumber != null) {
                            Log.d("JellyfinPlayer", "NextEpisodeId not available, finding next episode in same season...")
                            Log.d("JellyfinPlayer", "Current: S${details.ParentIndexNumber}E${details.IndexNumber}")
                            try {
                                // First, get the seasons to find the current season's ID
                                    val seasons = apiService.getSeasons(details.SeriesId)
                                    val currentSeason = seasons.firstOrNull { it.IndexNumber == details.ParentIndexNumber }
                                
                                if (currentSeason != null) {
                                    Log.d("JellyfinPlayer", "Found current season: ${currentSeason.Name} (ID: ${currentSeason.Id})")
                                    
                                    // Try to get next episode in the SAME season
                                    foundNextEpisode = apiService.getNextEpisodeInSeason(
                                        seriesId = details.SeriesId,
                                        seasonId = currentSeason.Id,
                                        currentEpisodeIndex = details.IndexNumber,
                                        currentSeasonNumber = details.ParentIndexNumber
                                    )
                                    
                                    if (foundNextEpisode != null) {
                                        Log.d("JellyfinPlayer", "✅ Found next episode in same season: S${foundNextEpisode.ParentIndexNumber}E${foundNextEpisode.IndexNumber} - ${foundNextEpisode.Name}")
                                    } else {
                                        // No more episodes in current season, try next season's first episode
                                        Log.d("JellyfinPlayer", "No more episodes in S${details.ParentIndexNumber}, checking next season...")
                                    val nextSeason = seasons.firstOrNull { 
                                            it.IndexNumber == details.ParentIndexNumber + 1 
                                    }
                                    
                                    if (nextSeason != null) {
                                            Log.d("JellyfinPlayer", "Found next season: ${nextSeason.Name} (ID: ${nextSeason.Id})")
                                            // Get first episode of next season (episode index 0, looking for episode 1)
                                            foundNextEpisode = apiService.getNextEpisodeInSeason(
                                                seriesId = details.SeriesId,
                                                seasonId = nextSeason.Id,
                                                currentEpisodeIndex = 0, // Looking for episode 1
                                                currentSeasonNumber = nextSeason.IndexNumber ?: (details.ParentIndexNumber + 1)
                                            )
                                            
                                            // If that didn't work, try getting all episodes from next season
                                            if (foundNextEpisode == null) {
                                                val nextSeasonEpisodes = apiService.getEpisodes(details.SeriesId, nextSeason.Id)
                                                foundNextEpisode = nextSeasonEpisodes.firstOrNull()
                                        }
                                        
                                        if (foundNextEpisode != null) {
                                                Log.d("JellyfinPlayer", "✅ Found first episode of next season: S${foundNextEpisode.ParentIndexNumber}E${foundNextEpisode.IndexNumber} - ${foundNextEpisode.Name}")
                                        }
                                        } else {
                                            Log.d("JellyfinPlayer", "No next season found (this is the last episode of the series)")
                                    }
                                    }
                                } else {
                                    Log.e("JellyfinPlayer", "Could not find current season with IndexNumber=${details.ParentIndexNumber}")
                                }
                            } catch (e: Exception) {
                                Log.e("JellyfinPlayer", "Error finding next episode", e)
                                e.printStackTrace()
                            }
                        }
                        
                        if (foundNextEpisode != null) {
                            nextEpisodeId = foundNextEpisode.Id
                            nextEpisodeDetails = foundNextEpisode
                            Log.d("JellyfinPlayer", "✅✅✅ Next episode resolved: ${foundNextEpisode.Name}, ID: ${foundNextEpisode.Id}")
                            Log.d("JellyfinPlayer", "✅ Next episode IndexNumber: ${foundNextEpisode.IndexNumber}, Season: ${foundNextEpisode.ParentIndexNumber}")
                        } else {
                            Log.d("JellyfinPlayer", "No next episode found (this might be the last episode)")
                        }
                    }
                    
                    isLoading = false
                } else {
                    Log.e("JellyfinPlayer", "Failed to fetch item details")
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("JellyfinPlayer", "Error preparing video", e)
                isLoading = false
            }
        }
    }

    // Initialize player when media URL is ready
    LaunchedEffect(mediaUrl) {
        if (mediaUrl != null && !playerInitialized) {
            withContext(Dispatchers.Main) {
                try {
                    
                    // Get authentication headers
                    val headers = apiService.getVideoRequestHeaders()

                    // Create HTTP data source factory with headers
                    // For 416 errors, we'll retry with range requests disabled
                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent("Jellyfin Android TV")
                        .setAllowCrossProtocolRedirects(true)
                    
                    // Set headers using setDefaultRequestProperties
                    val headersMap = headers.toMutableMap()
                    httpDataSourceFactory.setDefaultRequestProperties(headersMap)
                    

                    // Create data source factory
                    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

                    // Store mediaUrl in local variable for smart cast
                    val currentMediaUrl = mediaUrl ?: return@withContext
                    
                    // Detect if URL is HLS (ends with .m3u8 or contains master.m3u8)
                    val isHlsUrl = currentMediaUrl.contains(".m3u8", ignoreCase = true)

                    // Load ALL external subtitles into MediaItem (Jellyfin AndroidTV approach)
                    // This allows ExoPlayer to show the subtitle button and let users switch between them
                    val mediaItem = if (itemDetails != null) {
                        try {
                            val mediaSourceIdForSubtitle = itemDetails?.MediaSources?.firstOrNull()?.Id ?: item.Id
                            
                            // Get ALL subtitle streams from Jellyfin
                            val allSubtitleStreams = itemDetails?.MediaSources?.firstOrNull()?.MediaStreams
                                ?.filter { it.Type == "Subtitle" && it.Index != null }
                                ?: emptyList()
                            
                            Log.d("JellyfinPlayer", "Found ${allSubtitleStreams.size} subtitle stream(s) from Jellyfin")
                            
                            // Store subtitle streams for composite key registration in onTracksChanged
                            jellyfinSubtitleStreams = allSubtitleStreams
                            
                            // Reset SubtitleMapper for new playback session
                            com.flex.elefin.player.SubtitleMapper.reset()
                            
                            // Create SubtitleConfiguration for each subtitle using SubtitleMapper
                            // Uses COMPOSITE KEY approach (production-safe, used by Plex/Emby/Jellyfin TV)
                            val subtitleConfigurations = allSubtitleStreams.map { stream ->
                                try {
                                    val subtitleIndex = stream.Index ?: return@map null
                                    val subtitleUrl = apiService.buildJellyfinSubtitleUrl(
                                        itemId = item.Id,
                                        mediaSourceId = mediaSourceIdForSubtitle,
                                        streamIndex = subtitleIndex,
                                        isExternal = stream.IsExternal == true,
                                        codec = stream.Codec,
                                        path = stream.Path
                                    )
                                    
                                    Log.d("JellyfinPlayer", "Adding subtitle ${stream.Index}: ${stream.DisplayTitle ?: stream.Language} (${stream.Codec}) - IsExternal=${stream.IsExternal}")
                                    
                                    // Use SubtitleMapper to create configuration with position tracking
                                    // ⚠️ CRITICAL: Use actual Jellyfin index, NOT sequential position!
                                    // This ensures SubtitleMapper correctly maps Jellyfin index → ExoPlayer track
                                    com.flex.elefin.player.SubtitleMapper.buildSubtitleConfiguration(
                                        context = context,
                                        apiService = apiService,
                                        itemId = item.Id,
                                        mediaSourceId = mediaSourceIdForSubtitle ?: item.Id,
                                        stream = stream,
                                        positionIndex = subtitleIndex  // Use actual JF index, not sequential!
                                    )
                                } catch (e: Exception) {
                                    Log.w("JellyfinPlayer", "Failed to create subtitle config for index ${stream.Index}: ${e.message}")
                                    null
                                }
                            }.filterNotNull()
                            
                            Log.d("JellyfinPlayer", "Successfully created ${subtitleConfigurations.size} Jellyfin subtitle configuration(s)")
                            subtitleConfigurations.forEachIndexed { index, config ->
                                Log.d("JellyfinPlayer", "  [$index] ${config.uri}")
                                Log.d("JellyfinPlayer", "       Lang: ${config.language}, MIME: ${config.mimeType}, Label: ${config.label}")
                            }
                            
                            // ⭐ ADD DOWNLOADED OPENSUBTITLES (if any exist for this item)
                            val downloadedSubtitles = com.flex.elefin.subtitles.OpenSubtitlesApi.getDownloadedSubtitles(context, item.Id)
                            val downloadedSubtitleConfigs = downloadedSubtitles.mapNotNull { downloadedSub ->
                                try {
                                    Log.d("JellyfinPlayer", "📁 Adding downloaded subtitle: ${downloadedSub.fileName}")
                                    com.flex.elefin.player.SubtitleMapper.buildLocalSubtitleConfiguration(
                                        filePath = downloadedSub.filePath,
                                        language = downloadedSub.language,
                                        label = "${com.flex.elefin.subtitles.SubtitleLanguages.getDisplayName(downloadedSub.language)} (Downloaded)"
                                    )
                                } catch (e: Exception) {
                                    Log.w("JellyfinPlayer", "Failed to add downloaded subtitle ${downloadedSub.fileName}: ${e.message}")
                                    null
                                }
                            }
                            
                            if (downloadedSubtitleConfigs.isNotEmpty()) {
                                Log.d("JellyfinPlayer", "✅ Added ${downloadedSubtitleConfigs.size} downloaded subtitle(s) from OpenSubtitles")
                            }
                            
                            // Combine Jellyfin subtitles + downloaded OpenSubtitles
                            val allSubtitleConfigs = subtitleConfigurations + downloadedSubtitleConfigs
                            Log.d("JellyfinPlayer", "Total subtitle configurations: ${allSubtitleConfigs.size} (${subtitleConfigurations.size} Jellyfin + ${downloadedSubtitleConfigs.size} downloaded)")
                            
                            // Create MediaItem with ALL subtitle configurations
                            if (allSubtitleConfigs.isNotEmpty()) {
                                MediaItem.Builder()
                                    .setUri(Uri.parse(currentMediaUrl))
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(item.Name)
                                            .build()
                                    )
                                    .setSubtitleConfigurations(allSubtitleConfigs)
                                    .build().also {
                                        Log.d("JellyfinPlayer", "✅ MediaItem created with ${allSubtitleConfigs.size} subtitle configuration(s)")
                                    }
                            } else {
                                Log.d("JellyfinPlayer", "No valid subtitle configurations - creating MediaItem without subtitles")
                                MediaItem.Builder()
                                    .setUri(Uri.parse(currentMediaUrl))
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(item.Name)
                                            .build()
                                    )
                                    .build()
                            }
                        } catch (e: Exception) {
                            Log.e("JellyfinPlayer", "❌ Error creating MediaItem with subtitles: ${e.message}", e)
                            Log.e("JellyfinPlayer", "   Playing video without subtitles")
                            MediaItem.fromUri(Uri.parse(currentMediaUrl))
                        }
                    } else {
                        Log.d("JellyfinPlayer", "No item details - creating MediaItem without subtitles")
                        MediaItem.fromUri(Uri.parse(currentMediaUrl))
                    }
                    
                    // Create media source from MediaItem - use DefaultMediaSourceFactory for proper subtitle support
                    // DefaultMediaSourceFactory automatically detects the media type and handles subtitles
                    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    val mediaSource: MediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                    Log.d("JellyfinPlayer", "Created MediaSource using DefaultMediaSourceFactory")

                    // Set media source
                    player.setMediaSource(mediaSource)

                    // Store media source for potential retry
                    currentMediaSource = mediaSource

                    // Handle player lifecycle
                    player.addListener(object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                            Log.d("JellyfinPlayer", "📺 Video size changed: ${videoSize.width}x${videoSize.height}")
                            
                            // Update GL surface with video dimensions for proper aspect ratio
                            if (videoSize.width > 0 && videoSize.height > 0) {
                                glSurfaceViewRef.value?.setVideoSize(videoSize.width, videoSize.height)
                                
                                // Update resolution string with HDR/SDR info
                                val format = player.videoFormat
                                val isHdr = ColorInfo.isTransferHdr(format?.colorInfo)
                                val hdrTag = if (isHdr) "HDR" else "SDR"
                                
                                val resBase = when {
                                    videoSize.width >= 3840 || videoSize.height >= 2160 -> "4K"
                                    videoSize.width >= 2560 || videoSize.height >= 1440 -> "1440p"
                                    videoSize.width >= 1920 || videoSize.height >= 1080 -> "1080p"
                                    videoSize.width >= 1280 || videoSize.height >= 720 -> "720p"
                                    videoSize.width >= 854 || videoSize.height >= 480 -> "480p"
                                    else -> "${videoSize.height}p"
                                }
                                videoResolution = "$resBase $hdrTag"
                                Log.d("JellyfinPlayer", "📺 Detected resolution: $videoResolution (${videoSize.width}x${videoSize.height})")
                            }
                            
                            // Also check codec from videoFormat when size changes
                            val format = player.videoFormat
                            val codec = format?.codecs ?: format?.sampleMimeType ?: "unknown"
                            Log.d("JellyfinPlayer", "📺 Video format: codec=$codec, mime=${format?.sampleMimeType}")
                            
                            if (videoSize.width == 0 || videoSize.height == 0) {
                                Log.w("JellyfinPlayer", "⚠️ Video size is 0x0 - video may not be rendering!")
                                
                                // If AV1 was detected and we have black screen, this confirms the issue
                                if (runtimeAV1Detected) {
                                    Log.e("JellyfinPlayer", "❌ CONFIRMED: AV1 + current surface = no video output")
                                    Log.e("JellyfinPlayer", "❌ Solution: Disable GL Enhancements in Settings")
                                }
                            }
                        }
                        
                        override fun onRenderedFirstFrame() {
                            Log.d("JellyfinPlayer", "🎬 First video frame rendered!")
                            if (runtimeAV1Detected) {
                                Log.d("JellyfinPlayer", "✅ AV1 video is rendering successfully!")
                            }
                        }
                        
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e("JellyfinPlayer", "Player error: ${error.message}", error)
                            Log.e("JellyfinPlayer", "Error type: ${error.errorCode}, Cause: ${error.cause?.javaClass?.simpleName}")
                            
                            // Check for 10-bit AV1 specific error from libgav1
                            val causeMessage = error.cause?.message ?: ""
                            val is10BitAV1Error = causeMessage.contains("High bit depth") && 
                                                  causeMessage.contains("not supported with YUV surface")
                            
                            // Check if we should try auto-transcode fallback
                            val isDecoderError = is10BitAV1Error || 
                                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED ||
                                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                            
                            if (isDecoderError && autoTranscodeOnError && !hasTriedTranscodeFallback && !isUsingTranscodeFallback) {
                                Log.w("JellyfinPlayer", "")
                                Log.w("JellyfinPlayer", "🔄🔄🔄 AUTO-TRANSCODE FALLBACK TRIGGERED 🔄🔄🔄")
                                Log.w("JellyfinPlayer", "")
                                Log.w("JellyfinPlayer", "⚠️ Direct play failed - attempting server-side transcoding...")
                                Log.w("JellyfinPlayer", "   Error: ${error.message}")
                                Log.w("JellyfinPlayer", "")
                                
                                hasTriedTranscodeFallback = true
                                
                                // Retry with transcoded stream
                                scope.launch(Dispatchers.Main) {
                                    try {
                                        player.stop()
                                        player.clearMediaItems()
                                        
                                        // Get transcoding settings
                                        val transcodeTargetCodec = settings.transcodeTargetCodec
                                        val transcodeMaxBitrate = settings.transcodeMaxBitrateMbps
                                        
                                        // Get the transcoded URL
                                        val transcodedUrl = apiService.getTranscodedVideoUrl(
                                            itemId = item.Id,
                                            mediaSourceId = itemDetails?.MediaSources?.firstOrNull()?.Id,
                                            subtitleStreamIndex = null,
                                            targetVideoCodec = transcodeTargetCodec,
                                            maxBitrateMbps = transcodeMaxBitrate,
                                            audioCodec = "aac"
                                        )
                                        
                                        Log.d("JellyfinPlayer", "🔄 Transcoded URL: $transcodedUrl")
                                        
                                        // Create HLS media source for transcoded stream
                                        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                                            .createMediaSource(MediaItem.fromUri(Uri.parse(transcodedUrl)))
                                        
                                        player.setMediaSource(hlsMediaSource)
                                        player.prepare()
                                        player.play()
                                        
                                        isUsingTranscodeFallback = true
                                        Log.d("JellyfinPlayer", "✅ Switched to server transcoding: $transcodeTargetCodec @ ${transcodeMaxBitrate}Mbps")
                                        
                                        // Report playback start for transcode fallback
                                        scope.launch(Dispatchers.IO) {
                                            apiService.reportPlaybackStart(item.Id, 0)
                                        }
                                        
                                    } catch (e: Exception) {
                                        Log.e("JellyfinPlayer", "❌ Failed to switch to transcoding: ${e.message}", e)
                                        
                                        // Try MPV fallback if transcoding failed
                                        if (fallbackToMpv && isMpvInstalled && !hasTriedMpvFallback) {
                                            Log.w("JellyfinPlayer", "🎬 Transcoding failed - trying MPV fallback...")
                                            hasTriedMpvFallback = true
                                            launchMpvFallback()
                                        } else {
                                            showAV1Error = true
                                        }
                                    }
                                }
                                // Don't show error dialog - we're retrying with transcoding
                            } else if (isDecoderError && fallbackToMpv && isMpvInstalled && !hasTriedMpvFallback) {
                                // Transcoding is disabled, try MPV fallback
                                Log.w("JellyfinPlayer", "")
                                Log.w("JellyfinPlayer", "🎬🎬🎬 MPV FALLBACK TRIGGERED 🎬🎬🎬")
                                Log.w("JellyfinPlayer", "")
                                Log.w("JellyfinPlayer", "⚠️ ExoPlayer failed and transcoding is disabled")
                                Log.w("JellyfinPlayer", "⚠️ Launching MPV player as fallback...")
                                Log.w("JellyfinPlayer", "")
                                
                                hasTriedMpvFallback = true
                                launchMpvFallback()
                            } else if (is10BitAV1Error) {
                                Log.e("JellyfinPlayer", "")
                                Log.e("JellyfinPlayer", "❌❌❌ 10-BIT AV1 DECODING ERROR ❌❌❌")
                                Log.e("JellyfinPlayer", "")
                                Log.e("JellyfinPlayer", "⚠️ libgav1 cannot output 10-bit video to standard surface")
                                Log.e("JellyfinPlayer", "⚠️ This is a known limitation of the AV1 software decoder")
                                Log.e("JellyfinPlayer", "")
                                Log.e("JellyfinPlayer", "✅ SOLUTIONS:")
                                Log.e("JellyfinPlayer", "   1. Enable MPV Player: Settings → Playback → Use MPV Player")
                                Log.e("JellyfinPlayer", "      MPV uses dav1d which handles 10-bit AV1 natively")
                                Log.e("JellyfinPlayer", "   2. Server Transcoding: Configure Jellyfin to transcode AV1")
                                Log.e("JellyfinPlayer", "      Dashboard → Playback → Transcoding → AV1 → H.264/H.265")
                                Log.e("JellyfinPlayer", "")
                                
                                // Show user-friendly error to user
                                showAV1Error = true
                            }
                            // Check for other decoder errors
                            else if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED) {
                                Log.e("JellyfinPlayer", "❌ VIDEO DECODER ERROR: Device may not support this video codec!")
                                Log.e("JellyfinPlayer", "   Consider enabling server-side transcoding for this content")
                                
                                if (runtimeAV1Detected) {
                                    Log.e("JellyfinPlayer", "❌ AV1 DECODER FAILURE!")
                                    Log.e("JellyfinPlayer", "   Enable MPV Player or configure Jellyfin to transcode AV1 → H.264/H.265")
                                    showAV1Error = true
                                }
                            }
                            
                            // Check for subtitle-specific errors
                            if (error.cause is ParserException || error.message?.contains("subtitle", ignoreCase = true) == true) {
                                Log.e("JellyfinPlayer", "❌ SUBTITLE LOAD ERROR: This might be why external subtitles aren't appearing!")
                                Log.e("JellyfinPlayer", "   Error details: ${error.cause?.message ?: "Unknown"}")
                            }
                            
                            // Check if it's an HTTP 416 error (Range Not Satisfiable)
                            if (error.cause is HttpDataSource.InvalidResponseCodeException) {
                                val httpError = error.cause as HttpDataSource.InvalidResponseCodeException
                                if (httpError.responseCode == 416 && !hasRetriedWithoutRange && mediaUrl != null) {
                                    Log.w("JellyfinPlayer", "HTTP 416 error detected. Retrying without range requests...")
                                    scope.launch(Dispatchers.Main) {
                                        try {
                                            // Stop current playback
                                            player.stop()
                                            player.clearMediaItems()
                                            
                                            // Create a DataSource wrapper factory that removes range requests to avoid 416 errors
                                            val noRangeDataSourceFactory = object : DataSource.Factory {
                                                private val baseHttpFactory = DefaultHttpDataSource.Factory()
                                                    .setUserAgent("Jellyfin Android TV")
                                                    .setAllowCrossProtocolRedirects(true)
                                                    .setDefaultRequestProperties(headers.toMutableMap())
                                                
                                                private val baseFactory = DefaultDataSource.Factory(context, baseHttpFactory)
                                                
                                                override fun createDataSource(): DataSource {
                                                    val baseDataSource = baseFactory.createDataSource()
                                                    
                                                    // Return a wrapper that modifies DataSpecs to remove range information
                                                    return object : DataSource {
                                                        override fun open(dataSpec: DataSpec): Long {
                                                            // Remove range information to avoid 416 errors
                                                            // If DataSpec has position or length set, remove them to request entire file
                                                            val modifiedDataSpec = if (dataSpec.position != 0L || dataSpec.length > 0) {
                                                                // Create a new DataSpec without range request (request entire file)
                                                                DataSpec.Builder()
                                                                    .setUri(dataSpec.uri)
                                                                    .setHttpMethod(dataSpec.httpMethod)
                                                                    .setHttpRequestHeaders(dataSpec.httpRequestHeaders)
                                                                    .setKey(dataSpec.key)
                                                                    .setFlags(dataSpec.flags)
                                                                    .setPosition(0L) // Start from beginning
                                                                    .setLength(C.LENGTH_UNSET.toLong()) // Request entire file
                                                                    .build()
                                                            } else {
                                                                dataSpec
                                                            }
                                                            return baseDataSource.open(modifiedDataSpec)
                                                        }
                                                        
                                                        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                                                            return baseDataSource.read(buffer, offset, length)
                                                        }
                                                        
                                                        override fun getUri(): android.net.Uri? {
                                                            return baseDataSource.uri
                                                        }
                                                        
                                                        override fun close() {
                                                            baseDataSource.close()
                                                        }
                                                        
                                                        override fun addTransferListener(transferListener: TransferListener) {
                                                            baseDataSource.addTransferListener(transferListener)
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // Use the custom DataSource factory that removes range requests
                                            val retryDataSourceFactory = noRangeDataSourceFactory
                                            
                                            // Recreate MediaItem
                                            val retryMediaItem = if (subtitleStreamIndex != null && itemDetails != null) {
                                                try {
                                                    val mediaSourceIdForSubtitle = itemDetails?.MediaSources?.firstOrNull()?.Id ?: item.Id
                                                    val subtitleStream = itemDetails?.MediaSources?.firstOrNull()?.MediaStreams
                                                        ?.find { it.Type == "Subtitle" && it.Index == subtitleStreamIndex }
                                                    val subtitleUrl = apiService.buildJellyfinSubtitleUrl(
                                                        itemId = item.Id,
                                                        mediaSourceId = mediaSourceIdForSubtitle,
                                                        streamIndex = subtitleStreamIndex!!,
                                                        isExternal = subtitleStream?.IsExternal == true,
                                                        codec = subtitleStream?.Codec,
                                                        path = subtitleStream?.Path
                                                    )
                                                    val subtitleLanguage = subtitleStream?.Language
                                                    val subtitleMimeType = MimeTypes.TEXT_VTT
                                                    
                                                    MediaItem.Builder()
                                                        .setUri(Uri.parse(mediaUrl))
                                                        .setSubtitleConfigurations(
                                                            listOf(
                                                                MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                                                                    .setMimeType(subtitleMimeType)
                                                                    .setLanguage(subtitleLanguage)
                                                                    .build()
                                                            )
                                                        )
                                                        .build()
                                                } catch (e: Exception) {
                                                    MediaItem.fromUri(Uri.parse(mediaUrl))
                                                }
                                            } else {
                                                MediaItem.fromUri(Uri.parse(mediaUrl))
                                            }
                                            
                                            // Create new media source with the DataSource factory
                                            val retryMediaSource = ProgressiveMediaSource.Factory(retryDataSourceFactory as DataSource.Factory)
                                                .createMediaSource(retryMediaItem)
                                            
                                            // Set new media source and prepare
                                            player.setMediaSource(retryMediaSource)
                                            player.prepare()
                                            player.playWhenReady = true
                                            
                                            // Mark that we've retried
                                            hasRetriedWithoutRange = true
                                            hasSeekedToResume = false // Reset resume seek
                                            playerInitialized = true // Mark as initialized after retry
                                            
                                            Log.d("JellyfinPlayer", "Retried playback without range requests")
                                            
                                            // Report playback start for retry
                                            scope.launch(Dispatchers.IO) {
                                                apiService.reportPlaybackStart(item.Id, 0)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("JellyfinPlayer", "Error retrying playback without range requests", e)
                                        }
                                    }
                                    return // Don't log as fatal error, we're handling it
                                }
                            }
                            
                            // Check if it's a parser error (malformed file) - fallback to MP4 transcoding
                            if (error.cause is ParserException && !hasRetriedWithHls && mediaUrl != null && itemDetails != null) {
                                Log.w("JellyfinPlayer", "Parser error detected (malformed file). Falling back to MP4 transcoding...")
                                scope.launch(Dispatchers.Main) {
                                    try {
                                        // Stop current playback
                                        player.stop()
                                        player.clearMediaItems()
                                        
                                        // Get media source ID
                                        val mediaSource = itemDetails?.MediaSources?.firstOrNull()
                                        val mediaSourceId = mediaSource?.Id ?: item.Id
                                        
                                        // Generate MP4 transcoding URL (server will transcode to MP4)
                                        val base = if (apiService.serverBaseUrl.endsWith("/")) apiService.serverBaseUrl else "${apiService.serverBaseUrl}/"
                                        val mp4Url = "${base}Videos/${item.Id}/stream.mp4?VideoCodec=h264&AudioCodec=aac&mediaSourceId=$mediaSourceId&api_key=${apiService.apiKey}"
                                        
                                        // Create media source with transcoded MP4
                                        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                                            .setUserAgent("Jellyfin Android TV")
                                            .setAllowCrossProtocolRedirects(true)
                                            .setDefaultRequestProperties(apiService.getVideoRequestHeaders().toMutableMap())
                                        
                                        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                                        
                                        // Create MediaItem
                                        val transcodedMediaItem = if (subtitleStreamIndex != null && itemDetails != null) {
                                            try {
                                                val subtitleStream = itemDetails?.MediaSources?.firstOrNull()?.MediaStreams
                                                    ?.find { it.Type == "Subtitle" && it.Index == subtitleStreamIndex }
                                                
                                                val subtitleConfig = if (subtitleStream != null) {
                                                    Log.d("JellyfinPlayer", "🔄 Fallback: Adding subtitle ${subtitleStream.DisplayTitle}")
                                                    com.flex.elefin.player.SubtitleMapper.buildSubtitleConfiguration(
                                                        context = context,
                                                        apiService = apiService,
                                                        itemId = item.Id,
                                                        mediaSourceId = mediaSourceId, // Use the ID resolved above
                                                        stream = subtitleStream,
                                                        positionIndex = subtitleStream.Index ?: 0
                                                    )
                                                } else null
                                                
                                                val builder = MediaItem.Builder().setUri(Uri.parse(mp4Url))
                                                if (subtitleConfig != null) {
                                                    builder.setSubtitleConfigurations(listOf(subtitleConfig))
                                                }
                                                builder.build()
                                            } catch (e: Exception) {
                                                Log.e("JellyfinPlayer", "Error adding subtitle to fallback media item", e)
                                                MediaItem.fromUri(Uri.parse(mp4Url))
                                            }
                                        } else {
                                            MediaItem.fromUri(Uri.parse(mp4Url))
                                        }
                                        
                                        val transcodedMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                            .createMediaSource(transcodedMediaItem)
                                        
                                        // Set new media source and prepare
                                        player.setMediaSource(transcodedMediaSource)
                                        player.prepare()
                                        player.playWhenReady = true
                                        
                                        // Mark that we've retried
                                        hasRetriedWithHls = true
                                        hasSeekedToResume = false // Reset resume seek
                                        playerInitialized = true
                                        
                                        // Update mediaUrl for reference
                                        mediaUrl = mp4Url
                                        
                                        // Report playback start for HLS retry
                                        scope.launch(Dispatchers.IO) {
                                            apiService.reportPlaybackStart(item.Id, 0)
                                        }
                                        
                                        Log.d("JellyfinPlayer", "Retried playback with MP4 transcoding")
                                    } catch (e: Exception) {
                                        Log.e("JellyfinPlayer", "Error falling back to MP4 transcoding", e)
                                    }
                                }
                                return // Don't log as fatal error, we're handling it
                            }
                            
                            // For other errors, log and let the player handle it normally
                            Log.e("JellyfinPlayer", "Unhandled player error", error)
                        }

                        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                            isPlaying = isPlayingNow
                        }

                        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                            // ============================================================
                            // RUNTIME AV1 DETECTION - This is the ONLY reliable way to detect AV1
                            // Jellyfin metadata often returns Codec=null, so we must detect from ExoPlayer tracks
                            // ============================================================
                            val videoTrack = tracks.groups
                                .filter { it.type == C.TRACK_TYPE_VIDEO && it.isSupported }
                                .firstOrNull()
                            
                            if (videoTrack != null && videoTrack.length > 0) {
                                val videoFormat = videoTrack.mediaTrackGroup.getFormat(0)
                                val detectedMime = videoFormat.sampleMimeType ?: ""
                                val detectedCodecs = videoFormat.codecs ?: ""
                                
                                Log.d("JellyfinPlayer", "🔍 Runtime codec detection: mime=$detectedMime, codecs=$detectedCodecs")
                                
                                // Check for AV1 in MIME type or codecs string
                                val isAV1 = detectedMime.contains("av01", ignoreCase = true) || 
                                           detectedMime.contains("av1", ignoreCase = true) ||
                                           detectedCodecs.contains("av01", ignoreCase = true) ||
                                           detectedCodecs.contains("av1", ignoreCase = true)
                                
                                if (isAV1) {
                                    // Check bit depth from colorInfo (if available)
                                    val colorInfo = videoFormat.colorInfo
                                    val bitDepth = colorInfo?.lumaBitdepth ?: 8
                                    Log.e("JellyfinPlayer", "⚠️ RUNTIME AV1 DETECTED! mime=$detectedMime, codecs=$detectedCodecs, bitDepth=$bitDepth")
                                    Log.e("JellyfinPlayer", "   ColorInfo: $colorInfo")
                                    
                                    // AV1 with 10-bit will fail with libgav1 on standard surface
                                    // We'll detect this at error time and show the dialog
                                    Log.e("JellyfinPlayer", "")
                                    Log.e("JellyfinPlayer", "⚠️ AV1 DETECTED - May require special handling")
                                    Log.e("JellyfinPlayer", "⚠️ If video shows black screen, it's likely 10-bit AV1")
                                    Log.e("JellyfinPlayer", "⚠️ libgav1 cannot output 10-bit to standard SurfaceView")
                                    Log.e("JellyfinPlayer", "")
                                    Log.e("JellyfinPlayer", "✅ SOLUTIONS:")
                                    Log.e("JellyfinPlayer", "   1. Enable MPV Player in Settings → Playback → Use MPV Player")
                                    Log.e("JellyfinPlayer", "      MPV handles 10-bit AV1 natively via dav1d")
                                    Log.e("JellyfinPlayer", "   2. Configure Jellyfin server to transcode AV1 → H.264/H.265")
                                    Log.e("JellyfinPlayer", "      Dashboard → Playback → Transcoding")
                                    Log.e("JellyfinPlayer", "")
                                    runtimeAV1Detected = true
                                } else {
                                    Log.d("JellyfinPlayer", "✅ Non-AV1 codec detected: $detectedMime")
                                }
                            }
                            
                            // Log available tracks for debugging
                            Log.d("JellyfinPlayer", "Tracks changed: ${tracks.groups.size} track groups")
                            tracks.groups.forEach { group ->
                                Log.d("JellyfinPlayer", "Track group: type=${group.type}, supported=${group.isSupported}, trackCount=${group.mediaTrackGroup.length}, selected=${group.isSelected}")
                            }
                            
                            // When tracks are available, select subtitle track if specified
                            // Handle subtitle selection or disabling
                            // Filter out ONLY internal CEA-608/708 captions (auto-generated closed captions from video decoder)
                            // Keep all Jellyfin subtitles: external, embedded, and internal
                            // NOTE: APPLICATION_MEDIA3_CUES is the MIME type ExoPlayer uses for processed text subtitles, so we keep it
                            val textTrackGroups = tracks.groups.filter { group ->
                                if (group.type != androidx.media3.common.C.TRACK_TYPE_TEXT || !group.isSupported) {
                                    return@filter false
                                }
                                
                                val format = group.mediaTrackGroup.getFormat(0)
                                val isInternalCaption = format.sampleMimeType == MimeTypes.APPLICATION_CEA608 ||
                                                       format.sampleMimeType == MimeTypes.APPLICATION_CEA708
                                
                                // Only filter out CEA-608/708 captions, keep everything else
                                !isInternalCaption
                            }
                            
                            Log.d("JellyfinPlayer", "Found ${textTrackGroups.size} supported text track groups")
                            Log.d("JellyfinPlayer", "Jellyfin subtitle streams available for matching: ${jellyfinSubtitleStreams.size}")
                            
                            // ⭐ STEP 1: REGISTER ALL EXOPLAYER TRACKS WITH COMPOSITE KEYS (Production-Safe!)
                            // This must happen BEFORE selection logic so composite keys are available
                            // Only register tracks ONCE to prevent duplicates!
                            if (!hasRegisteredTracks && textTrackGroups.isNotEmpty()) {
                                hasRegisteredTracks = true // Mark as registered
                                Log.d("JellyfinPlayer", "⭐ STARTING TRACK REGISTRATION PHASE (first time only)")
                                Log.d("JellyfinPlayer", "   Text track groups to process: ${textTrackGroups.size}")
                                Log.d("JellyfinPlayer", "   Jellyfin subtitle streams to match: ${jellyfinSubtitleStreams.size}")
                                jellyfinSubtitleStreams.forEach { stream ->
                                    Log.d("JellyfinPlayer", "     JF Index=${stream.Index}, Lang=${stream.Language}, IsCC=${stream.IsHearingImpaired}, IsForced=${stream.IsForced}, IsExternal=${stream.IsExternal}")
                                }
                                
                                // ⚠️ CRITICAL: Find the ACTUAL group index in tracks.groups, not the filtered textTrackGroups index
                                textTrackGroups.forEachIndexed { filteredIndex, group ->
                                    // Find the original group index in the full tracks list
                                    val actualGroupIndex = tracks.groups.indexOf(group)
                                    
                                    val format = group.mediaTrackGroup.getFormat(0)
                                    val trackIndex = 0 // First track in group
                                    
                                    Log.d("JellyfinPlayer", "  Registering ExoPlayer subtitle track group $filteredIndex (actual index=$actualGroupIndex):")
                                    Log.d("JellyfinPlayer", "    Language: '${format.language}', Label: '${format.label}'")
                                    Log.d("JellyfinPlayer", "    MIME: ${format.sampleMimeType}, ID: ${format.id}")
                                    Log.d("JellyfinPlayer", "    Selection flags: ${format.selectionFlags}, Role flags: ${format.roleFlags}")
                                    
                                    // Match this ExoPlayer track to a Jellyfin subtitle by lang…39769 tokens truncated…                            ) {
                                        Text(
                                            text = season.Name,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (isLoadingEpisodes) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(color = themeColor)
                            }
                        }
                    } else if (episodes.isNotEmpty()) {
                        items(episodes.size) { index ->
                            val ep = episodes[index]
                            val isCurrentPlaying = ep.Id == (itemDetails?.Id ?: item.Id)
                            val episodePlayResumeMs = ep.UserData?.PositionTicks?.let { it / 10_000 } ?: 0L
                            
                            androidx.compose.material3.Card(
                                onClick = { playClickedEpisode(ep, episodePlayResumeMs) },
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (isCurrentPlaying) themeColor.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val epTag = ep.ImageTags?.get("Primary")
                                    val epImageUrl = remember(ep.Id, epTag) {
                                        if (epTag != null) {
                                            apiService.getImageUrl(
                                                itemId = ep.Id,
                                                imageType = "Primary",
                                                imageTag = epTag,
                                                maxWidth = 300,
                                                quality = 70
                                            )
                                        } else ""
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 62.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Gray.copy(alpha = 0.1f))
                                    ) {
                                        if (epImageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(epImageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = ep.Name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Tv,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                        
                                        // Position indicator / played percentage
                                        ep.UserData?.PlayedPercentage?.let { pct ->
                                            if (pct > 0 && pct < 100) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .background(Color.White.copy(alpha = 0.3f))
                                                        .align(Alignment.BottomStart)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(pct.toFloat() / 100f)
                                                            .fillMaxHeight()
                                                            .background(themeColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${ep.IndexNumber}. ${ep.Name}",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                            color = if (isCurrentPlaying) themeColor else Color.White,
                                            fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        ep.RunTimeTicks?.let { ticks ->
                                            val minutes = ticks / 10_000L / 1000 / 60
                                            if (minutes > 0) {
                                                Text(
                                                    text = "${minutes} min",
                                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Fullscreen mode
        playerContent(Modifier.fillMaxSize())
    }

    // Settings menu with subtitle picker
    if (showSettingsMenu) {
            ExoPlayerSettingsMenu(
                item = itemDetails ?: item,
                apiService = apiService,
                currentSubtitleIndex = currentSubtitleIndex,
                onDismiss = { 
                    showSettingsMenu = false
                    settingsMenuInitialLevel = "main" // Reset for next time
                },
                player = player,
                jellyfinSubtitleStreams = jellyfinSubtitleStreams, // Pass for composite key registration
                downloadedSubtitles = downloadedSubtitles, // Downloaded OpenSubtitles
                initialMenuLevel = settingsMenuInitialLevel, // Open to subtitles if CC button was pressed
                isMobile = isMobile,
                playbackQuality = playbackQuality,
                onPlaybackQualitySelected = { selectedQuality ->
                    playbackQuality = selectedQuality
                    settings.playbackQuality = selectedQuality.name
                    Log.d("JellyfinPlayer", "Playback quality selected: ${selectedQuality.label}; Original keeps Direct Play")
                },
                onDownloadedSubtitleSelected = { filePath ->
                    // Select downloaded subtitle by finding its ExoPlayer track
                    scope.launch(Dispatchers.Main) {
                        try {
                            val tracks = player.currentTracks
                            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                            
                            Log.d("JellyfinPlayer", "🔍 Looking for downloaded subtitle track: $filePath")
                            Log.d("JellyfinPlayer", "   Available text groups: ${textGroups.size}")
                            
                            // Find the track group that matches this downloaded subtitle
                            // Downloaded subtitles have labels like "English (Downloaded)"
                            val fileName = java.io.File(filePath).name
                            val language = fileName.substringBefore("_")
                            
                            var foundTrack = false
                            textGroups.forEachIndexed { groupIndex, group ->
                                val format = group.mediaTrackGroup.getFormat(0)
                                Log.d("JellyfinPlayer", "   Group $groupIndex: label='${format.label}', lang='${format.language}'")
                                
                                // Match by label containing "(Downloaded)" or by file URI
                                val isDownloadedTrack = format.label?.contains("(Downloaded)") == true ||
                                                       format.id?.contains(filePath) == true
                                val matchesLanguage = format.language == language
                                
                                if (isDownloadedTrack && matchesLanguage && !foundTrack) {
                                    foundTrack = true
                                    Log.d("JellyfinPlayer", "   ✅ Found matching downloaded subtitle track at group $groupIndex")
                                    
                                    // Select this track
                                    val updatedParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .addOverride(
                                            androidx.media3.common.TrackSelectionOverride(
                                                group.mediaTrackGroup,
                                                listOf(0)
                                            )
                                        )
                                        .build()
                                    
                                    player.trackSelectionParameters = updatedParameters
                                    Log.d("JellyfinPlayer", "✅ Selected downloaded subtitle: $fileName")
                                    currentSubtitleIndex = null // Clear Jellyfin index since this is a downloaded subtitle
                                }
                            }
                            
                            if (!foundTrack) {
                                Log.w("JellyfinPlayer", "⚠️ Could not find ExoPlayer track for downloaded subtitle: $filePath")
                            }
                            
                            showSettingsMenu = false
                        } catch (e: Exception) {
                            Log.e("JellyfinPlayer", "Error selecting downloaded subtitle", e)
                        }
                    }
                },
                onSubtitleSelected = { subtitleIndex ->
                    currentSubtitleIndex = subtitleIndex
                    // Use ExoPlayer track selection API to select the subtitle
                    scope.launch(Dispatchers.Main) {
                        try {
                            // Safety check - make sure player is still valid
                            if (player.playbackState == Player.STATE_IDLE) {
                                Log.w("JellyfinPlayer", "⚠️ Player is idle, skipping subtitle selection")
                                showSettingsMenu = false
                                return@launch
                            }
                            
                            if (subtitleIndex == null) {
                                // Disable all subtitles by clearing overrides only
                                // Do NOT use setTrackTypeDisabled - that prevents ExoPlayer UI from working
                                val updatedParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .build()
                                
                                player.trackSelectionParameters = updatedParameters
                                Log.d("JellyfinPlayer", "✅ Cleared subtitle selection (subtitles disabled)")
                            } else {
                                // Find the ExoPlayer track that matches the selected Jellyfin subtitle index
                                Log.d("JellyfinPlayer", "🔍 Attempting to select subtitle: Jellyfin index=$subtitleIndex")
                                Log.d("JellyfinPlayer", "   Available Jellyfin subtitle streams: ${jellyfinSubtitleStreams.size}")
                                jellyfinSubtitleStreams.forEach { stream ->
                                    Log.d("JellyfinPlayer", "     JF Index=${stream.Index}, Lang=${stream.Language}, DisplayTitle=${stream.DisplayTitle}")
                                }
                                
                                val exoTrackInfo = SubtitleMapper.getExoPlayerTrackInfo(subtitleIndex)
                                
                                if (exoTrackInfo != null) {
                                    val (groupIndex, trackIndexInGroup) = exoTrackInfo
                                    val tracks = player.currentTracks
                                    
                                    Log.d("JellyfinPlayer", "   SubtitleMapper found: ExoPlayer group=$groupIndex, track=$trackIndexInGroup")
                                    Log.d("JellyfinPlayer", "   Total track groups: ${tracks.groups.size}")
                                    
                                    if (groupIndex >= 0 && groupIndex < tracks.groups.size) {
                                        val group = tracks.groups[groupIndex]
                                        
                                        // Safety check for track index
                                        if (trackIndexInGroup >= 0 && trackIndexInGroup < group.mediaTrackGroup.length) {
                                            val format = group.mediaTrackGroup.getFormat(trackIndexInGroup)
                                            Log.d("JellyfinPlayer", "   Selected group format: lang=${format.language}, label=${format.label}, mime=${format.sampleMimeType}")
                                            
                                            // Override to select this specific track (text tracks are not disabled, so no need to enable)
                                            val updatedParameters = player.trackSelectionParameters
                                                .buildUpon()
                                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                                .addOverride(
                                                    androidx.media3.common.TrackSelectionOverride(
                                                        group.mediaTrackGroup,
                                                        listOf(trackIndexInGroup)
                                                    )
                                                )
                                                .build()
                                            
                                            player.trackSelectionParameters = updatedParameters
                                            Log.d("JellyfinPlayer", "✅ Selected subtitle: Jellyfin index=$subtitleIndex, ExoPlayer group=$groupIndex, track=$trackIndexInGroup")
                                            
                                            // Save preference
                                            settings.setSubtitlePreference(item.Id, subtitleIndex)
                                            lastSelectedSubtitleIndex = subtitleIndex
                                            currentSubtitleIndex = subtitleIndex
                                            Log.d("JellyfinPlayer", "💾 Saved subtitle preference: $subtitleIndex")
                                        } else {
                                            Log.w("JellyfinPlayer", "⚠️ Invalid track index: $trackIndexInGroup (group.length=${group.mediaTrackGroup.length})")
                                        }
                                    } else {
                                        Log.w("JellyfinPlayer", "⚠️ Invalid group index: $groupIndex (tracks.groups.size=${tracks.groups.size})")
                                    }
                                } else {
                                    Log.w("JellyfinPlayer", "⚠️ No ExoPlayer track found for Jellyfin subtitle index $subtitleIndex")
                                    Log.w("JellyfinPlayer", "   This means SubtitleMapper doesn't have a mapping for this Jellyfin index")
                                    Log.w("JellyfinPlayer", "   Possible reasons:")
                                    Log.w("JellyfinPlayer", "   1. Track registration hasn't completed yet")
                                    Log.w("JellyfinPlayer", "   2. This subtitle wasn't in the MediaStreams when tracks were registered")
                                    Log.w("JellyfinPlayer", "   3. ExoPlayer failed to load this subtitle (404, MIME error, etc.)")
                                }
                            }
                            
                            showSettingsMenu = false
                        } catch (e: Exception) {
                            Log.e("JellyfinPlayer", "Error selecting subtitle track", e)
                        }
                    }
                }
            )
        }
        
        // AV1 10-bit decoding error dialog
        if (showAV1Error) {
            Dialog(
                onDismissRequest = { showAV1Error = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.tv.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.tv.material3.SurfaceDefaults.colors(
                            containerColor = Color(0xFF1A1A2E),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Error icon
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "10-bit AV1 Video Not Supported",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "This video uses 10-bit AV1 encoding which requires special hardware or software support not available on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Solutions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF16213E), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Solutions:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF4ECDC4),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "1. Enable MPV Player in Settings → Playback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "2. Configure Jellyfin to transcode AV1 content",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Dismiss button
                            val dismissFocusRequester = remember { FocusRequester() }
                            LaunchedEffect(Unit) {
                                dismissFocusRequester.requestFocus()
                            }
                            
                            var isFocused by remember { mutableStateOf(false) }
                            androidx.tv.material3.Button(
                                onClick = { 
                                    showAV1Error = false
                                    onBack()
                                },
                                modifier = Modifier
                                    .focusRequester(dismissFocusRequester)
                                    .onFocusChanged { isFocused = it.isFocused },
                                colors = androidx.tv.material3.ButtonDefaults.colors(
                                    containerColor = if (isFocused) Color(0xFF4ECDC4) else Color(0xFF2D4059),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        }
    }

// Data class to hold audio track information
data class AudioTrackInfo(
    val group: Tracks.Group,
    val index: Int,
    val language: String?,
    val label: String?,
    val codec: String?,
    val isSelected: Boolean,
    val channelCount: Int,
    val sampleRate: Int
)

@Composable
fun ExoPlayerSettingsMenu(
    item: JellyfinItem,
    apiService: JellyfinApiService,
    currentSubtitleIndex: Int?,
    onDismiss: () -> Unit,
    onSubtitleSelected: (Int?) -> Unit,
    player: ExoPlayer? = null,
    jellyfinSubtitleStreams: List<MediaStream> = emptyList(), // For composite key registration
    downloadedSubtitles: List<com.flex.elefin.subtitles.DownloadedSubtitle> = emptyList(), // Downloaded OpenSubtitles
    onDownloadedSubtitleSelected: ((String) -> Unit)? = null, // Callback for selecting downloaded subtitle by file path
    initialMenuLevel: String = "main", // "main", "subtitles", "audio", "speed" - allows opening directly to a submenu
    isMobile: Boolean = false,
    playbackQuality: PlaybackQuality = PlaybackQuality.ORIGINAL,
    onPlaybackQualitySelected: (PlaybackQuality) -> Unit = {}
) {
    var itemDetails by remember { mutableStateOf<JellyfinItem?>(null) }
    var isLoadingSubtitles by remember { mutableStateOf(true) }
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }
    
    // Navigation state for multi-level menu
    var currentMenuLevel by remember { mutableStateOf(initialMenuLevel) } // "main", "subtitles", "audio", "speed", "quality"
    
    // Focus requesters for auto-focus on first item in each menu
    val mainMenuFirstItemFocusRequester = remember { FocusRequester() }
    val subtitlesFirstItemFocusRequester = remember { FocusRequester() }
    val audioFirstItemFocusRequester = remember { FocusRequester() }
    val speedFirstItemFocusRequester = remember { FocusRequester() }
    
    // Auto-focus first item when menu level changes
    LaunchedEffect(currentMenuLevel) {
        kotlinx.coroutines.delay(150) // Longer delay to ensure UI is fully rendered
        try {
            when (currentMenuLevel) {
                "main" -> mainMenuFirstItemFocusRequester.requestFocus()
                "subtitles" -> subtitlesFirstItemFocusRequester.requestFocus()
                "audio" -> audioFirstItemFocusRequester.requestFocus()
                "speed" -> speedFirstItemFocusRequester.requestFocus()
            }
        } catch (e: IllegalStateException) {
            // FocusRequester not yet attached to a composable - this can happen
            // if the menu is dismissed before the focus request completes
            Log.w("ExoPlayerSettingsMenu", "Focus request failed (menu may have been dismissed): ${e.message}")
        }
    }
    
    // Fetch full item details to get MediaSources with subtitle and audio streams
    LaunchedEffect(item.Id, apiService) {
        withContext(Dispatchers.IO) {
            try {
                val details = apiService.getItemDetails(item.Id)
                itemDetails = details
                isLoadingSubtitles = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Normal cancellation when composable leaves composition - don't log as error
                throw e // Re-throw to respect cancellation
            } catch (e: Exception) {
                Log.e("ExoPlayerSettingsMenu", "Error fetching item details", e)
                isLoadingSubtitles = false
            }
        }
    }
    
    // Update tracks when player tracks change
    // Capture jellyfinSubtitleStreams in the effect scope
    DisposableEffect(player, jellyfinSubtitleStreams) {
        val listener = if (player != null) {
            // Get initial tracks if available
            val initialTracks = player.currentTracks
            if (initialTracks.groups.isNotEmpty()) {
                currentTracks = initialTracks
            }
            
            // Listen for track changes
            object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    currentTracks = tracks
                    
                    // Log detected subtitle tracks for debugging
                    val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                    Log.d("JellyfinPlayer", "ExoPlayer detected ${textGroups.size} subtitle track group(s)")
                    if (textGroups.isEmpty()) {
                        Log.w("JellyfinPlayer", "⚠️ No subtitle tracks detected by ExoPlayer")
                        Log.w("JellyfinPlayer", "   This usually means:")
                        Log.w("JellyfinPlayer", "   1. The subtitle URL returned 404 (file doesn't exist)")
                        Log.w("JellyfinPlayer", "   2. The subtitle MIME type is incorrect")
                        Log.w("JellyfinPlayer", "   3. ExoPlayer couldn't parse the subtitle file")
                    } else {
                        // Log detected subtitle tracks for debugging
                        textGroups.forEachIndexed { idx, group ->
                            val format = group.mediaTrackGroup.getFormat(0)
                            Log.d("JellyfinPlayer", "  ExoPlayer subtitle track group $idx:")
                            Log.d("JellyfinPlayer", "    Format.id: '${format.id}', Lang: ${format.language}, MIME: ${format.sampleMimeType}")
                            Log.d("JellyfinPlayer", "    Label: ${format.label}, Selected: ${group.isSelected}")
                        }
                    }
                }
                
            }.also { player.addListener(it) }
        } else null
        
        onDispose {
            listener?.let { player?.removeListener(it) }
        }
    }
    
    // Get audio tracks from ExoPlayer
    val audioTracks = remember(currentTracks) {
        currentTracks?.groups?.filter { group ->
            group.type == C.TRACK_TYPE_AUDIO && group.isSupported
        }?.mapIndexedNotNull { index, group ->
            // Get format info from the first track in the group
            if (group.mediaTrackGroup.length > 0) {
                val format = group.mediaTrackGroup.getFormat(0)
                AudioTrackInfo(
                    group = group,
                    index = index,
                    language = format.language,
                    label = format.label,
                    codec = format.codecs,
                    isSelected = group.isSelected,
                    channelCount = format.channelCount,
                    sampleRate = format.sampleRate
                )
            } else null
        } ?: emptyList()
    }
    
    // Get audio streams from Jellyfin MediaSources for additional metadata
    val audioStreams = remember(itemDetails?.MediaSources) {
        itemDetails?.MediaSources?.firstOrNull()?.MediaStreams
            ?.filter { it.Type == "Audio" }
            ?.sortedBy { it.Index ?: 0 } ?: emptyList()
    }
    
    // Get subtitle streams from MediaSources
    val subtitleStreams = remember(itemDetails?.MediaSources) {
        itemDetails?.MediaSources?.firstOrNull()?.MediaStreams
            ?.filter { it.Type == "Subtitle" }
            ?.sortedBy { it.Index ?: 0 } ?: emptyList()
    }
    
    // Handle back button navigation
    BackHandler(enabled = currentMenuLevel != "main") {
        when (currentMenuLevel) {
            "subtitles", "audio", "speed", "quality" -> currentMenuLevel = "main"
            else -> onDismiss()
        }
    }
    
    Dialog(
        onDismissRequest = {
            if (currentMenuLevel == "main") {
                onDismiss()
            } else {
                currentMenuLevel = "main"
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)), // Darker, more opaque background
            contentAlignment = Alignment.Center
        ) {
            androidx.tv.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight(0.6f),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), // Semi-transparent surface
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Dialog title - changes based on current menu level
                    Text(
                        text = when (currentMenuLevel) {
                            "subtitles" -> "Subtitles"
                            "audio" -> "Pistas de audio"
                            "speed" -> "Velocidad de reproducción"
                            "quality" -> "Calidad"
                            else -> "Ajustes del reproductor"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.8f
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Custom colors for better visibility - light gray instead of white
                    val listItemColors = androidx.tv.material3.ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color(0xFF424242), // Dark gray when focused
                        focusedContentColor = Color.White,
                        selectedContainerColor = Color(0xFF616161), // Medium gray when selected
                        selectedContentColor = Color.White
                    )
                    
                    // Show different content based on current menu level
                    when (currentMenuLevel) {
                        "main" -> {
                            // Main menu - show 3 category buttons
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Audio Tracks button
                                var isFirstMainMenuItem = true
                                if (player != null && audioTracks.isNotEmpty()) {
                                    item {
                                        ListItem(
                                            selected = false,
                                            onClick = { currentMenuLevel = "audio" },
                                            colors = listItemColors,
                                            headlineContent = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = "Pistas de audio",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.9f
                                                        )
                                                    )
                                                }
                                            },
                                            trailingContent = {
                                                Text(
                                                    text = "▶",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isMobile) Modifier.clickable { currentMenuLevel = "audio" }
                                                    else Modifier.focusRequester(mainMenuFirstItemFocusRequester)
                                                )
                                        )
                                    }
                                    isFirstMainMenuItem = false
                                }
                                
                                // Subtitles button
                                item {
                                    ListItem(
                                        selected = false,
                                        onClick = { currentMenuLevel = "subtitles" },
                                        colors = listItemColors,
                                        headlineContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "Subtítulos",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.9f
                                                    )
                                                )
                                            }
                                        },
                                        trailingContent = {
                                            Text(
                                                text = "▶",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isMobile) Modifier.clickable { currentMenuLevel = "subtitles" }
                                                else if (isFirstMainMenuItem) Modifier.focusRequester(mainMenuFirstItemFocusRequester)
                                                else Modifier
                                            )
                                    )
                                }
                                
                                // Playback Speed button
                                if (player != null) {
                                    item {
                                        ListItem(
                                            selected = false,
                                            onClick = { currentMenuLevel = "speed" },
                                            colors = listItemColors,
                                            headlineContent = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FastForward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = "Velocidad de reproducción",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.9f
                                                        )
                                                    )
                                                }
                                            },
                                            trailingContent = {
                                                Text(
                                                    text = "▶",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isMobile) Modifier.clickable { currentMenuLevel = "speed" }
                                                    else Modifier
                                                )
                                        )
                                    }
                                }

                                item {
                                    ListItem(
                                        selected = false,
                                        onClick = { currentMenuLevel = "quality" },
                                        colors = listItemColors,
                                        headlineContent = {
                                            Text("Calidad: ${playbackQuality.label}", style = MaterialTheme.typography.titleMedium)
                                        },
                                        trailingContent = { Text("▶") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "quality" -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(PlaybackQuality.values().size) { index ->
                                    val option = PlaybackQuality.values()[index]
                                    ListItem(
                                        selected = option == playbackQuality,
                                        onClick = {
                                            onPlaybackQualitySelected(option)
                                            currentMenuLevel = "main"
                                        },
                                        colors = listItemColors,
                                        headlineContent = { Text(option.label) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        
                        "audio" -> {
                            // Audio tracks list
                            LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(audioTracks.size) { index ->
                                val track = audioTracks[index]
                                val trackTitle = buildString {
                                    track.label?.let { append(it) }
                                    if (isEmpty()) {
                                        track.language?.let { append(it) } ?: append("Unknown")
                                    }
                                }
                                val trackInfo = buildString {
                                    track.codec?.let { 
                                        if (isNotEmpty()) append(", ")
                                        append(it)
                                    }
                                    if (track.channelCount > 0) {
                                        if (isNotEmpty()) append(", ")
                                        append("${track.channelCount}ch")
                                    }
                                    if (track.sampleRate > 0) {
                                        if (isNotEmpty()) append(", ")
                                        append("${track.sampleRate / 1000}kHz")
                                    }
                                }
                                
                                ListItem(
                                    selected = track.isSelected,
                                    onClick = {
                                        // Select audio track
                                        player?.let { exoPlayer ->
                                            try {
                                                val trackSelectionOverride = TrackSelectionOverride(
                                                    track.group.mediaTrackGroup,
                                                    0 // Select first track in the group
                                                )
                                                val updatedParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .addOverride(trackSelectionOverride)
                                                    .build()
                                                exoPlayer.trackSelectionParameters = updatedParameters
                                                Log.d("ExoPlayerSettingsMenu", "Selected audio track: $trackTitle")
                                            } catch (e: Exception) {
                                                Log.e("ExoPlayerSettingsMenu", "Error selecting audio track", e)
                                            }
                                        }
                                    },
                                    colors = listItemColors,
                                    headlineContent = {
                                        Column {
                                            Text(
                                                text = trackTitle,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.8f
                                                )
                                            )
                                            if (trackInfo.isNotEmpty()) {
                                                Text(
                                                    text = trackInfo,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.7f
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isMobile) Modifier.clickable {
                                            player?.let { exoPlayer ->
                                                try {
                                                    val trackSelectionOverride = TrackSelectionOverride(
                                                        track.group.mediaTrackGroup,
                                                        0
                                                    )
                                                    val updatedParameters = exoPlayer.trackSelectionParameters
                                                        .buildUpon()
                                                        .addOverride(trackSelectionOverride)
                                                        .build()
                                                    exoPlayer.trackSelectionParameters = updatedParameters
                                                    Log.d("ExoPlayerSettingsMenu", "Selected audio track: $trackTitle")
                                                } catch (e: Exception) {
                                                    Log.e("ExoPlayerSettingsMenu", "Error selecting audio track", e)
                                                }
                                            }
                                        }
                                        else if (index == 0) Modifier.focusRequester(audioFirstItemFocusRequester)
                                        else Modifier
                                    )
                                )
                            }
                        }
                        }
                        
                        "subtitles" -> {
                            // Subtitles list
                            if (isLoadingSubtitles) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Loading subtitles...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.8f
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // "None" option to disable subtitles
                            item {
                                ListItem(
                                    selected = currentSubtitleIndex == null,
                                    onClick = {
                                        onSubtitleSelected(null)
                                    },
                                    colors = listItemColors,
                                    headlineContent = {
                                        Text(
                                            text = "None (Off)",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.8f
                                            )
                                        )
                                    },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isMobile) Modifier.clickable {
                                            onSubtitleSelected(null)
                                        }
                                        else Modifier.focusRequester(subtitlesFirstItemFocusRequester)
                                    )
                                )
                            }
                            
                            // Subtitle stream options from Jellyfin
                            items(subtitleStreams) { stream ->
                                val subtitleTitle = stream.DisplayTitle
                                    ?: stream.Language
                                    ?: "Unknown"
                                val subtitleInfo = buildString {
                                    if (stream.IsDefault == true) append("Default")
                                    if (stream.IsForced == true) {
                                        if (isNotEmpty()) append(", ")
                                        append("Forced")
                                    }
                                    if (stream.IsExternal == true) {
                                        if (isNotEmpty()) append(", ")
                                        append("External")
                                    }
                                    // Debug: Show the actual Jellyfin index
                                    if (isNotEmpty()) append(" • ")
                                    append("Index ${stream.Index}")
                                }
                                
                                ListItem(
                                    selected = stream.Index == currentSubtitleIndex,
                                    onClick = {
                                        stream.Index?.let { index ->
                                            Log.d("ExoPlayerSettingsMenu", "📺 User clicked: $subtitleTitle (Jellyfin Index=$index)")
                                            onSubtitleSelected(index)
                                        }
                                    },
                                    colors = listItemColors,
                                    headlineContent = {
                                        Column {
                                            Text(
                                                text = subtitleTitle,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.8f
                                                )
                                            )
                                            if (subtitleInfo.isNotEmpty()) {
                                                Text(
                                                    text = subtitleInfo,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.7f
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isMobile) Modifier.clickable {
                                                stream.Index?.let { index ->
                                                    Log.d("ExoPlayerSettingsMenu", "📺 User clicked: $subtitleTitle (Jellyfin Index=$index)")
                                                    onSubtitleSelected(index)
                                                }
                                            } else Modifier
                                        )
                                )
                            }
                            
                            // ⭐ Downloaded subtitles from OpenSubtitles
                            if (downloadedSubtitles.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Downloaded Subtitles",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = MaterialTheme.typography.titleSmall.fontSize * 0.8f
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                                
                                items(downloadedSubtitles) { downloadedSub ->
                                    val subtitleTitle = com.flex.elefin.subtitles.SubtitleLanguages.getDisplayName(downloadedSub.language)
                                    val subtitleInfo = "${downloadedSub.release} (Downloaded)"
                                    
                                    ListItem(
                                        selected = false, // Downloaded subtitles have different selection mechanism
                                        onClick = {
                                            Log.d("ExoPlayerSettingsMenu", "📺 User clicked downloaded subtitle: ${downloadedSub.fileName}")
                                            onDownloadedSubtitleSelected?.invoke(downloadedSub.filePath)
                                        },
                                        colors = listItemColors,
                                        headlineContent = {
                                            Column {
                                                Text(
                                                    text = subtitleTitle,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.8f
                                                    )
                                                )
                                                Text(
                                                    text = subtitleInfo,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.7f
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isMobile) Modifier.clickable {
                                                    Log.d("ExoPlayerSettingsMenu", "📺 User clicked downloaded subtitle: ${downloadedSub.fileName}")
                                                    onDownloadedSubtitleSelected?.invoke(downloadedSub.filePath)
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                        }
                        }
                        }
                        
                        "speed" -> {
                            // Playback speed list
                            player?.let { exoPlayer ->
                                val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                                val currentSpeed = exoPlayer.playbackParameters.speed
                                val currentSpeedIndex = speedOptions.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.01f }.takeIf { it >= 0 } ?: 3
                                
                                LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                            items(speedOptions.size) { index ->
                                val speed = speedOptions[index]
                                val speedText = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x"
                                
                                ListItem(
                                    selected = index == currentSpeedIndex,
                                    onClick = {
                                        exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
                                        Log.d("ExoPlayerSettingsMenu", "Changed playback speed to ${speed}x")
                                    },
                                    colors = listItemColors,
                                    headlineContent = {
                                        Text(
                                            text = speedText,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.8f
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isMobile) Modifier.clickable {
                                                exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
                                                Log.d("ExoPlayerSettingsMenu", "Changed playback speed to ${speed}x")
                                            }
                                            else if (index == 0) Modifier.focusRequester(speedFirstItemFocusRequester)
                                            else Modifier
                                        )
                                )
                            }
                        }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NextUpOverlay(
    nextEpisode: JellyfinItem,
    countdown: Int,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.0f)), // Transparent background
        contentAlignment = Alignment.BottomEnd
    ) {
        androidx.tv.material3.Surface(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.8f),
                contentColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                // Episode info
                nextEpisode.SeriesName?.let { seriesName ->
                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Episode number and name
                val episodeInfo = buildString {
                    nextEpisode.ParentIndexNumber?.let { seasonNum ->
                        append("S$seasonNum")
                    }
                    nextEpisode.IndexNumber?.let { episodeNum ->
                        if (isNotEmpty()) append(" • ")
                        append("E$episodeNum")
                    }
                    if (isNotEmpty() && nextEpisode.Name.isNotEmpty()) {
                        append(" — ")
                    }
                    append(nextEpisode.Name)
                }
                
                Text(
                    text = episodeInfo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 2
                )
                
                // Countdown
                Text(
                    text = "Autoplay in $countdown…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Normalizes ISO 639-1 (2-letter) and ISO 639-2/T (3-letter) language codes to a common format.
 * This helps match subtitles when ExoPlayer uses different language code standards than Jellyfin.
 * 
 * Examples:
 * - "es" -> "es"
 * - "spa" -> "es"
 * - "en" -> "en"
 * - "eng" -> "en"
 * - "fr" -> "fr"
 * - "fra" -> "fr"
 * - "tur" -> "tr"
 * - "chi" -> "zh"
 */
private fun normalizeLanguageCode(languageCode: String?): String? {
    if (languageCode == null) return null
    
    // Map common ISO 639-2/T (3-letter) codes to ISO 639-1 (2-letter) codes
    val iso639Map = mapOf(
        "eng" to "en",
        "spa" to "es",
        "fra" to "fr",
        "deu" to "de",
        "ita" to "it",
        "por" to "pt",
        "rus" to "ru",
        "jpn" to "ja",
        "chi" to "zh",
        "kor" to "ko",
        "ara" to "ar",
        "tur" to "tr",
        "pol" to "pl",
        "nld" to "nl",
        "swe" to "sv",
        "dan" to "da",
        "fin" to "fi",
        "nor" to "no",
        "ces" to "cs",
        "hun" to "hu",
        "tha" to "th",
        "vie" to "vi",
        "ind" to "id",
        "heb" to "he",
        "ukr" to "uk",
        "ron" to "ro",
        "ell" to "el",
        "cat" to "ca",
        "hrv" to "hr",
        "slk" to "sk",
        "bul" to "bg",
        "srp" to "sr",
        "slv" to "sl",
        "lit" to "lt",
        "lav" to "lv",
        "est" to "et",
        "isl" to "is",
        "msa" to "ms",
        "fil" to "tl",
        "hin" to "hi",
        "ben" to "bn",
        "tam" to "ta",
        "tel" to "te",
        "mar" to "mr",
        "urd" to "ur",
        "fas" to "fa",
        "swa" to "sw"
    )
    
    val lowerCode = languageCode.lowercase()
    
    // If it's a 3-letter code and we have a mapping, return the 2-letter equivalent
    if (lowerCode.length == 3 && iso639Map.containsKey(lowerCode)) {
        return iso639Map[lowerCode]
    }
    
    // If it's already 2 letters, return as-is
    if (lowerCode.length == 2) {
        return lowerCode
    }
    
    // Fallback: return first 2 characters
    return lowerCode.take(2)
}

@Composable
fun SubtitleSelectionDialog(
    item: JellyfinItem,
    apiService: JellyfinApiService,
    currentSubtitleIndex: Int?,
    onDismiss: () -> Unit,
    onSubtitleSelected: (Int?) -> Unit
) {
    var itemDetails by remember { mutableStateOf<JellyfinItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Fetch full item details to get subtitle streams
    // First refresh the item on the server to detect any newly added external subtitles
    LaunchedEffect(item.Id, apiService) {
        withContext(Dispatchers.IO) {
            try {
                // Refresh item metadata on server to detect new external subtitle files
                Log.d("SubtitleDialog", "Refreshing item metadata to detect new subtitles...")
                apiService.refreshItemMetadata(item.Id)
                
                // Small delay to allow server to process the refresh
                kotlinx.coroutines.delay(500)
                
                // Now fetch the updated item details
                val details = apiService.getItemDetails(item.Id)
                itemDetails = details
                isLoading = false
                
                val subtitleCount = details?.MediaSources?.firstOrNull()?.MediaStreams
                    ?.count { it.Type == "Subtitle" } ?: 0
                Log.d("SubtitleDialog", "Loaded $subtitleCount subtitle streams after refresh")
            } catch (e: Exception) {
                Log.e("SubtitleDialog", "Error fetching item details", e)
                isLoading = false
            }
        }
    }
    
    // Full-screen dialog with dark background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Dialog content
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 500.dp)
                .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(24.dp)
                .clickable(
                    onClick = { /* Prevent click from closing dialog */ },
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            androidx.compose.material3.Text(
                text = "Select Subtitle",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (isLoading) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
                androidx.compose.material3.CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            } else {
                // Get subtitle streams
                val subtitleStreams = itemDetails?.MediaSources?.firstOrNull()?.MediaStreams
                    ?.filter { it.Type == "Subtitle" }
                    ?.sortedBy { it.Index ?: 0 } ?: emptyList()
                
                // Scrollable list of subtitles
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // "None" option
                    item {
                        SubtitleOptionItem(
                            title = "None (Off)",
                            isSelected = currentSubtitleIndex == null,
                            onClick = {
                                onSubtitleSelected(null)
                            }
                        )
                    }
                    
                    // Subtitle options
                    items(subtitleStreams.size) { index ->
                        val stream = subtitleStreams[index]
                        val streamIndex = stream.Index ?: 0
                        
                        // Build subtitle title
                        val subtitleTitle = buildString {
                            append(stream.DisplayTitle ?: stream.Language ?: "Unknown")
                            if (stream.IsForced == true) append(" [Forced]")
                            if (stream.IsExternal == true) append(" (External)")
                            if (stream.IsHearingImpaired == true) append(" [CC]")
                        }
                        
                        SubtitleOptionItem(
                            title = subtitleTitle,
                            isSelected = currentSubtitleIndex == streamIndex,
                            onClick = {
                                onSubtitleSelected(streamIndex)
                            }
                        )
                    }
                }
            }
            
            // Close button
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.focusable()
            ) {
                androidx.compose.material3.Text("Close", color = Color.White)
            }
        }
    }
}

@Composable
fun SubtitleOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                when {
                    isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    isFocused -> androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Selected",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )
        }
        
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AudioSelectionDialog(
    player: ExoPlayer,
    currentAudioIndex: Int?,
    onDismiss: () -> Unit,
    onAudioSelected: (Int?) -> Unit
) {
    val audioGroups = remember(player.currentTracks) {
        player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 500.dp)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                .padding(24.dp)
                .clickable(
                    onClick = { /* Prevent click from closing dialog */ },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "Select Audio Track",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(audioGroups.size) { index ->
                    val group = audioGroups[index]
                    val format = group.mediaTrackGroup.getFormat(0)
                    val trackTitle = buildString {
                        append(format.label ?: format.language ?: "Unknown")
                        format.codecs?.let { append(" • $it") }
                        if (format.channelCount > 0) append(" • ${format.channelCount}ch")
                    }
                    
                    SimpleOptionItem(
                        title = trackTitle,
                        isSelected = group.isSelected,
                        onClick = {
                            val updatedParameters = player.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .addOverride(
                                    TrackSelectionOverride(
                                        group.mediaTrackGroup,
                                        listOf(0)
                                    )
                                )
                                .build()
                            
                            player.trackSelectionParameters = updatedParameters
                            onAudioSelected(index)
                        }
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.focusable()
            ) {
                androidx.compose.material3.Text("Close", color = Color.White)
            }
        }
    }
}

@Composable
fun SpeedSelectionDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit
) {
    val speeds = remember { listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f) }
    var currentSpeed by remember { mutableStateOf(player.playbackParameters.speed) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .heightIn(max = 500.dp)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                .padding(24.dp)
                .clickable(
                    onClick = { /* Prevent click from closing dialog */ },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "Velocidad de reproducción",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(speeds.size) { index ->
                    val speed = speeds[index]
                    val speedText = when (speed) {
                        1.0f -> "Normal (1.0x)"
                        else -> "${speed}x"
                    }
                    
                    SimpleOptionItem(
                        title = speedText,
                        isSelected = (currentSpeed - speed) < 0.01f,
                        onClick = {
                            player.setPlaybackSpeed(speed)
                            currentSpeed = speed
                        }
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.focusable()
            ) {
                androidx.compose.material3.Text("Close", color = Color.White)
            }
        }
    }
}


@Composable
fun SimpleOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                when {
                    isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    isFocused -> androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )
        }
        
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Netflix-style Skip Intro / Skip Credits button
 * Positioned at bottom-right, D-pad focusable for Android TV
 */
@Composable
fun SkipButton(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus the skip button when it appears
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus errors
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .background(
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                )
                .then(
                    if (isFocused) {
                        Modifier.border(3.dp, Color.White, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                androidx.compose.material3.Text(
                    text = text,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

// YouTube TV-style player control button — now supports custom size for mobile
@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = when {
                    isFocused -> Color.White
                    else -> Color.White.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(50)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                color = Color.White,
                shape = RoundedCornerShape(50)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

// Format time in HH:MM:SS or MM:SS format
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

// Seekbar that supports D-pad (TV) and touch drag (mobile)
@Composable
private fun PlayerSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    
    // Seek step when progress bar is focused: 3% of duration or 30 seconds (much faster than regular seeking)
    // This allows users to quickly scrub through the video
    // For a 2-hour movie: ~3.6 minutes per press
    // For a 1-hour show: ~1.8 minutes per press
    // For a 30-min episode: ~54 seconds per press
    val seekStep = if (duration > 0) {
        maxOf(duration / 33, 30000L) // 3% of duration, minimum 30 seconds
    } else {
        30000L
    }
    
    val barHeight = if (isFocused) 12.dp else 6.dp
    val thumbSize = if (isFocused) 18.dp else 0.dp
    
    // Track whether user is dragging (mobile only)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isMobile) 36.dp else 24.dp) // Taller touch target on mobile
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .then(
                if (isMobile && duration > 0) {
                    Modifier.pointerInput(duration) {
                        detectTapGestures(
                            onPress = { offset ->
                                isDragging = true
                                dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                try {
                                    val position = awaitRelease()
                                    if (duration > 0) {
                                        onSeek((dragProgress * duration).toLong())
                                    }
                                } finally {
                                    isDragging = false
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .onPreviewKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            // Seek backward
                            val newPosition = (currentPosition - seekStep).coerceAtLeast(0)
                            onSeek(newPosition)
                            true
                        }
                        Key.DirectionRight -> {
                            // Seek forward
                            val newPosition = if (duration > 0) {
                                (currentPosition + seekStep).coerceAtMost(duration)
                            } else {
                                currentPosition + seekStep
                            }
                            onSeek(newPosition)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Use drag progress while dragging, otherwise actual position
        val displayProgress = if (isDragging) dragProgress
            else if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        val trackWidth = maxWidth
        val showThumb = isFocused || isDragging
        val thumbSizeActual = if (showThumb) (if (isMobile) 20.dp else 18.dp) else 0.dp
        val barHeightActual = when {
            isMobile && isDragging -> 10.dp
            isMobile -> 6.dp
            isFocused -> 12.dp
            else -> 6.dp
        }

        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightActual)
                .background(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(barHeightActual / 2)
                )
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(barHeightActual / 2)
                )
        ) {
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        color = if (isFocused || isDragging) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.White,
                        shape = RoundedCornerShape(barHeightActual / 2)
                    )
            )
        }
        
        // Thumb indicator — show when focused (TV) or dragging (mobile)
        if (showThumb && thumbSizeActual > 0.dp) {
            val thumbOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
                (trackWidth.toPx() * displayProgress.coerceIn(0f, 1f) - thumbSizeActual.toPx() / 2).toDp()
            }
            
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSizeActual)
                    .align(Alignment.CenterStart)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    )
                    .border(
                        width = 2.dp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

// Picture Mode / Aspect Ratio button
@Composable
private fun AspectModeButton(
    currentMode: AspectMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = when {
                    isFocused -> Color.White
                    else -> Color.White.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(50)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                color = Color.White,
                shape = RoundedCornerShape(50)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AspectRatio,
                contentDescription = "Modo de imagen: ${currentMode.label}",
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = currentMode.label,
                color = if (isFocused) Color.Black else Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

