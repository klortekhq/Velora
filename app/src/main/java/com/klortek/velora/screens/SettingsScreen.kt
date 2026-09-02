package com.klortek.velora.screens

import coil.annotation.ExperimentalCoilApi

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.klortek.velora.jellyfin.AppSettings
import com.klortek.velora.BuildConfig
import coil.ImageLoader
import coil.imageLoader
import coil.disk.DiskCache
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import android.widget.Toast
import com.klortek.velora.updater.GitHubRelease
import com.klortek.velora.updater.UpdateService
import android.content.pm.PackageManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.i18n.VeloraLocale
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

// Settings categories
enum class SettingsCategory(val title: String, val icon: ImageVector) {
    LANGUAGE("Idioma y reproducción", Icons.Default.Language),
    PLAYBACK("Reproducción", Icons.Default.PlayArrow),
    VIDEO("Vídeo", Icons.Default.Videocam),
    SUBTITLES("Audio y subtítulos", Icons.Default.Subtitles),
    APPEARANCE("Apariencia", Icons.Default.Palette),
    PERFORMANCE("Rendimiento", Icons.Default.Speed),
    LIBRARY("Biblioteca", Icons.Default.VideoLibrary),
    ADVANCED("Avanzado", Icons.Default.Settings),
    UPDATES("Actualizaciones", Icons.Default.Update),
    JELLYSEERR("Jellyseerr (Descubrir contenido)", Icons.Default.Videocam),
    TRAILERS("Tráilers", Icons.Default.Movie),
    ACCOUNT("Cuenta", Icons.Default.Person),
    ABOUT("Acerca de", Icons.Default.Info),
}

private fun SettingsCategory.localizedTitle(context: android.content.Context): String = when (this) {
    SettingsCategory.LANGUAGE -> context.getString(com.klortek.velora.R.string.settings_language_reproduction)
    SettingsCategory.PLAYBACK -> context.getString(com.klortek.velora.R.string.settings_category_playback)
    SettingsCategory.VIDEO -> context.getString(com.klortek.velora.R.string.settings_category_video)
    SettingsCategory.SUBTITLES -> context.getString(com.klortek.velora.R.string.settings_category_subtitles)
    SettingsCategory.APPEARANCE -> context.getString(com.klortek.velora.R.string.settings_category_appearance)
    SettingsCategory.PERFORMANCE -> context.getString(com.klortek.velora.R.string.settings_category_performance)
    SettingsCategory.LIBRARY -> context.getString(com.klortek.velora.R.string.settings_category_library)
    SettingsCategory.ADVANCED -> context.getString(com.klortek.velora.R.string.settings_category_advanced)
    SettingsCategory.UPDATES -> context.getString(com.klortek.velora.R.string.settings_category_updates)
    SettingsCategory.ACCOUNT -> context.getString(com.klortek.velora.R.string.settings_category_account)
    SettingsCategory.ABOUT -> context.getString(com.klortek.velora.R.string.settings_category_about)
    else -> title
}

// Mobile settings uses an explicit high-contrast palette. The app theme is
// TV-first and its inherited content colors are too subtle on phone panels.
private val MobileSettingsBackground = Color(0xFF090B10)
private val MobileSettingsText = Color(0xFFF5F7FA)
private val MobileSettingsSecondaryText = Color(0xFFB8C1CC)
private val MobileSettingsAccent = Color(0xFF25B8E8)
private val MobileSettingsDivider = Color(0xFF303846)
private val MobileSettingsSurface = Color(0xFF151A23)

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    initialCategory: SettingsCategory = SettingsCategory.PLAYBACK
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()
    
    // Selected category
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    
    // All settings state
    var mpvEnabled by remember { mutableStateOf(settings.isMpvEnabled) }
    
    // MPV download state
    var isMpvInstalled by remember { mutableStateOf(false) }
    var isMpvDownloading by remember { mutableStateOf(false) }
    var mpvDownloadProgress by remember { mutableStateOf(0f) }
    var mpvInstallCheckTrigger by remember { mutableStateOf(0) }
    
    // Check if the optional external MPV player is installed.
    LaunchedEffect(mpvInstallCheckTrigger) {
        isMpvInstalled = try {
            context.packageManager.getPackageInfo("is.xyz.mpv", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    // Periodically check for MPV installation after download is triggered
    LaunchedEffect(isMpvDownloading) {
        if (!isMpvDownloading && mpvInstallCheckTrigger > 0) {
            // After download completes, periodically check if MPV was installed
            repeat(10) { // Check for up to ~30 seconds
                kotlinx.coroutines.delay(3000)
                val nowInstalled = try {
                    context.packageManager.getPackageInfo("is.xyz.mpv", 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
                if (nowInstalled) {
                    isMpvInstalled = true
                    return@LaunchedEffect
                }
            }
        }
    }
    var debugOutlinesEnabled by remember { mutableStateOf(settings.showDebugOutlines) }
    var preloadLibraryImagesEnabled by remember { mutableStateOf(settings.preloadLibraryImages) }
    var cacheLibraryImagesEnabled by remember { mutableStateOf(settings.cacheLibraryImages) }
    var useGlideEnabled by remember { mutableStateOf(settings.useGlide) }
    var reducePosterResolutionEnabled by remember { mutableStateOf(settings.reducePosterResolution) }
    var animatedPlayButtonEnabled by remember { mutableStateOf(settings.useAnimatedPlayButton) }
    var use24HourTimeEnabled by remember { mutableStateOf(settings.use24HourTime) }
    var longPressDurationSeconds by remember { mutableStateOf(settings.longPressDurationSeconds) }
    var remoteThemingEnabled by remember { mutableStateOf(settings.remoteThemingEnabled) }
    var darkModeEnabled by remember { mutableStateOf(settings.darkModeEnabled) }
    var autoRefreshEnabled by remember { mutableStateOf(settings.autoRefreshEnabled) }
    var autoRefreshIntervalMinutes by remember { mutableStateOf(settings.autoRefreshIntervalMinutes) }
    var hideShowsWithZeroEpisodesEnabled by remember { mutableStateOf(settings.hideShowsWithZeroEpisodes) }
    var minimalBuffer4KEnabled by remember { mutableStateOf(settings.minimalBuffer4K) }
    var transcodeAacToAc3Enabled by remember { mutableStateOf(settings.transcodeAacToAc3) }
    var useLogoForTitleEnabled by remember { mutableStateOf(settings.useLogoForTitle) }
    var autoplayNextEpisodeEnabled by remember { mutableStateOf(settings.autoplayNextEpisode) }
    var autoplayCountdownSeconds by remember { mutableStateOf(settings.autoplayCountdownSeconds) }
    var autoUpdateEnabled by remember { mutableStateOf(settings.autoUpdateEnabled) }
    var skipIntroEnabled by remember { mutableStateOf(settings.skipIntroEnabled) }
    var skipCreditsEnabled by remember { mutableStateOf(settings.skipCreditsEnabled) }
    
    // Server-side transcoding settings
    var serverTranscodingEnabled by remember { mutableStateOf(settings.serverTranscodingEnabled) }
    var transcodeAV1 by remember { mutableStateOf(settings.transcodeAV1) }
    var transcodeHEVC by remember { mutableStateOf(settings.transcodeHEVC) }
    var transcodeTargetCodec by remember { mutableStateOf(settings.transcodeTargetCodec) }
    var transcodeMaxBitrate by remember { mutableStateOf(settings.transcodeMaxBitrateMbps) }
    var autoTranscodeOnError by remember { mutableStateOf(settings.autoTranscodeOnError) }
    var fallbackToMpv by remember { mutableStateOf(settings.fallbackToMpv) }
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<com.klortek.velora.updater.GitHubRelease?>(null) }
    var checkingForUpdates by remember { mutableStateOf(false) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }

    // ExoPlayer Subtitle customization settings
    var exoSubtitleTextSize by remember { mutableStateOf(settings.exoSubtitleTextSize) }
    var exoSubtitleBgTransparent by remember { mutableStateOf(settings.exoSubtitleBgTransparent) }
    var showExoSubtitleColorDialog by remember { mutableStateOf(false) }
    var showExoSubtitleBgColorDialog by remember { mutableStateOf(false) }

    // Video Enhancement settings
    var useGLEnhancements by remember { mutableStateOf(settings.useGLEnhancements) }
    var enableFakeHDR by remember { mutableStateOf(settings.enableFakeHDR) }
    var enableSharpening by remember { mutableStateOf(settings.enableSharpening) }
    var hdrStrength by remember { mutableStateOf(settings.hdrStrength) }
    var sharpenStrength by remember { mutableStateOf(settings.sharpenStrength) }
    var enableFrameBlending by remember { mutableStateOf(settings.enableFrameBlending) }
    var frameBlendStrength by remember { mutableStateOf(settings.frameBlendStrength) }
    
    // UI Performance settings
    var disableUIAnimations by remember { mutableStateOf(settings.disableUIAnimations) }
    var useSimpleCards by remember { mutableStateOf(settings.useSimpleCards) }
    var useGoogleTvCards by remember { mutableStateOf(settings.useGoogleTvCards) }
    var lowPowerMode by remember { mutableStateOf(settings.lowPowerMode) }
    var use4KBackgrounds by remember { mutableStateOf(settings.use4KBackgrounds) }
    var navigationSoundsEnabled by remember { mutableStateOf(settings.navigationSoundsEnabled) }
    var themeMusicEnabled by remember { mutableStateOf(settings.themeMusicEnabled) }
    
    // Logout confirmation
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Global language and playback preference state. These are shared by mobile,
    // tablet, Android TV and Fire TV because they live in AppSettings.
    var languageTag by remember { mutableStateOf(settings.languageTag) }
    var preferredAudioLanguage by remember { mutableStateOf(settings.preferredAudioLanguage) }
    var subtitleMode by remember { mutableStateOf(settings.subtitleMode) }
    var preferredSubtitleLanguage by remember { mutableStateOf(settings.preferredSubtitleLanguage) }

    // Jellyseerr state variables
    var jellyseerrUrl by remember { mutableStateOf(settings.jellyseerrUrl) }
    var showJellyseerrUrlDialog by remember { mutableStateOf(false) }
    var jellyseerrAuthType by remember { mutableStateOf(settings.jellyseerrAuthType) }
    var jellyseerrApiKey by remember { mutableStateOf(settings.jellyseerrApiKey) }
    var jellyseerrUsername by remember { mutableStateOf(settings.jellyseerrUsername) }
    var jellyseerrSessionCookie by remember { mutableStateOf(settings.jellyseerrSessionCookie) }
    var showJellyseerrApiKeyDialog by remember { mutableStateOf(false) }
    var showJellyseerrLoginDialog by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var jellyseerrEnabled by remember { mutableStateOf(settings.jellyseerrEnabled) }
    var jellyseerrSearchEnabled by remember { mutableStateOf(settings.jellyseerrSearchEnabled) }
    
    // MPV Shader Profile
    var mpvShaderProfile by remember { mutableStateOf(settings.mpvShaderProfile) }

    // OpenSubtitles state variables
    var openSubtitlesApiKey by remember { mutableStateOf(settings.openSubtitlesApiKey) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var openSubtitlesUsername by remember { mutableStateOf(settings.openSubtitlesUsername) }
    var openSubtitlesPassword by remember { mutableStateOf(settings.openSubtitlesPassword) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showClearSubtitlesDialog by remember { mutableStateOf(false) }
    var rowCardCount by remember { mutableStateOf(settings.rowCardCount) }
    var offlineMaxStorageBytes by remember { mutableStateOf(settings.offlineMaxStorageBytes) }
    var offlineWifiOnly by remember { mutableStateOf(settings.offlineWifiOnly) }
    var smartDownloadsEnabled by remember { mutableStateOf(settings.smartDownloadsEnabled) }
    var smartDownloadsRemoveWatched by remember { mutableStateOf(settings.smartDownloadsRemoveWatched) }
    var smartDownloadsKeepUnwatchedEpisodes by remember { mutableStateOf(settings.smartDownloadsKeepUnwatchedEpisodes) }
    var downloadedSubtitlesCount by remember { mutableStateOf(0) }

    val isTv = remember(context) { com.klortek.velora.ui.DeviceUtils.isTvDevice(context) }
    var activeCategoryDetail by remember { mutableStateOf<SettingsCategory?>(null) }

    BackHandler(enabled = !isTv && activeCategoryDetail != null) {
        activeCategoryDetail = null
    }

    @Composable
    fun SettingsOptions(category: SettingsCategory) {
        when (category) {
                        SettingsCategory.LANGUAGE -> {
                            val languageOptions = VeloraLocale.languages
                            val audioOptions = listOf(VeloraLocale.AUTO) + languageOptions.filter { it.tag != VeloraLocale.AUTO }.map { it.tag }
                            val subtitleLanguageOptions = listOf(VeloraLocale.AUTO) + languageOptions.filter { it.tag != VeloraLocale.AUTO }.map { it.tag }
                            val languageLabel = languageOptions.firstOrNull { it.tag == languageTag }?.nativeLabel ?: languageTag
                            val audioLabel = if (preferredAudioLanguage == VeloraLocale.AUTO) {
                                context.getString(com.klortek.velora.R.string.settings_audio_auto)
                            } else {
                                languageOptions.firstOrNull { it.tag == preferredAudioLanguage }?.nativeLabel ?: preferredAudioLanguage
                            }
                            val subtitleLanguageLabel = if (preferredSubtitleLanguage == VeloraLocale.AUTO) {
                                context.getString(com.klortek.velora.R.string.settings_audio_auto)
                            } else {
                                languageOptions.firstOrNull { it.tag == preferredSubtitleLanguage }?.nativeLabel ?: preferredSubtitleLanguage
                            }
                            val subtitleModeLabel = when (subtitleMode) {
                                VeloraLocale.SUBTITLES_PREFERRED -> context.getString(com.klortek.velora.R.string.settings_subtitle_preferred)
                                VeloraLocale.SUBTITLES_FORCED -> context.getString(com.klortek.velora.R.string.settings_subtitle_forced)
                                VeloraLocale.SUBTITLES_AUTO -> context.getString(com.klortek.velora.R.string.settings_subtitle_auto)
                                else -> context.getString(com.klortek.velora.R.string.settings_subtitle_off)
                            }

                            SettingCycle(
                                title = context.getString(com.klortek.velora.R.string.settings_app_language),
                                description = context.getString(com.klortek.velora.R.string.settings_app_language_description),
                                currentValue = languageLabel,
                                onCycle = {
                                    val currentIndex = languageOptions.indexOfFirst { it.tag == languageTag }.coerceAtLeast(0)
                                    val next = languageOptions[(currentIndex + 1) % languageOptions.size].tag
                                    languageTag = next
                                    settings.languageTag = next
                                    VeloraLocale.restart(context)
                                }
                            )
                            SettingCycle(
                                title = context.getString(com.klortek.velora.R.string.settings_audio_language),
                                description = context.getString(com.klortek.velora.R.string.settings_audio_language_description),
                                currentValue = audioLabel,
                                onCycle = {
                                    val currentIndex = audioOptions.indexOf(preferredAudioLanguage).coerceAtLeast(0)
                                    val next = audioOptions[(currentIndex + 1) % audioOptions.size]
                                    preferredAudioLanguage = next
                                    settings.preferredAudioLanguage = next
                                }
                            )
                            SettingCycle(
                                title = context.getString(com.klortek.velora.R.string.settings_subtitle_mode),
                                description = context.getString(com.klortek.velora.R.string.settings_subtitle_mode_description),
                                currentValue = subtitleModeLabel,
                                onCycle = {
                                    val options = listOf(VeloraLocale.SUBTITLES_OFF, VeloraLocale.SUBTITLES_PREFERRED, VeloraLocale.SUBTITLES_FORCED, VeloraLocale.SUBTITLES_AUTO)
                                    val currentIndex = options.indexOf(subtitleMode).coerceAtLeast(0)
                                    val next = options[(currentIndex + 1) % options.size]
                                    subtitleMode = next
                                    settings.subtitleMode = next
                                }
                            )
                            SettingCycle(
                                title = context.getString(com.klortek.velora.R.string.settings_subtitle_language),
                                description = context.getString(com.klortek.velora.R.string.settings_subtitle_language_description),
                                currentValue = subtitleLanguageLabel,
                                onCycle = {
                                    val currentIndex = subtitleLanguageOptions.indexOf(preferredSubtitleLanguage).coerceAtLeast(0)
                                    val next = subtitleLanguageOptions[(currentIndex + 1) % subtitleLanguageOptions.size]
                                    preferredSubtitleLanguage = next
                                    settings.preferredSubtitleLanguage = next
                                }
                            )
                        }
                        SettingsCategory.PLAYBACK -> {
                            // MPV Player Toggle
                            SettingToggle(
                                title = "Usar reproductor MPV",
                                description = "Usa el reproductor MPV integrado para mejorar la compatibilidad con AV1, HEVC y HDR.",
                                isEnabled = mpvEnabled,
                                onToggle = {
                                    mpvEnabled = !mpvEnabled
                                    settings.isMpvEnabled = mpvEnabled
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Skip Intro
                            SettingToggle(
                                title = "Saltar intro",
                                description = "Muestra un botón para saltar las intros de los episodios (requiere el complemento Intro Skipper).",
                                isEnabled = skipIntroEnabled,
                                onToggle = {
                                    skipIntroEnabled = !skipIntroEnabled
                                    settings.skipIntroEnabled = skipIntroEnabled
                                }
                            )
                            
                            // Skip Credits
                            SettingToggle(
                                title = "Saltar créditos",
                                description = "Muestra un botón para saltar los créditos finales de los episodios.",
                                isEnabled = skipCreditsEnabled,
                                onToggle = {
                                    skipCreditsEnabled = !skipCreditsEnabled
                                    settings.skipCreditsEnabled = skipCreditsEnabled
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Server-Side Transcoding Section Header
                            Text(
                                text = "Transcodificación en el servidor",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            // Auto-transcode on playback error
                            SettingToggle(
                                title = "Transcodificar automáticamente si falla",
                                description = "Reintenta automáticamente con transcodificación del servidor si falla la reproducción directa.",
                                isEnabled = autoTranscodeOnError,
                                onToggle = {
                                    autoTranscodeOnError = !autoTranscodeOnError
                                    settings.autoTranscodeOnError = autoTranscodeOnError
                                }
                            )
                            
                            // Fallback to MPV player
                            SettingToggle(
                                title = "Usar MPV como alternativa",
                                description = if (isMpvInstalled) {
                                    "Usa MPV si falla ExoPlayer y la transcodificación está desactivada (MPV instalado ✓)"
                                } else {
                                    "Usa MPV si falla ExoPlayer (requiere tener instalado un reproductor MPV externo)"
                                },
                                isEnabled = fallbackToMpv,
                                onToggle = {
                                    fallbackToMpv = !fallbackToMpv
                                    settings.fallbackToMpv = fallbackToMpv
                                }
                            )
                            
                            // Server Transcoding Master Toggle
                            SettingToggle(
                                title = "Transcodificar siempre",
                                description = "Solicita siempre transcodificación del servidor para los códecs seleccionados (AV1, HEVC).",
                                isEnabled = serverTranscodingEnabled,
                                onToggle = {
                                    serverTranscodingEnabled = !serverTranscodingEnabled
                                    settings.serverTranscodingEnabled = serverTranscodingEnabled
                                }
                            )
                            
                            if (serverTranscodingEnabled) {
                                // Transcode AV1
                                SettingToggle(
                                    title = "Transcodificar AV1",
                                    description = "Solicita transcodificación para vídeo AV1 (recomendado para Shield TV).",
                                    isEnabled = transcodeAV1,
                                    onToggle = {
                                        transcodeAV1 = !transcodeAV1
                                        settings.transcodeAV1 = transcodeAV1
                                    }
                                )
                                
                                // Transcode HEVC
                                SettingToggle(
                                    title = "Transcodificar HEVC/H.265",
                                    description = "Solicita transcodificación para vídeo HEVC/H.265 (solo si el dispositivo no lo admite).",
                                    isEnabled = transcodeHEVC,
                                    onToggle = {
                                        transcodeHEVC = !transcodeHEVC
                                        settings.transcodeHEVC = transcodeHEVC
                                    }
                                )
                                
                                // Target Codec
                                SettingCycle(
                                    title = "Códec de destino",
                                    description = "Transcodificar a: ${transcodeTargetCodec.uppercase()}",
                                    currentValue = transcodeTargetCodec.uppercase(),
                                    onCycle = {
                                        transcodeTargetCodec = if (transcodeTargetCodec == "h264") "hevc" else "h264"
                                        settings.transcodeTargetCodec = transcodeTargetCodec
                                    }
                                )
                                
                                // Max Bitrate
                                SettingSlider(
                                    title = "Bitrate máximo de vídeo",
                                    description = "${transcodeMaxBitrate} Mbps (más alto = mejor calidad)",
                                    onDecrease = {
                                        transcodeMaxBitrate = (transcodeMaxBitrate - 5).coerceAtLeast(5)
                                        settings.transcodeMaxBitrateMbps = transcodeMaxBitrate
                                    },
                                    onIncrease = {
                                        transcodeMaxBitrate = (transcodeMaxBitrate + 5).coerceAtMost(120)
                                        settings.transcodeMaxBitrateMbps = transcodeMaxBitrate
                                    },
                                    canDecrease = transcodeMaxBitrate > 5,
                                    canIncrease = transcodeMaxBitrate < 120
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Autoplay Next Episode
                            SettingToggle(
                                title = "Reproducir el siguiente episodio",
                                description = "Reproduce automáticamente el siguiente episodio al terminar el actual.",
                                isEnabled = autoplayNextEpisodeEnabled,
                                onToggle = {
                                    autoplayNextEpisodeEnabled = !autoplayNextEpisodeEnabled
                                    settings.autoplayNextEpisode = autoplayNextEpisodeEnabled
                                }
                            )
                            
                            // Autoplay Countdown Duration
                            if (autoplayNextEpisodeEnabled) {
                                SettingCycle(
                                    title = "Cuenta atrás automática",
                                    description = "Tiempo antes de terminar el episodio para mostrar la cuenta atrás (${autoplayCountdownSeconds}s).",
                                    currentValue = "${autoplayCountdownSeconds}s",
                                    onCycle = {
                                        autoplayCountdownSeconds = when (autoplayCountdownSeconds) {
                                            10 -> 15
                                            15 -> 30
                                            30 -> 45
                                            45 -> 60
                                            60 -> 90
                                            90 -> 120
                                            120 -> 10
                                            else -> 10
                                        }
                                        settings.autoplayCountdownSeconds = autoplayCountdownSeconds
                                    }
                                )
                            }
                        }
                        
                        SettingsCategory.VIDEO -> {
                            // ExoPlayer GL Enhancements
                            SettingToggle(
                                title = "Procesado GL de ExoPlayer",
                                description = "Usa OpenGL para efectos de vídeo avanzados en ExoPlayer (simulación HDR y nitidez).",
                                isEnabled = useGLEnhancements,
                                onToggle = {
                                    useGLEnhancements = !useGLEnhancements
                                    settings.useGLEnhancements = useGLEnhancements
                                    if (!useGLEnhancements) {
                                        enableFakeHDR = false
                                        enableSharpening = false
                                        enableFrameBlending = false
                                        settings.enableFakeHDR = false
                                        settings.enableSharpening = false
                                        settings.enableFrameBlending = false
                                    }
                                }
                            )

                            // MPV Post-Processing
                            // Show Dynamic Tone Mapping toggle if relevant, or just keep it independent
                            
                            // Dynamic Tone Mapping Toggle
                            var enableDynamicToneMapping by remember { mutableStateOf(settings.enableDynamicToneMapping) }
                            SettingToggle(
                                title = "Activar mapeo dinámico de tonos",
                                description = "Activa la simulación HDR adaptada a cada escena. Mejora el contraste dinámicamente.",
                                isEnabled = enableDynamicToneMapping,
                                onToggle = {
                                    enableDynamicToneMapping = !enableDynamicToneMapping
                                    settings.enableDynamicToneMapping = enableDynamicToneMapping
                                }
                            )

                            SettingCycle(
                                title = "Postprocesado de MPV",
                                description = "Aplica perfiles de shaders al reproductor MPV (efectos tipo HDR, nitidez, etc.).",
                                currentValue = com.klortek.velora.player.mpv.MpvShaderManager.ShaderProfile.fromString(mpvShaderProfile).displayName,
                                onCycle = {
                                    val currentProfile = com.klortek.velora.player.mpv.MpvShaderManager.ShaderProfile.fromString(mpvShaderProfile)
                                    val allProfiles = com.klortek.velora.player.mpv.MpvShaderManager.ShaderProfile.entries
                                    val nextIndex = (allProfiles.indexOf(currentProfile) + 1) % allProfiles.size
                                    val nextProfile = allProfiles[nextIndex]
                                    
                                    mpvShaderProfile = nextProfile.name
                                    settings.mpvShaderProfile = nextProfile.name
                                }
                            )
                            
                            if (useGLEnhancements) {
                                // Fake HDR
                                SettingToggle(
                                    title = "HDR simulado",
                                    description = "Simula HDR mediante mapeo de tonos y aumento de brillo.",
                                    isEnabled = enableFakeHDR,
                                    onToggle = {
                                        enableFakeHDR = !enableFakeHDR
                                        settings.enableFakeHDR = enableFakeHDR
                                    }
                                )
                                
                                if (enableFakeHDR) {
                                    SettingSlider(
                                        title = "Intensidad HDR",
                                        description = "Intensidad: %.1f (rango: 1,0-2,0)".format(hdrStrength),
                                        onDecrease = {
                                            hdrStrength = (hdrStrength - 0.1f).coerceAtLeast(1.0f)
                                            settings.hdrStrength = hdrStrength
                                        },
                                        onIncrease = {
                                            hdrStrength = (hdrStrength + 0.1f).coerceAtMost(2.0f)
                                            settings.hdrStrength = hdrStrength
                                        },
                                        canDecrease = hdrStrength > 1.0f,
                                        canIncrease = hdrStrength < 2.0f
                                    )
                                }
                                
                                // Sharpening
                                SettingToggle(
                                    title = "Nitidez",
                                    description = "Mejora la nitidez de la imagen mediante detección de bordes.",
                                    isEnabled = enableSharpening,
                                    onToggle = {
                                        enableSharpening = !enableSharpening
                                        settings.enableSharpening = enableSharpening
                                    }
                                )
                                
                                if (enableSharpening) {
                                    SettingSlider(
                                        title = "Intensidad de nitidez",
                                        description = "Intensidad: %.1f (rango: 0,0-1,0)".format(sharpenStrength),
                                        onDecrease = {
                                            sharpenStrength = (sharpenStrength - 0.1f).coerceAtLeast(0.0f)
                                            settings.sharpenStrength = sharpenStrength
                                        },
                                        onIncrease = {
                                            sharpenStrength = (sharpenStrength + 0.1f).coerceAtMost(1.0f)
                                            settings.sharpenStrength = sharpenStrength
                                        },
                                        canDecrease = sharpenStrength > 0.0f,
                                        canIncrease = sharpenStrength < 1.0f
                                    )
                                }
                                
                                // Frame Blending
                                SettingToggle(
                                    title = "Mezcla de fotogramas",
                                    description = "Simula movimiento suave mezclando fotogramas (efecto telenovela).",
                                    isEnabled = enableFrameBlending,
                                    onToggle = {
                                        enableFrameBlending = !enableFrameBlending
                                        settings.enableFrameBlending = enableFrameBlending
                                    }
                                )
                                
                                if (enableFrameBlending) {
                                    SettingSlider(
                                        title = "Intensidad de mezcla",
                                        description = "Intensidad: %.1f (rango: 0,0-1,0)".format(frameBlendStrength),
                                        onDecrease = {
                                            frameBlendStrength = (frameBlendStrength - 0.1f).coerceAtLeast(0.0f)
                                            settings.frameBlendStrength = frameBlendStrength
                                        },
                                        onIncrease = {
                                            frameBlendStrength = (frameBlendStrength + 0.1f).coerceAtMost(1.0f)
                                            settings.frameBlendStrength = frameBlendStrength
                                        },
                                        canDecrease = frameBlendStrength > 0.0f,
                                        canIncrease = frameBlendStrength < 1.0f
                                    )
                                }
                                
                            }
                        }
                        
                        SettingsCategory.SUBTITLES -> {
                            // ExoPlayer Subtitle Text Size
                            SettingSlider(
                                title = "Tamaño del texto de subtítulos",
                                description = "Tamaño: $exoSubtitleTextSize (rango: 20-100)",
                                onDecrease = {
                                    if (exoSubtitleTextSize > 20) {
                                        exoSubtitleTextSize -= 5
                                        settings.exoSubtitleTextSize = exoSubtitleTextSize
                                    }
                                },
                                onIncrease = {
                                    if (exoSubtitleTextSize < 100) {
                                        exoSubtitleTextSize += 5
                                        settings.exoSubtitleTextSize = exoSubtitleTextSize
                                    }
                                },
                                canDecrease = exoSubtitleTextSize > 20,
                                canIncrease = exoSubtitleTextSize < 100
                            )
                            
                            // Subtitle Text Color
                            SettingButton(
                                title = "Color del texto de subtítulos",
                                description = "Elige el color del texto de los subtítulos.",
                                buttonText = "Elegir color",
                                onClick = { showExoSubtitleColorDialog = true }
                            )
                            
                            // Subtitle Background Transparency
                            SettingToggle(
                                title = "Fondo transparente de subtítulos",
                                description = "Hace que el fondo de los subtítulos sea transparente u opaco.",
                                isEnabled = exoSubtitleBgTransparent,
                                onToggle = {
                                    exoSubtitleBgTransparent = !exoSubtitleBgTransparent
                                    settings.exoSubtitleBgTransparent = exoSubtitleBgTransparent
                                },
                                enabledText = "Transparente",
                                disabledText = "Opaco"
                            )
                            
                            // Subtitle Background Color
                            if (!exoSubtitleBgTransparent) {
                                SettingButton(
                                    title = "Color del fondo de subtítulos",
                                    description = "Elige el color del fondo de los subtítulos.",
                                    buttonText = "Elegir color",
                                    onClick = { showExoSubtitleBgColorDialog = true }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // OpenSubtitles API Key
                            SettingButton(
                                title = "Clave API de OpenSubtitles",
                                description = if (openSubtitlesApiKey.isNotBlank()) 
                                    "Clave API configurada ✓" 
                                else 
                                    "Necesaria para descargar subtítulos. Consigue una clave gratuita en opensubtitles.com",
                                buttonText = if (openSubtitlesApiKey.isNotBlank()) "Cambiar" else "Definir clave",
                                onClick = { showApiKeyDialog = true }
                            )
                            
                            if (showApiKeyDialog) {
                                var apiKeyInput by remember { mutableStateOf(openSubtitlesApiKey) }
                                AlertDialog(
                                    onDismissRequest = { showApiKeyDialog = false },
                                            title = { Text("Clave API de OpenSubtitles") },
                                    text = {
                                        Column {
                                            Text(
                                                "Consigue tu clave API gratuita en:\nhttps://www.opensubtitles.com/en/consumers\n\nPlan gratuito: 100 descargas al día",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            OutlinedTextField(
                                                value = apiKeyInput,
                                                onValueChange = { apiKeyInput = it },
                                                label = { Text("Clave API") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                openSubtitlesApiKey = apiKeyInput
                                                settings.openSubtitlesApiKey = apiKeyInput
                                                showApiKeyDialog = false
                                            }
                                        ) {
                                            Text("Guardar")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showApiKeyDialog = false }) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
                            
                            SettingButton(
                                title = "Acceso a OpenSubtitles",
                                description = if (openSubtitlesUsername.isNotBlank()) 
                                    "Sesión iniciada como: $openSubtitlesUsername ✓" 
                                else 
                                    "Necesario para descargar subtítulos",
                                buttonText = if (openSubtitlesUsername.isNotBlank()) "Cambiar" else "Iniciar sesión",
                                onClick = { showLoginDialog = true }
                            )
                            
                            if (showLoginDialog) {
                                var usernameInput by remember { mutableStateOf(openSubtitlesUsername) }
                                var passwordInput by remember { mutableStateOf(openSubtitlesPassword) }
                                AlertDialog(
                                    onDismissRequest = { showLoginDialog = false },
                                            title = { Text("Acceso a OpenSubtitles") },
                                    text = {
                                        Column {
                                            Text(
                                                "Introduce las credenciales de tu cuenta de OpenSubtitles.com.\nCrea una cuenta gratuita en opensubtitles.com si la necesitas.",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            OutlinedTextField(
                                                value = usernameInput,
                                                onValueChange = { usernameInput = it },
                                                label = { Text("Usuario") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = passwordInput,
                                                onValueChange = { passwordInput = it },
                                                label = { Text("Contraseña") },
                                                singleLine = true,
                                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                openSubtitlesUsername = usernameInput
                                                openSubtitlesPassword = passwordInput
                                                settings.openSubtitlesUsername = usernameInput
                                                settings.openSubtitlesPassword = passwordInput
                                                showLoginDialog = false
                                            }
                                        ) {
                                            Text("Guardar")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showLoginDialog = false }) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
                            
                            // Clear Downloaded Subtitles
                            // Count downloaded subtitles on first composition
                            LaunchedEffect(Unit) {
                                val subtitlesDir = java.io.File(context.filesDir, "downloaded_subtitles")
                                downloadedSubtitlesCount = if (subtitlesDir.exists()) {
                                    subtitlesDir.walkTopDown()
                                        .filter { it.isFile && it.extension in listOf("srt", "vtt", "ass", "ssa", "sub") }
                                        .count()
                                } else 0
                            }
                            
                            SettingButton(
                                title = "Borrar subtítulos descargados",
                                description = if (downloadedSubtitlesCount > 0) 
                                    "$downloadedSubtitlesCount archivo(s) de subtítulos guardado(s) localmente" 
                                else 
                                    "No hay subtítulos descargados",
                                buttonText = "Borrar",
                                onClick = { showClearSubtitlesDialog = true }
                            )
                            
                            if (showClearSubtitlesDialog) {
                                AlertDialog(
                                    onDismissRequest = { showClearSubtitlesDialog = false },
                                    title = { Text("¿Borrar subtítulos descargados?") },
                                    text = {
                                        Text(
                                            "Se eliminarán los $downloadedSubtitlesCount archivo(s) de subtítulos descargado(s) de OpenSubtitles.\n\nPodrás descargarlos de nuevo cuando quieras.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                // Delete all downloaded subtitles
                                                val subtitlesDir = java.io.File(context.filesDir, "downloaded_subtitles")
                                                if (subtitlesDir.exists()) {
                                                    subtitlesDir.deleteRecursively()
                                                    android.util.Log.d("Settings", "Cleared all downloaded subtitles")
                                                }
                                                downloadedSubtitlesCount = 0
                                                showClearSubtitlesDialog = false
                                                
                                                // Show toast
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Downloaded subtitles cleared",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Text("Borrar", color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearSubtitlesDialog = false }) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Transcode AAC to AC3
                            SettingToggle(
                                title = "Transcodificar AAC a AC3",
                                description = "Transcodifica todo el audio AAC a AC3 (máximo 5.1). AC3 es compatible universalmente.",
                                isEnabled = transcodeAacToAc3Enabled,
                                onToggle = {
                                    transcodeAacToAc3Enabled = !transcodeAacToAc3Enabled
                                    settings.transcodeAacToAc3 = transcodeAacToAc3Enabled
                                }
                            )
                        }
                        
                        SettingsCategory.JELLYSEERR -> {
                            // Jellyseerr URL
                            SettingButton(
                                title = "URL de Jellyseerr",
                                description = if (jellyseerrUrl.isNotBlank()) 
                                    jellyseerrUrl
                                else 
                                    "Define la URL de tu servidor Jellyseerr/Overseerr",
                                buttonText = if (jellyseerrUrl.isNotBlank()) "Cambiar" else "Definir URL",
                                onClick = { showJellyseerrUrlDialog = true }
                            )
                            
                            if (showJellyseerrUrlDialog) {
                                var urlInput by remember { mutableStateOf(jellyseerrUrl) }
                                Dialog(
                                    onDismissRequest = { showJellyseerrUrlDialog = false },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val content = @Composable {
                                            Column(
                                                modifier = Modifier.padding(if (isTv) 32.dp else 24.dp),
                                                verticalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                Text(
                                                    text = "URL de Jellyseerr",
                                                    style = if (isTv) MaterialTheme.typography.headlineSmall else androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                                    color = if (isTv) MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                                )
                                                
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "Introduce la URL completa de tu instancia de Jellyseerr (por ejemplo, http://192.168.1.50:5055)",
                                                        style = if (isTv) MaterialTheme.typography.bodyMedium else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                        color = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                    
                                                    OutlinedTextField(
                                                         value = urlInput,
                                                         onValueChange = { urlInput = it },
                                                         label = { Text("URL") },
                                                         placeholder = { Text("http://ip:port") },
                                                         singleLine = true,
                                                         modifier = Modifier.fillMaxWidth(),
                                                         colors = TextFieldDefaults.colors(
                                                             focusedTextColor = Color.White,
                                                             unfocusedTextColor = Color.White,
                                                             focusedContainerColor = Color.Transparent,
                                                             unfocusedContainerColor = Color.Transparent,
                                                             cursorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                             focusedLabelColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                             unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                                             focusedIndicatorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                             unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                                                         )
                                                     )
                                                }
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    if (isTv) {
                                                        Button(
                                                            onClick = { showJellyseerrUrlDialog = false },
                                                            modifier = Modifier.weight(1f),
                                                            colors = ButtonDefaults.colors(
                                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                        ) {
                                                            Text("Cancelar")
                                                        }
                                                        
                                                        Button(
                                                            onClick = {
                                                                jellyseerrUrl = urlInput
                                                                settings.jellyseerrUrl = urlInput
                                                                showJellyseerrUrlDialog = false
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("Guardar")
                                                        }
                                                    } else {
                                                        androidx.compose.material3.Button(
                                                            onClick = { showJellyseerrUrlDialog = false },
                                                            modifier = Modifier.weight(1f),
                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        ) {
                                                            androidx.compose.material3.Text("Cancelar", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        
                                                        androidx.compose.material3.Button(
                                                            onClick = {
                                                                jellyseerrUrl = urlInput
                                                                settings.jellyseerrUrl = urlInput
                                                                showJellyseerrUrlDialog = false
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            androidx.compose.material3.Text("Guardar")
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (isTv) {
                                            Surface(
                                                modifier = Modifier.width(500.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = SurfaceDefaults.colors(
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    contentColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                content = { content() }
                                            )
                                        } else {
                                            androidx.compose.material3.Surface(
                                                modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                                content = { content() }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Authentication method toggle
                            SettingCycle(
                                title = "Método de autenticación",
                                description = when (jellyseerrAuthType) {
                                    "api_key" -> "Usando clave API (recomendado para acceso de administrador)"
                                    "credentials" -> "Usando acceso con usuario y contraseña"
                                    else -> "Selecciona el método de autenticación"
                                },
                                currentValue = when (jellyseerrAuthType) {
                                    "api_key" -> "Clave API"
                                    "credentials" -> "Usuario y contraseña"
                                    else -> "Clave API"
                                },
                                onCycle = {
                                    jellyseerrAuthType = when (jellyseerrAuthType) {
                                        "api_key" -> "credentials"
                                        else -> "api_key"
                                    }
                                    settings.jellyseerrAuthType = jellyseerrAuthType
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Show appropriate authentication option based on type
                            if (jellyseerrAuthType == "api_key") {
                                // API Key authentication
                                SettingButton(
                                    title = "Clave API de Jellyseerr",
                                    description = if (jellyseerrApiKey.isNotBlank()) 
                                        "Clave API configurada ✓" 
                                    else 
                                        "Consíguela en Ajustes > General de Jellyseerr",
                                    buttonText = if (jellyseerrApiKey.isNotBlank()) "Cambiar" else "Definir clave",
                                    onClick = { showJellyseerrApiKeyDialog = true }
                                )
                                
                                if (showJellyseerrApiKeyDialog) {
                                    var apiKeyInput by remember { mutableStateOf(jellyseerrApiKey) }
                                    Dialog(
                                        onDismissRequest = { showJellyseerrApiKeyDialog = false },
                                        properties = DialogProperties(usePlatformDefaultWidth = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.7f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val content = @Composable {
                                                Column(
                                                    modifier = Modifier.padding(if (isTv) 32.dp else 24.dp),
                                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                                ) {
                                                    Text(
                                                        text = "Clave API de Jellyseerr",
                                                        style = if (isTv) MaterialTheme.typography.headlineSmall else androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                                        color = if (isTv) MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                                    )
                                                    
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(
                                                            text = "Introduce tu clave API de Jellyseerr. Está en Jellyseerr: Ajustes > General > Clave API.",
                                                            style = if (isTv) MaterialTheme.typography.bodyMedium else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                            color = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                        )
                                                        
                                                        OutlinedTextField(
                                                             value = apiKeyInput,
                                                             onValueChange = { apiKeyInput = it },
                                                             label = { Text("Clave API") },
                                                             singleLine = true,
                                                             modifier = Modifier.fillMaxWidth(),
                                                             colors = TextFieldDefaults.colors(
                                                                 focusedTextColor = Color.White,
                                                                 unfocusedTextColor = Color.White,
                                                                 focusedContainerColor = Color.Transparent,
                                                                 unfocusedContainerColor = Color.Transparent,
                                                                 cursorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 focusedLabelColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                                                 focusedIndicatorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                                                             )
                                                         )
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        if (isTv) {
                                                            Button(
                                                                onClick = { showJellyseerrApiKeyDialog = false },
                                                                modifier = Modifier.weight(1f),
                                                                colors = ButtonDefaults.colors(
                                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                )
                                                            ) {
                                                                Text("Cancelar")
                                                            }
                                                            
                                                            Button(
                                                                onClick = {
                                                                    jellyseerrApiKey = apiKeyInput
                                                                    settings.jellyseerrApiKey = apiKeyInput
                                                                    showJellyseerrApiKeyDialog = false
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text("Guardar")
                                                            }
                                                        } else {
                                                            androidx.compose.material3.Button(
                                                                onClick = { showJellyseerrApiKeyDialog = false },
                                                                modifier = Modifier.weight(1f),
                                                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                            ) {
                                                                androidx.compose.material3.Text("Cancelar", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            
                                                            androidx.compose.material3.Button(
                                                                onClick = {
                                                                    jellyseerrApiKey = apiKeyInput
                                                                    settings.jellyseerrApiKey = apiKeyInput
                                                                    showJellyseerrApiKeyDialog = false
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                androidx.compose.material3.Text("Guardar")
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (isTv) {
                                                Surface(
                                                    modifier = Modifier.width(500.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = SurfaceDefaults.colors(
                                                        containerColor = MaterialTheme.colorScheme.surface,
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    content = { content() }
                                                )
                                            } else {
                                                androidx.compose.material3.Surface(
                                                    modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                                    content = { content() }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Username/Password authentication
                                SettingButton(
                                    title = "Acceso a Jellyseerr",
                                    description = if (jellyseerrSessionCookie.isNotBlank() && jellyseerrUsername.isNotBlank()) 
                                        "Sesión iniciada como ${jellyseerrUsername} ✓" 
                                    else 
                                        "Inicia sesión con tu cuenta de Jellyseerr o Jellyfin",
                                    buttonText = if (jellyseerrSessionCookie.isNotBlank()) "Volver a iniciar sesión" else "Iniciar sesión",
                                    onClick = { 
                                        showJellyseerrLoginDialog = true
                                        loginError = null
                                    }
                                )
                                
                                // Logout button (only show if logged in)
                                if (jellyseerrSessionCookie.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SettingButton(
                                        title = "Cerrar sesión",
                                        description = "Borra las credenciales guardadas.",
                                        buttonText = "Cerrar sesión",
                                        onClick = {
                                            settings.clearJellyseerrCredentials()
                                            jellyseerrSessionCookie = ""
                                            jellyseerrUsername = ""
                                            jellyseerrApiKey = ""
                                            jellyseerrAuthType = "api_key"
                                        }
                                    )
                                }
                                
                                if (showJellyseerrLoginDialog) {
                                    var usernameInput by remember { mutableStateOf("") }
                                    var passwordInput by remember { mutableStateOf("") }
                                    var useJellyfinAuth by remember { mutableStateOf(true) }
                                    
                                    Dialog(
                                        onDismissRequest = { 
                                            if (!isLoggingIn) {
                                                showJellyseerrLoginDialog = false 
                                            }
                                        },
                                        properties = DialogProperties(usePlatformDefaultWidth = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.7f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val content = @Composable {
                                                Column(
                                                    modifier = Modifier.padding(if (isTv) 32.dp else 24.dp),
                                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                                ) {
                                                    Text(
                                                        text = "Acceso a Jellyseerr",
                                                        style = if (isTv) MaterialTheme.typography.headlineSmall else androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                                        color = if (isTv) MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                                    )
                                                    
                                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                        Text(
                                                            text = "Inicia sesión con tus credenciales.",
                                                            style = if (isTv) MaterialTheme.typography.bodyMedium else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                            color = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                        )
                                                        
                                                        // Auth type selector
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            if (isTv) {
                                                                Button(
                                                                    onClick = { useJellyfinAuth = true },
                                                                    modifier = Modifier.weight(1f),
                                                                    colors = ButtonDefaults.colors(
                                                                        containerColor = if (useJellyfinAuth) 
                                                                            MaterialTheme.colorScheme.primary 
                                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                    )
                                                                ) {
                                                                    Text("Jellyfin")
                                                                }
                                                                Button(
                                                                    onClick = { useJellyfinAuth = false },
                                                                    modifier = Modifier.weight(1f),
                                                                    colors = ButtonDefaults.colors(
                                                                        containerColor = if (!useJellyfinAuth) 
                                                                            MaterialTheme.colorScheme.primary 
                                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                    )
                                                                ) {
                                                                    Text("Local/Correo")
                                                                }
                                                            } else {
                                                                androidx.compose.material3.Button(
                                                                    onClick = { useJellyfinAuth = true },
                                                                    modifier = Modifier.weight(1f),
                                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                        containerColor = if (useJellyfinAuth) 
                                                                            androidx.compose.material3.MaterialTheme.colorScheme.primary 
                                                                        else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                                                    )
                                                                ) {
                                                                    androidx.compose.material3.Text("Jellyfin", color = if (useJellyfinAuth) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                                androidx.compose.material3.Button(
                                                                    onClick = { useJellyfinAuth = false },
                                                                    modifier = Modifier.weight(1f),
                                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                        containerColor = if (!useJellyfinAuth) 
                                                                            androidx.compose.material3.MaterialTheme.colorScheme.primary 
                                                                        else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                                                    )
                                                                ) {
                                                                    androidx.compose.material3.Text("Local/Correo", color = if (!useJellyfinAuth) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                            }
                                                        }
                                                        
                                                        OutlinedTextField(
                                                             value = usernameInput,
                                                             onValueChange = { usernameInput = it },
                                                             label = { Text(if (useJellyfinAuth) "Usuario de Jellyfin" else "Correo electrónico") },
                                                             singleLine = true,
                                                             enabled = !isLoggingIn,
                                                             modifier = Modifier.fillMaxWidth(),
                                                             colors = TextFieldDefaults.colors(
                                                                 focusedTextColor = Color.White,
                                                                 unfocusedTextColor = Color.White,
                                                                 focusedContainerColor = Color.Transparent,
                                                                 unfocusedContainerColor = Color.Transparent,
                                                                 cursorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 focusedLabelColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                                                 focusedIndicatorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                                                             )
                                                         )
                                                         
                                                         OutlinedTextField(
                                                             value = passwordInput,
                                                             onValueChange = { passwordInput = it },
                                                             label = { Text("Contraseña") },
                                                             singleLine = true,
                                                             enabled = !isLoggingIn,
                                                             visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                             modifier = Modifier.fillMaxWidth(),
                                                             colors = TextFieldDefaults.colors(
                                                                 focusedTextColor = Color.White,
                                                                 unfocusedTextColor = Color.White,
                                                                 focusedContainerColor = Color.Transparent,
                                                                 unfocusedContainerColor = Color.Transparent,
                                                                 cursorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 focusedLabelColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                                                 focusedIndicatorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                                 unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                                                             )
                                                         )
                                                        
                                                        if (isLoggingIn) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.Center,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(20.dp),
                                                                    strokeWidth = 2.dp
                                                                )
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Text("Iniciando sesión…", style = if (isTv) MaterialTheme.typography.bodyMedium else androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                                            }
                                                        }
                                                        
                                                        loginError?.let { error ->
                                                            Text(
                                                                text = error,
                                                                color = if (isTv) MaterialTheme.colorScheme.error else androidx.compose.material3.MaterialTheme.colorScheme.error,
                                                                style = if (isTv) MaterialTheme.typography.bodySmall else androidx.compose.material3.MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        if (isTv) {
                                                            Button(
                                                                onClick = { showJellyseerrLoginDialog = false },
                                                                enabled = !isLoggingIn,
                                                                modifier = Modifier.weight(1f),
                                                                colors = ButtonDefaults.colors(
                                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                )
                                                            ) {
                                                                Text("Cancelar")
                                                            }
                                                            
                                                            Button(
                                                                onClick = {
                                                                    if (jellyseerrUrl.isBlank()) {
                                                                        loginError = "Define primero la URL de Jellyseerr"
                                                                        return@Button
                                                                    }
                                                                    if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                                                        loginError = "Please enter username and password"
                                                                        return@Button
                                                                    }
                                                                    
                                                                    isLoggingIn = true
                                                                    loginError = null
                                                                    
                                                                    scope.launch {
                                                                        val result = if (useJellyfinAuth) {
                                                                            com.klortek.velora.jellyseerr.JellyseerrApiService.loginWithJellyfin(
                                                                                jellyseerrUrl,
                                                                                usernameInput,
                                                                                passwordInput
                                                                            )
                                                                        } else {
                                                                            com.klortek.velora.jellyseerr.JellyseerrApiService.loginWithEmail(
                                                                                jellyseerrUrl,
                                                                                usernameInput,
                                                                                passwordInput
                                                                            )
                                                                        }
                                                                        
                                                                        isLoggingIn = false
                                                                        
                                                                        result.fold(
                                                                            onSuccess = { cookie ->
                                                                                jellyseerrSessionCookie = cookie
                                                                                jellyseerrUsername = usernameInput
                                                                                settings.jellyseerrSessionCookie = cookie
                                                                                settings.jellyseerrUsername = usernameInput
                                                                                settings.jellyseerrAuthType = "credentials"
                                                                                showJellyseerrLoginDialog = false
                                                                                Toast.makeText(context, "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
                                                                            },
                                                                            onFailure = { error ->
                                                                                loginError = "Error al iniciar sesión: ${error.message}"
                                                                            }
                                                                        )
                                                                    }
                                                                },
                                                                enabled = !isLoggingIn,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text("Iniciar sesión")
                                                            }
                                                        } else {
                                                            androidx.compose.material3.Button(
                                                                onClick = { showJellyseerrLoginDialog = false },
                                                                enabled = !isLoggingIn,
                                                                modifier = Modifier.weight(1f),
                                                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                            ) {
                                                                androidx.compose.material3.Text("Cancelar", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            
                                                            androidx.compose.material3.Button(
                                                                onClick = {
                                                                    if (jellyseerrUrl.isBlank()) {
                                                                        loginError = "Define primero la URL de Jellyseerr"
                                                                        return@Button
                                                                    }
                                                                    if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                                                        loginError = "Please enter username and password"
                                                                        return@Button
                                                                    }
                                                                    
                                                                    isLoggingIn = true
                                                                    loginError = null
                                                                    
                                                                    scope.launch {
                                                                        val result = if (useJellyfinAuth) {
                                                                            com.klortek.velora.jellyseerr.JellyseerrApiService.loginWithJellyfin(
                                                                                jellyseerrUrl,
                                                                                usernameInput,
                                                                                passwordInput
                                                                            )
                                                                        } else {
                                                                            com.klortek.velora.jellyseerr.JellyseerrApiService.loginWithEmail(
                                                                                jellyseerrUrl,
                                                                                usernameInput,
                                                                                passwordInput
                                                                            )
                                                                        }
                                                                        
                                                                        isLoggingIn = false
                                                                        
                                                                        result.fold(
                                                                            onSuccess = { cookie ->
                                                                                jellyseerrSessionCookie = cookie
                                                                                jellyseerrUsername = usernameInput
                                                                                settings.jellyseerrSessionCookie = cookie
                                                                                settings.jellyseerrUsername = usernameInput
                                                                                settings.jellyseerrAuthType = "credentials"
                                                                                showJellyseerrLoginDialog = false
                                                                                Toast.makeText(context, "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
                                                                            },
                                                                            onFailure = { error ->
                                                                                loginError = "Error al iniciar sesión: ${error.message}"
                                                                            }
                                                                        )
                                                                    }
                                                                },
                                                                enabled = !isLoggingIn,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                androidx.compose.material3.Text("Iniciar sesión")
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (isTv) {
                                                Surface(
                                                    modifier = Modifier.width(500.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = SurfaceDefaults.colors(
                                                        containerColor = MaterialTheme.colorScheme.surface,
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    content = { content() }
                                                )
                                            } else {
                                                androidx.compose.material3.Surface(
                                                    modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                                    content = { content() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Enable Jellyseerr Discover Tab toggle (only show if configured)
                            val isJellyseerrConfigured = jellyseerrUrl.isNotBlank() && (
                                (jellyseerrAuthType == "api_key" && jellyseerrApiKey.isNotBlank()) ||
                                (jellyseerrAuthType == "credentials" && jellyseerrSessionCookie.isNotBlank())
                            )
                            
                            if (isJellyseerrConfigured) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                SettingToggle(
                                    title = "Activar pestaña Descubrir",
                                    description = "Muestra contenido de tendencias, popular y próximos estrenos de Jellyseerr.",
                                    isEnabled = jellyseerrEnabled,
                                    onToggle = {
                                        jellyseerrEnabled = !jellyseerrEnabled
                                        settings.jellyseerrEnabled = jellyseerrEnabled
                                    }
                                )
                                
                                SettingToggle(
                                    title = "Incluir en búsquedas",
                                    description = "Muestra resultados de Jellyseerr en la pantalla principal de búsqueda.",
                                    isEnabled = jellyseerrSearchEnabled,
                                    onToggle = {
                                        jellyseerrSearchEnabled = !jellyseerrSearchEnabled
                                        settings.jellyseerrSearchEnabled = jellyseerrSearchEnabled
                                    }
                                )
                            }
                        }

                        SettingsCategory.TRAILERS -> {
                            val tmdbApiKey = settings.tmdbApiKey
                            var showTmdbKeyDialog by remember { mutableStateOf(false) }
                            
                            SettingButton(
                                title = "Clave API de TMDB (tráilers)",
                                description = if (tmdbApiKey.isNotBlank()) 
                                    "Clave de TMDB configurada ✓" 
                                else 
                                    "Necesaria para obtener tráilers directamente de The Movie Database",
                                buttonText = if (tmdbApiKey.isNotBlank()) "Cambiar" else "Definir clave",
                                onClick = { showTmdbKeyDialog = true }
                            )
                            
                            if (showTmdbKeyDialog) {
                                var apiKeyInput by remember { mutableStateOf(tmdbApiKey) }
                                var isVerifying by remember { mutableStateOf(false) }
                                var verificationError by remember { mutableStateOf<String?>(null) }
                                val scope = rememberCoroutineScope()
                                val context = LocalContext.current
                                Dialog(
                                    onDismissRequest = { showTmdbKeyDialog = false },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val content = @Composable {
                                            Column(
                                                modifier = Modifier.padding(if (isTv) 32.dp else 24.dp),
                                                verticalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                if (isTv) {
                                                    Text(
                                                        text = "Clave API de TMDB",
                                                        style = MaterialTheme.typography.headlineSmall
                                                    )
                                                } else {
                                                    androidx.compose.material3.Text(
                                                        text = "Clave API de TMDB",
                                                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                                                    )
                                                }
                                                
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (isTv) {
                                                        Text(
                                                            text = "Introduce tu clave API de TMDB para obtener tráilers directamente de The Movie Database.",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                        )
                                                    } else {
                                                        androidx.compose.material3.Text(
                                                            text = "Introduce tu clave API de TMDB para obtener tráilers directamente de The Movie Database.",
                                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                    
                                                    OutlinedTextField(
                                                         value = apiKeyInput,
                                                         onValueChange = { 
                                                             apiKeyInput = it
                                                             verificationError = null 
                                                         },
                                                         label = { Text("Clave API de TMDB") },
                                                         singleLine = true,
                                                         modifier = Modifier.fillMaxWidth(),
                                                         isError = verificationError != null,
                                                         supportingText = {
                                                             if (verificationError != null) {
                                                                 Text(
                                                                     text = verificationError!!,
                                                                     color = MaterialTheme.colorScheme.error
                                                                 )
                                                             }
                                                         },
                                                         colors = TextFieldDefaults.colors(
                                                             focusedTextColor = Color.White,
                                                             unfocusedTextColor = Color.White,
                                                             focusedContainerColor = Color.Transparent,
                                                             unfocusedContainerColor = Color.Transparent,
                                                             cursorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                             focusedLabelColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                             unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                                             focusedIndicatorColor = if (isTv) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                             unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f),
                                                             errorLabelColor = if (isTv) MaterialTheme.colorScheme.error else androidx.compose.material3.MaterialTheme.colorScheme.error,
                                                             errorIndicatorColor = if (isTv) MaterialTheme.colorScheme.error else androidx.compose.material3.MaterialTheme.colorScheme.error,
                                                             errorSupportingTextColor = if (isTv) MaterialTheme.colorScheme.error else androidx.compose.material3.MaterialTheme.colorScheme.error
                                                         )
                                                     )
                                                }
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    if (isTv) {
                                                        Button(
                                                            onClick = { showTmdbKeyDialog = false },
                                                            modifier = Modifier.weight(1f),
                                                            enabled = !isVerifying,
                                                            colors = ButtonDefaults.colors(
                                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                        ) {
                                                            Text("Cancelar")
                                                        }
                                                    } else {
                                                        androidx.compose.material3.Button(
                                                            onClick = { showTmdbKeyDialog = false },
                                                            modifier = Modifier.weight(1f),
                                                            enabled = !isVerifying,
                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        ) {
                                                            androidx.compose.material3.Text(
                                                                text = "Cancelar",
                                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    
                                                    if (isTv) {
                                                        Button(
                                                            onClick = {
                                                                isVerifying = true
                                                                verificationError = null
                                                                scope.launch {
                                                                    val result = com.klortek.velora.tmdb.TmdbApiService.verifyKey(apiKeyInput)
                                                                    isVerifying = false
                                                                    when (result) {
                                                                        is com.klortek.velora.tmdb.TmdbApiService.VerificationResult.Success -> {
                                                                            settings.tmdbApiKey = apiKeyInput.trim()
                                                                            showTmdbKeyDialog = false
                                                                            Toast.makeText(context, "Clave de TMDB verificada ✓", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                        is com.klortek.velora.tmdb.TmdbApiService.VerificationResult.Error -> {
                                                                            verificationError = result.message
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f),
                                                            enabled = !isVerifying
                                                        ) {
                                                            if (isVerifying) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(24.dp),
                                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                                    strokeWidth = 2.dp
                                                                )
                                                            } else {
                                                                Text("Guardar")
                                                            }
                                                        }
                                                    } else {
                                                        androidx.compose.material3.Button(
                                                            onClick = {
                                                                isVerifying = true
                                                                verificationError = null
                                                                scope.launch {
                                                                    val result = com.klortek.velora.tmdb.TmdbApiService.verifyKey(apiKeyInput)
                                                                    isVerifying = false
                                                                    when (result) {
                                                                        is com.klortek.velora.tmdb.TmdbApiService.VerificationResult.Success -> {
                                                                            settings.tmdbApiKey = apiKeyInput.trim()
                                                                            showTmdbKeyDialog = false
                                                                            Toast.makeText(context, "Clave de TMDB verificada ✓", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                        is com.klortek.velora.tmdb.TmdbApiService.VerificationResult.Error -> {
                                                                            verificationError = result.message
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f),
                                                            enabled = !isVerifying
                                                        ) {
                                                            if (isVerifying) {
                                                                androidx.compose.material3.CircularProgressIndicator(
                                                                    modifier = Modifier.size(24.dp),
                                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                                                                    strokeWidth = 2.dp
                                                                )
                                                            } else {
                                                                androidx.compose.material3.Text("Guardar")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (isTv) {
                                            Surface(
                                                modifier = Modifier.width(500.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = SurfaceDefaults.colors(
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    contentColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                content = { content() }
                                            )
                                        } else {
                                            androidx.compose.material3.Surface(
                                                modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                                content = { content() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        
                        SettingsCategory.APPEARANCE -> {
                            // Dark Mode
                            SettingToggle(
                                title = "Modo oscuro",
                                description = "Desactiva la imagen de fondo y usa el fondo oscuro de Material.",
                                isEnabled = darkModeEnabled,
                                onToggle = {
                                    darkModeEnabled = !darkModeEnabled
                                    settings.darkModeEnabled = darkModeEnabled
                                }
                            )
                            
                            // Use Logo for Title
                            SettingToggle(
                                title = "Usar logo como título",
                                description = "Muestra el logo en lugar del texto del título en las pantallas multimedia.",
                                isEnabled = useLogoForTitleEnabled,
                                onToggle = {
                                    useLogoForTitleEnabled = !useLogoForTitleEnabled
                                    settings.useLogoForTitle = useLogoForTitleEnabled
                                }
                            )
                            
                            // Animated Play Button
                            SettingToggle(
                                title = "Botón de reproducción animado",
                                description = "Usa un botón de reproducción animado con efecto de brillo Lottie.",
                                isEnabled = animatedPlayButtonEnabled,
                                onToggle = {
                                    animatedPlayButtonEnabled = !animatedPlayButtonEnabled
                                    settings.useAnimatedPlayButton = animatedPlayButtonEnabled
                                }
                            )
                            
                            // 24-Hour Time Format
                            SettingToggle(
                                title = "Formato horario de 24 horas",
                                description = "Muestra la hora en formato de 24 horas (HH:mm).",
                                isEnabled = use24HourTimeEnabled,
                                onToggle = {
                                    use24HourTimeEnabled = !use24HourTimeEnabled
                                    settings.use24HourTime = use24HourTimeEnabled
                                }
                            )

                            // 4K Quality Backgrounds
                            SettingToggle(
                                title = "Fondos en calidad 4K",
                                description = "Usa resolución 4K (3840x2160) para las imágenes de fondo. Puede afectar al rendimiento.",
                                isEnabled = use4KBackgrounds,
                                onToggle = {
                                    use4KBackgrounds = !use4KBackgrounds
                                    settings.use4KBackgrounds = use4KBackgrounds
                                }
                            )

                            // Navigation Sounds
                            SettingToggle(
                                title = "Sonidos de navegación",
                                description = "Activa los sonidos del sistema al navegar y pulsar en la aplicación.",
                                isEnabled = navigationSoundsEnabled,
                                onToggle = {
                                    navigationSoundsEnabled = !navigationSoundsEnabled
                                    settings.navigationSoundsEnabled = navigationSoundsEnabled
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            if (isTv) {
                                Text(
                                    text = "Color de acento del tema",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else {
                                Text(
                                    text = "Color de acento del tema",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            var selectedThemeColorHex by remember { mutableStateOf(settings.themeColorHex) }

                            val colorPresets = listOf(
                                Pair("Blanco", "#FFFFFF"),
                                Pair("Amarillo", "#ECC564"),
                                Pair("Azul", "#2196F3"),
                                Pair("Verde", "#4CAF50"),
                                Pair("Rojo", "#F44336"),
                                Pair("Morado", "#9C27B0"),
                                Pair("Naranja", "#FF9800")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorPresets.forEach { (name, hex) ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = selectedThemeColorHex.equals(hex, ignoreCase = true)
                                    if (isTv) {
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isFocused by interactionSource.collectIsFocusedAsState()
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(50.dp))
                                                .background(color)
                                                .border(
                                                    width = if (isSelected || isFocused) 3.dp else 1.dp,
                                                    color = if (isSelected) {
                                                        Color.White
                                                    } else if (isFocused) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        Color.White.copy(alpha = 0.3f)
                                                    },
                                                    shape = RoundedCornerShape(50.dp)
                                                )
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    selectedThemeColorHex = hex
                                                    settings.themeColorHex = hex
                                                    com.klortek.velora.theme.JetcasterPrimaryColorState = color
                                                }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(50.dp))
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(50.dp)
                                                )
                                                .clickable {
                                                    selectedThemeColorHex = hex
                                                    settings.themeColorHex = hex
                                                    com.klortek.velora.theme.JetcasterPrimaryColorState = color
                                                }
                                        )
                                    }
                                }
                            }
                        }
                        
                        SettingsCategory.PERFORMANCE -> {
                            // Use Google TV Cards
                            SettingToggle(
                                title = "Usar tarjetas de Google TV",
                                description = "Tarjetas ligeras con animación de escala sutil y borde luminoso.",
                                isEnabled = useGoogleTvCards,
                                onToggle = {
                                    useGoogleTvCards = !useGoogleTvCards
                                    settings.useGoogleTvCards = useGoogleTvCards
                                    // Disable simple cards if Google TV cards enabled
                                    if (useGoogleTvCards) {
                                        useSimpleCards = false
                                        settings.useSimpleCards = false
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Animaciones",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                            )
                            
                            // Disable UI Animations
                            SettingToggle(
                                title = "Desactivar animaciones de interfaz",
                                description = "Desactiva las animaciones de desplazamiento para mejorar el rendimiento.",
                                isEnabled = disableUIAnimations,
                                onToggle = {
                                    disableUIAnimations = !disableUIAnimations
                                    settings.disableUIAnimations = disableUIAnimations
                                }
                            )
                            
                            // Preload Library Images
                            SettingToggle(
                                title = "Precargar imágenes de biblioteca",
                                description = "Precarga imágenes para un desplazamiento más fluido.",
                                isEnabled = preloadLibraryImagesEnabled,
                                onToggle = {
                                    preloadLibraryImagesEnabled = !preloadLibraryImagesEnabled
                                    settings.preloadLibraryImages = preloadLibraryImagesEnabled
                                }
                            )
                            
                            // Cache Library Images
                            SettingToggle(
                                title = "Guardar imágenes de biblioteca en caché",
                                description = "Guarda imágenes en disco y memoria para acelerar la carga.",
                                isEnabled = cacheLibraryImagesEnabled,
                                onToggle = {
                                    cacheLibraryImagesEnabled = !cacheLibraryImagesEnabled
                                    settings.cacheLibraryImages = cacheLibraryImagesEnabled
                                }
                            )
                            
                            // Use Glide
                            SettingToggle(
                                title = "Usar Glide para imágenes",
                                description = "Usa Glide en lugar de Coil para cargar imágenes.",
                                isEnabled = useGlideEnabled,
                                onToggle = {
                                    useGlideEnabled = !useGlideEnabled
                                    settings.useGlide = useGlideEnabled
                                }
                            )
                            
                            // Reduce Poster Resolution
                            SettingToggle(
                                title = "Reducir resolución de carteles",
                                description = "Reduce los carteles a 600x300 px para ahorrar ancho de banda.",
                                isEnabled = reducePosterResolutionEnabled,
                                onToggle = {
                                    reducePosterResolutionEnabled = !reducePosterResolutionEnabled
                                    settings.reducePosterResolution = reducePosterResolutionEnabled
                                }
                            )
                        }
                        
                        SettingsCategory.LIBRARY -> {
                            // Auto-Refresh Media
                            SettingToggle(
                                title = "Actualizar contenido automáticamente",
                                description = "Busca contenido nuevo automáticamente (cada ${autoRefreshIntervalMinutes} min).",
                                isEnabled = autoRefreshEnabled,
                                onToggle = {
                                    autoRefreshEnabled = !autoRefreshEnabled
                                    settings.autoRefreshEnabled = autoRefreshEnabled
                                }
                            )
                            
                            // Refresh Interval
                            if (autoRefreshEnabled) {
                                SettingCycle(
                                    title = "Intervalo de actualización",
                                    description = "Frecuencia de búsqueda de contenido nuevo.",
                                    currentValue = "${autoRefreshIntervalMinutes}m",
                                    onCycle = {
                                        autoRefreshIntervalMinutes = when (autoRefreshIntervalMinutes) {
                                            2 -> 3
                                            3 -> 5
                                            5 -> 10
                                            10 -> 15
                                            15 -> 2
                                            else -> 5
                                        }
                                        settings.autoRefreshIntervalMinutes = autoRefreshIntervalMinutes
                                    }
                                )
                            }
                            
                            // Hide Shows with Zero Episodes
                            SettingToggle(
                                title = "Ocultar series vacías",
                                description = "Oculta de Inicio y Biblioteca las series sin episodios.",
                                isEnabled = hideShowsWithZeroEpisodesEnabled,
                                onToggle = {
                                    hideShowsWithZeroEpisodesEnabled = !hideShowsWithZeroEpisodesEnabled
                                    settings.hideShowsWithZeroEpisodes = hideShowsWithZeroEpisodesEnabled
                                }
                            )

                            // Row Card Count
                            SettingCycle(
                                title = "Número de tarjetas por fila",
                                description = "Número de elementos que se cargan y muestran por fila.",
                                currentValue = rowCardCount.toString(),
                                onCycle = {
                                    rowCardCount = when (rowCardCount) {
                                        25 -> 50
                                        50 -> 75
                                        75 -> 100
                                        100 -> 25
                                        else -> 25
                                    }
                                    settings.rowCardCount = rowCardCount
                                }
                            )

                            SettingToggle(
                                title = "Música de tema",
                                description = "Reproduce la música del contenido seleccionado cuando Velora la tenga disponible.",
                                isEnabled = themeMusicEnabled,
                                onToggle = {
                                    themeMusicEnabled = !themeMusicEnabled
                                    settings.themeMusicEnabled = themeMusicEnabled
                                }
                            )

                            // Offline media is intentionally a mobile/tablet
                            // feature. TV builds do not expose this control.
                            if (!isTv) {
                                val gigabyte = 1024L * 1024L * 1024L
                                val offlineLimitOptions = listOf(5L, 10L, 25L, 50L, 100L).map { it * gigabyte } + 0L
                                val offlineLimitLabel = if (offlineMaxStorageBytes == 0L) {
                                    context.getString(com.klortek.velora.R.string.settings_offline_storage_unlimited)
                                } else {
                                    "${offlineMaxStorageBytes / gigabyte} GB"
                                }
                                SettingCycle(
                                    title = context.getString(com.klortek.velora.R.string.settings_offline_storage_limit),
                                    description = context.getString(com.klortek.velora.R.string.settings_offline_storage_limit_description),
                                    currentValue = offlineLimitLabel,
                                    onCycle = {
                                        val currentIndex = offlineLimitOptions.indexOf(offlineMaxStorageBytes).coerceAtLeast(0)
                                        val next = offlineLimitOptions[(currentIndex + 1) % offlineLimitOptions.size]
                                        offlineMaxStorageBytes = next
                                        settings.offlineMaxStorageBytes = next
                                    }
                                )
                                SettingToggle(
                                    title = context.getString(com.klortek.velora.R.string.settings_offline_wifi_only),
                                    description = context.getString(com.klortek.velora.R.string.settings_offline_wifi_only_description),
                                    isEnabled = offlineWifiOnly,
                                    onToggle = {
                                        offlineWifiOnly = !offlineWifiOnly
                                        settings.offlineWifiOnly = offlineWifiOnly
                                    }
                                )
                                SettingToggle(
                                    title = context.getString(com.klortek.velora.R.string.settings_smart_downloads),
                                    description = context.getString(com.klortek.velora.R.string.settings_smart_downloads_description),
                                    isEnabled = smartDownloadsEnabled,
                                    onToggle = {
                                        smartDownloadsEnabled = !smartDownloadsEnabled
                                        settings.smartDownloadsEnabled = smartDownloadsEnabled
                                    }
                                )
                                if (smartDownloadsEnabled) {
                                    SettingToggle(
                                        title = context.getString(com.klortek.velora.R.string.settings_smart_remove_watched),
                                        description = context.getString(com.klortek.velora.R.string.settings_smart_remove_watched_description),
                                        isEnabled = smartDownloadsRemoveWatched,
                                        onToggle = {
                                            smartDownloadsRemoveWatched = !smartDownloadsRemoveWatched
                                            settings.smartDownloadsRemoveWatched = smartDownloadsRemoveWatched
                                        }
                                    )
                                    SettingCycle(
                                        title = context.getString(com.klortek.velora.R.string.settings_smart_keep_episodes),
                                        description = context.getString(com.klortek.velora.R.string.settings_smart_keep_episodes_description),
                                        currentValue = smartDownloadsKeepUnwatchedEpisodes.toString(),
                                        onCycle = {
                                            val options = listOf(0, 1, 2, 3, 5, 10)
                                            val next = options[(options.indexOf(smartDownloadsKeepUnwatchedEpisodes).coerceAtLeast(0) + 1) % options.size]
                                            smartDownloadsKeepUnwatchedEpisodes = next
                                            settings.smartDownloadsKeepUnwatchedEpisodes = next
                                        }
                                    )
                                }
                            }
                        }
                        
                        SettingsCategory.ADVANCED -> {
                            // Debug Outlines
                            SettingToggle(
                                title = "Mostrar contornos de depuración",
                                description = "Muestra bordes de depuración para visualizar el diseño.",
                                isEnabled = debugOutlinesEnabled,
                                onToggle = {
                                    debugOutlinesEnabled = !debugOutlinesEnabled
                                    settings.showDebugOutlines = debugOutlinesEnabled
                                }
                            )
                            
                            // Long Press Duration
                            SettingCycle(
                                title = "Duración de pulsación larga",
                                description = "Tiempo que hay que mantener Enter/OK para abrir el menú del episodio.",
                                currentValue = "${longPressDurationSeconds}s",
                                onCycle = {
                                    longPressDurationSeconds = when (longPressDurationSeconds) {
                                        2 -> 3
                                        3 -> 4
                                        4 -> 5
                                        5 -> 2
                                        else -> 2
                                    }
                                    settings.longPressDurationSeconds = longPressDurationSeconds
                                }
                            )
                            
                            // Clear Image Cache
                            SettingButton(
                                title = "Borrar caché de imágenes",
                                description = "Borra todas las imágenes guardadas en disco y memoria.",
                                buttonText = "Borrar",
                                onClick = {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val imageLoader: coil.ImageLoader = context.imageLoader
                                                imageLoader.diskCache?.clear()
                                                imageLoader.memoryCache?.clear()
                                                
                                                val coilCacheDir = context.filesDir.resolve("image_cache")
                                                if (coilCacheDir.exists()) {
                                                    coilCacheDir.deleteRecursively()
                                                }
                                                
                                                val glideCacheDir = File(context.cacheDir, "glide_image_cache")
                                                if (glideCacheDir.exists()) {
                                                    glideCacheDir.deleteRecursively()
                                                }
                                                
                                                Glide.get(context).clearDiskCache()
                                            }
                                            
                                            withContext(Dispatchers.Main) {
                                                Glide.get(context).clearMemory()
                                                Toast.makeText(context, "Caché borrada correctamente", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("SettingsScreen", "Error clearing cache", e)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(com.klortek.velora.R.string.error_fragment_message), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        
                        SettingsCategory.UPDATES -> {
                            // Auto-Check for Updates
                            SettingToggle(
                                title = "Buscar actualizaciones automáticamente",
                                description = "Busca actualizaciones automáticamente al iniciar la aplicación.",
                                isEnabled = autoUpdateEnabled,
                                onToggle = {
                                    autoUpdateEnabled = !autoUpdateEnabled
                                    settings.autoUpdateEnabled = autoUpdateEnabled
                                }
                            )
                            
                            // Check for Updates
                            SettingButton(
                                title = "Buscar actualizaciones",
                                description = if (checkingForUpdates) {
                                    "Comprobando actualizaciones…"
                                } else if (updateCheckMessage != null) {
                                    updateCheckMessage!!
                                } else {
                                    "Comprueba manualmente las actualizaciones de la aplicación en GitHub"
                                },
                                buttonText = if (checkingForUpdates) "Comprobando…" else "Comprobar",
                                enabled = !checkingForUpdates,
                                onClick = {
                                    scope.launch {
                                        checkingForUpdates = true
                                        updateCheckMessage = null
                                        
                                        try {
                                            val versionCode = try {
                                                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                                    packageInfo.longVersionCode.toInt()
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    packageInfo.versionCode
                                                }
                                            } catch (e: Exception) { 1 }
                                            val versionName = runCatching {
                                                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                                            }.getOrNull()
                                            
                                            val release = withContext(Dispatchers.IO) {
                                                UpdateService.getLatestRelease()
                                            }
                                            
                                            if (release != null) {
                                                val remoteVersionCode = UpdateService.parseVersion(release.tagName)
                                                if (UpdateService.updateAvailable(remoteVersionCode, versionCode, versionName)) {
                                                    latestRelease = release
                                                    showUpdateDialog = true
                                                    updateCheckMessage = "Update available: ${release.name}"
                                                } else {
                                                    updateCheckMessage = "You're on the latest version (${release.name})"
                                                }
                                            } else {
                                                updateCheckMessage = "Failed to check for updates. Please try again later."
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("SettingsScreen", "Error checking for updates", e)
                                            updateCheckMessage = "Error checking for updates: ${e::class.simpleName}"
                                        } finally {
                                            checkingForUpdates = false
                                        }
                                    }
                                }
                            )
                        }
                        
                        SettingsCategory.ABOUT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Velora",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Versión ${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "By Klørtek",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }

                        SettingsCategory.ACCOUNT -> {
                            // Log Out
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cerrar sesión",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Cierra la sesión y vuelve a la pantalla de acceso.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                
                                Button(
                                    onClick = { showLogoutConfirmation = true },
                                    colors = ButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Cerrar sesión")
                                }
                            }
                        }

        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isTv) MaterialTheme.colorScheme.surface else MobileSettingsBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Responsive Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isTv) 48.dp else 16.dp,
                        vertical = if (isTv) 24.dp else 16.dp
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTv) {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }

                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                } else {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (activeCategoryDetail != null) {
                                activeCategoryDetail = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(com.klortek.velora.R.string.action_back),
                            tint = MobileSettingsText
                        )
                    }

                    androidx.compose.material3.Text(
                        text = if (activeCategoryDetail != null) activeCategoryDetail!!.localizedTitle(context)
                        else context.getString(com.klortek.velora.R.string.nav_settings),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MobileSettingsText,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (!isTv && activeCategoryDetail == null) {
                // Mobile category list
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsCategory.entries
                        .filterNot { it == SettingsCategory.JELLYSEERR || it == SettingsCategory.TRAILERS }
                        .forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MobileSettingsSurface)
                                .border(
                                    width = 1.dp,
                                    color = MobileSettingsDivider,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { activeCategoryDetail = category }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = MobileSettingsAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            androidx.compose.material3.Text(
                                text = category.localizedTitle(context),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                color = MobileSettingsText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                val categoryToRender = if (isTv) selectedCategory else activeCategoryDetail!!

                if (isTv) {
                    // TV content
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Left column: Categories
                        Column(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                                .padding(end = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SettingsCategory.entries
                                .filterNot { it == SettingsCategory.JELLYSEERR || it == SettingsCategory.TRAILERS }
                                .forEach { category ->
                                CategoryItem(
                                    category = category,
                                    isSelected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    onFocused = { selectedCategory = category }
                                )
                            }
                        }
                        
                        // Vertical divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                        
                        // Right panel: Settings for selected category
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 32.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Category title
                            Text(
                                text = selectedCategory.localizedTitle(context),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            SettingsOptions(categoryToRender)
                        }
                    }
                } else {
                    // Mobile content panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SettingsOptions(categoryToRender)
                        
                        // Add some bottom padding
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
        
        // Dialogs
        // Logout confirmation dialog
        if (showLogoutConfirmation) {
            Dialog(
                onDismissRequest = { showLogoutConfirmation = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    val content = @Composable {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isTv) 32.dp else 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                text = "¿Cerrar sesión?",
                                style = if (isTv) MaterialTheme.typography.headlineSmall else androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                color = if (isTv) MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "¿Seguro que quieres cerrar sesión? Tendrás que volver a iniciar sesión para acceder a tus contenidos.",
                                style = if (isTv) MaterialTheme.typography.bodyMedium else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (isTv) {
                                    Button(
                                        onClick = { showLogoutConfirmation = false },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text("Cancelar")
                                    }
                                    
                                    Button(
                                        onClick = {
                                            showLogoutConfirmation = false
                                            val config = JellyfinConfig(context)
                                            config.clearAuth()
                                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            context.startActivity(intent)
                                            (context as? android.app.Activity)?.finish()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Cerrar sesión")
                                    }
                                } else {
                                    androidx.compose.material3.Button(
                                        onClick = { showLogoutConfirmation = false },
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        androidx.compose.material3.Text("Cancelar", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            showLogoutConfirmation = false
                                            val config = JellyfinConfig(context)
                                            config.clearAuth()
                                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            context.startActivity(intent)
                                            (context as? android.app.Activity)?.finish()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        androidx.compose.material3.Text("Cerrar sesión", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    if (isTv) {
                        Surface(
                            modifier = Modifier
                                .width(500.dp)
                                .heightIn(max = 300.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = SurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            content = { content() }
                        )
                    } else {
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            content = { content() }
                        )
                    }
                }
            }
        }

        // Show update dialog if update is found
        latestRelease?.let { release ->
            if (showUpdateDialog) {
                UpdateDialog(
                    release = release,
                    onDismiss = {
                        showUpdateDialog = false
                        latestRelease = null
                    },
                    onUpdate = {
                        showUpdateDialog = false
                        latestRelease = null
                    }
                )
            }
        }

        // ExoPlayer subtitle text color picker dialog
        if (showExoSubtitleColorDialog) {
            SubtitleColorPickerDialog(
                title = "Color del texto de subtítulos",
                currentColor = settings.exoSubtitleTextColor,
                onColorSelected = { color ->
                    settings.exoSubtitleTextColor = color
                    showExoSubtitleColorDialog = false
                },
                onDismiss = { showExoSubtitleColorDialog = false }
            )
        }

        // ExoPlayer subtitle background color picker dialog
        if (showExoSubtitleBgColorDialog) {
            SubtitleColorPickerDialog(
                title = "Color del fondo de subtítulos",
                currentColor = settings.exoSubtitleBgColor,
                onColorSelected = { color ->
                    settings.exoSubtitleBgColor = color
                    showExoSubtitleBgColorDialog = false
                },
                onDismiss = { showExoSubtitleBgColorDialog = false }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isFocused -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (it.isFocused) {
                    onFocused()
                }
            },
        colors = ButtonDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp) // Reduced button padding by 20%
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp), // Decreased by 20% (5.1 * 0.8 ≈ 4)
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp) // Reduced icon size by ~17% (24 * 0.83 ≈ 20)
            )
            Text(
                text = category.localizedTitle(context),
                style = MaterialTheme.typography.bodyMedium, // Smaller text style
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    enabledText: String = "ON",
    disabledText: String = "OFF"
) {
    val context = LocalContext.current
    val isTv = remember(context) { com.klortek.velora.ui.DeviceUtils.isTvDevice(context) }
    val titleColor = if (isTv) MaterialTheme.colorScheme.onSurface else MobileSettingsText
    val descriptionColor = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MobileSettingsSecondaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(!isTv) { onToggle() }
            .padding(vertical = if (isTv) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isTv) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            } else {
                androidx.compose.material3.Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = titleColor)
                androidx.compose.material3.Text(text = description, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            }
        }
        
        if (isTv) {
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.colors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(if (isEnabled) enabledText else disabledText)
            }
        } else {
            androidx.compose.material3.Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    description: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean
) {
    val context = LocalContext.current
    val isTv = remember(context) { com.klortek.velora.ui.DeviceUtils.isTvDevice(context) }
    val titleColor = if (isTv) MaterialTheme.colorScheme.onSurface else MobileSettingsText
    val descriptionColor = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MobileSettingsSecondaryText

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isTv) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            } else {
                androidx.compose.material3.Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = titleColor)
                androidx.compose.material3.Text(text = description, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isTv) {
                Button(onClick = onDecrease, enabled = canDecrease) {
                    Text("-")
                }
                Button(onClick = onIncrease, enabled = canIncrease) {
                    Text("+")
                }
            } else {
                androidx.compose.material3.IconButton(
                    onClick = onDecrease,
                    enabled = canDecrease
                ) {
                    androidx.compose.material3.Text(
                        text = "-",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canDecrease) androidx.compose.material3.MaterialTheme.colorScheme.primary 
                                else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                androidx.compose.material3.IconButton(
                    onClick = onIncrease,
                    enabled = canIncrease
                ) {
                    androidx.compose.material3.Text(
                        text = "+",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canIncrease) androidx.compose.material3.MaterialTheme.colorScheme.primary 
                                else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCycle(
    title: String,
    description: String,
    currentValue: String,
    onCycle: () -> Unit
) {
    val context = LocalContext.current
    val isTv = remember(context) { com.klortek.velora.ui.DeviceUtils.isTvDevice(context) }
    val titleColor = if (isTv) MaterialTheme.colorScheme.onSurface else MobileSettingsText
    val descriptionColor = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MobileSettingsSecondaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(!isTv) { onCycle() }
            .padding(vertical = if (isTv) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isTv) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            } else {
                androidx.compose.material3.Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = titleColor)
                androidx.compose.material3.Text(text = description, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            }
        }
        
        if (isTv) {
            Button(
                onClick = onCycle,
                colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(currentValue)
            }
        } else {
            androidx.compose.material3.TextButton(onClick = onCycle) {
                androidx.compose.material3.Text(
                    text = currentValue,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingButton(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val isTv = remember(context) { com.klortek.velora.ui.DeviceUtils.isTvDevice(context) }
    val titleColor = if (isTv) MaterialTheme.colorScheme.onSurface else MobileSettingsText
    val descriptionColor = if (isTv) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MobileSettingsSecondaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(!isTv && enabled) { onClick() }
            .padding(vertical = if (isTv) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isTv) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            } else {
                androidx.compose.material3.Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = titleColor)
                androidx.compose.material3.Text(text = description, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = descriptionColor, modifier = Modifier.padding(top = 4.dp))
            }
        }
        
        if (isTv) {
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(buttonText)
            }
        } else {
            androidx.compose.material3.Button(
                onClick = onClick,
                enabled = enabled,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            ) {
                androidx.compose.material3.Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubtitleColorPickerDialog(
    title: String,
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isTv = remember(context) { com.klortek.velora.ui.DeviceUtils.isTvDevice(context) }

    val colorOptions = listOf(
        "White" to 0xFFFFFFFF.toInt(),
        "Black" to 0xFF000000.toInt(),
        "Yellow" to 0xFFFFFF00.toInt(),
        "Cyan" to 0xFF00FFFF.toInt(),
        "Green" to 0xFF00FF00.toInt(),
        "Red" to 0xFFFF0000.toInt(),
        "Blue" to 0xFF0000FF.toInt(),
        "Magenta" to 0xFFFF00FF.toInt()
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            if (isTv) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(16.dp),
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.7f
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val listItemColors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            focusedContentColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            selectedContentColor = Color.White
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(colorOptions) { (name, color) ->
                                val isSelected = color == currentColor
                                ListItem(
                                    selected = isSelected,
                                    onClick = { onColorSelected(color) },
                                    headlineContent = {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.9f
                                            )
                                        )
                                    },
                                    trailingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color(color), RoundedCornerShape(4.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        )
                                    },
                                    colors = listItemColors,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            } else {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight(0.6f),
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = title,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(colorOptions) { (name, color) ->
                                val isSelected = color == currentColor
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .clickable { onColorSelected(color) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    androidx.compose.material3.Text(
                                        text = name,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary
                                                else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(color), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        androidx.compose.material3.Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Text("Cancelar")
                        }
                    }
                }
            }
        }
    }
}
