package com.klortek.velora.jellyfin

import java.util.Locale

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.Headers
import androidx.compose.runtime.Stable
import com.klortek.velora.BuildConfig
import com.klortek.velora.player.PlaybackQuality
import com.klortek.velora.security.SensitiveDataRedactor

// Chapter info for movies/episodes
@Stable
@Serializable
data class ChapterInfo(
    val StartPositionTicks: Long = 0,
    val Name: String? = null,
    val ImageTag: String? = null
) {
    // Convert ticks to milliseconds (1 tick = 100 nanoseconds = 0.0001 ms)
    val startMs: Long get() = StartPositionTicks / 10000
    
    // Format start time as HH:MM:SS or MM:SS
    fun formatStartTime(): String {
        val totalSeconds = startMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }
}

internal fun veloraClientDeviceName(tvBuild: Boolean): String =
    if (tvBuild) "Android TV" else "Android"

internal fun veloraClientDeviceId(rawDeviceId: String?): String =
    rawDeviceId?.takeIf { it.isNotBlank() } ?: "velora-android"

@Stable
@Serializable
data class JellyfinItem(
    val Id: String,
    val Name: String,
    val Overview: String? = null,
    val PremiereDate: String? = null, // ISO date string for premiere date
    val DateCreated: String? = null, // ISO date string for date created/added
    val DateScanned: String? = null, // ISO date string for date scanned into library
    val ProductionYear: Int? = null,
    val ImageTags: Map<String, String>? = null,
    val SeriesName: String? = null,
    val SeriesId: String? = null, // Parent series ID for episodes
    val Type: String? = null,
    val CollectionType: String? = null, // Library type: "movies", "tvshows", "music", etc.
    val UserData: UserData? = null,
    val MediaSources: List<MediaSource>? = null,
    val RunTimeTicks: Long? = null,
    val OfficialRating: String? = null,
    val CommunityRating: Float? = null,
    val Genres: List<String>? = null, // Genres is an array of strings in Jellyfin API
    val ProviderIds: Map<String, String>? = null,
    val CriticRating: Float? = null, // Critic rating if available
    val People: List<Person>? = null, // Cast and crew members
    val IndexNumber: Int? = null, // Episode number for episodes
    val ParentIndexNumber: Int? = null, // Season number for episodes
    val ChildCount: Int? = null, // Number of child items (e.g., seasons for Series)
    val RecursiveItemCount: Int? = null, // Total recursive item count (e.g., total episodes for Series)
    val NextEpisodeId: String? = null, // ID of the next episode for autoplay
    val Chapters: List<ChapterInfo>? = null // Chapter markers for the video
) {
    // Helper to get the last played date from either UserData or calculate from position
    fun getLastPlayedDateForSort(): String {
        return UserData?.LastPlayedDate ?: "1970-01-01T00:00:00.0000000Z"
    }

    // Format RunTimeTicks into a human-readable string like "1 hour 47 mins"
    val formattedRuntime: String? get() {
        val ticks = RunTimeTicks ?: return null
        val totalMinutes = ticks / 600_000_000L
        if (totalMinutes <= 0) return null
        
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        
        return buildString {
            if (hours > 0) {
                append("$hours ${if (hours == 1L) "hour" else "hours"}")
                if (mins > 0) append(" ")
            }
            if (mins > 0) {
                append("$mins ${if (mins == 1L) "min" else "mins"}")
            }
        }
    }
}

@Stable
@Serializable
data class Person(
    val Id: String? = null,
    val Name: String,
    val Type: String? = null, // "Actor", "Director", "Writer", etc.
    val Role: String? = null, // Character name played
    val PrimaryImageTag: String? = null // Image tag for person's photo
)

// Full person details from /Users/{userId}/Items/{personId}
@Stable
@Serializable
data class PersonDetails(
    val Id: String,
    val Name: String,
    val Overview: String? = null, // Biography
    val PremiereDate: String? = null, // Birth date (Jellyfin uses PremiereDate for persons)
    val EndDate: String? = null, // Death date
    val ProductionLocations: List<String>? = null, // Place of birth
    val ImageTags: Map<String, String>? = null,
    val BackdropImageTags: List<String>? = null,
    val Type: String? = null,
    // Alternative field names that Jellyfin might use
    val BirthDate: String? = null,
    val DeathDate: String? = null
) {
    // Helper to get birth date from either field
    val birthDateValue: String? get() = PremiereDate ?: BirthDate
    // Helper to get death date from either field  
    val deathDateValue: String? get() = EndDate ?: DeathDate
}

@Stable
@Serializable
data class JellyfinPlaybackInfo(
    val MediaSources: List<MediaSource>? = null
)

// Media Segments for Skip Intro / Skip Credits (Jellyfin 10.10+)
@Stable
@Serializable
data class MediaSegment(
    val Id: String? = null,
    val ItemId: String? = null,
    val Type: String? = null, // "Intro", "Outro", "Recap", "Preview", "Commercial"
    val StartTicks: Long? = null, // Start time in ticks (1 tick = 100 nanoseconds)
    val EndTicks: Long? = null // End time in ticks
) {
    // Convert ticks to milliseconds (1 tick = 100 nanoseconds = 0.0001 ms)
    val startMs: Long get() = (StartTicks ?: 0) / 10000
    val endMs: Long get() = (EndTicks ?: 0) / 10000
}

@Stable
@Serializable
data class MediaSegmentsResponse(
    val Items: List<MediaSegment>? = null
)

// Simplified skip markers for the video player
data class SkipMarkers(
    val introStartMs: Long? = null,
    val introEndMs: Long? = null,
    val creditsStartMs: Long? = null
)

@Stable
@Serializable
data class MediaSource(
    val Id: String? = null,
    /** Provider-facing name for a selectable source, when available. */
    val Name: String? = null,
    /** File size in bytes when Jellyfin exposes it for the selected source. */
    val Size: Long? = null,
    val LiveStreamId: String? = null,
    val Protocol: String? = null,
    val Path: String? = null,
    val Container: String? = null,
    val TranscodingUrl: String? = null,
    val SupportsDirectPlay: Boolean? = null,
    val SupportsDirectStream: Boolean? = null,
    val MediaStreams: List<MediaStream>? = null
)

@Stable
@Serializable
data class MediaStream(
    val Index: Int? = null,
    val Type: String? = null, // "Video", "Audio", "Subtitle"
    val Codec: String? = null,
    val Language: String? = null,
    val DisplayLanguage: String? = null, // Human-readable language name (e.g., "Turkish")
    val DisplayTitle: String? = null,
    val IsExternal: Boolean? = null,
    val SupportsExternalStream: Boolean? = null, // Whether this subtitle can be streamed via /Subtitles/{index}/Stream
    val DeliveryUrl: String? = null,
    val DeliveryMethod: String? = null, // "External", "Encode", "Embed", "Hls"
    val Path: String? = null, // File system path for external subtitles
    val IsDefault: Boolean? = null,
    val IsForced: Boolean? = null,
    val IsTextSubtitleStream: Boolean? = null, // True for text (SRT, VTT, ASS), false for bitmap (PGS, VOBSUB)
    val CodecTag: String? = null, // Codec tag for advanced format detection
    val IsHearingImpaired: Boolean? = null, // Closed captions / SDH subtitles
    val Title: String? = null, // Subtitle track title
    val Width: Int? = null, // Video/subtitle width
    val Height: Int? = null, // Video/subtitle height
    val ChannelLayout: String? = null, // Audio channel layout (e.g., "5.1", "7.1", "stereo")
    val Channels: Int? = null, // Numeric audio channel count when ChannelLayout is absent
    val BitDepth: Int? = null,
    val BitRate: Int? = null,
    val Profile: String? = null,
    val Level: Int? = null,
    val PixelFormat: String? = null,
    val ColorPrimaries: String? = null,
    val ColorRange: String? = null,
    val ColorSpace: String? = null,
    val ColorTransfer: String? = null,
    val VideoRange: String? = null,
    val VideoRangeType: String? = null,
    val Hdr10PlusPresentFlag: Boolean? = null,
    val DvProfile: Int? = null,
    val DvLevel: Int? = null,
    val DvVersionMajor: Int? = null,
    val DvVersionMinor: Int? = null,
    val RpuPresentFlag: Int? = null,
    val ElPresentFlag: Int? = null,
    val BlPresentFlag: Int? = null,
    val DvBlSignalCompatibilityId: Int? = null,
    val VideoDoViTitle: String? = null,
    val AverageFrameRate: Float? = null,
    val RealFrameRate: Float? = null,
    val ReferenceFrameRate: Float? = null
)

@Stable
@Serializable
data class UserData(
    val PlayedPercentage: Double? = null,
    @SerialName("PlaybackPositionTicks")
    val PositionTicks: Long? = null,
    val Played: Boolean? = null,
    val IsFavorite: Boolean? = null,
    val UnplayedItemCount: Int? = null, // Number of unwatched episodes for Series
    val LastPlayedDate: String? = null // ISO date string for when item was last played
)

@Serializable
data class QuickConnectInitiateResponse(
    val Secret: String,
    val Code: String
)

@Serializable
data class QuickConnectStateResponse(
    val Authenticated: Boolean,
    val Code: String? = null,
    val Secret: String? = null,
    val Authentication: QuickConnectAuthentication? = null
)

@Serializable
data class QuickConnectAuthentication(
    val AccessToken: String,
    val User: QuickConnectUser
)

@Serializable
data class QuickConnectUser(
    val Id: String,
    val Name: String
)

@Serializable
data class QuickConnectAuthenticateRequest(
    val Secret: String
)

@Serializable
data class QuickConnectAuthenticationResponse(
    val AccessToken: String,
    val User: QuickConnectUser
)

@Serializable
data class ItemsResponse(
    val Items: List<JellyfinItem> = emptyList(),
    val TotalRecordCount: Int = 0
)

@Serializable
data class JellyfinLibrary(
    val Id: String,
    val Name: String,
    val Type: String? = null,
    val CollectionType: String? = null, // "movies", "tvshows", "music", "books", "photos", etc.
    val ImageTags: Map<String, String>? = null
)

@Serializable
data class RemoteSessionInfo(
    val Id: String? = null,
    val DeviceName: String? = null,
    val Client: String? = null,
    val UserName: String? = null,
    val PlayState: RemotePlayState? = null
)

@Serializable
data class RemotePlayState(
    val IsPaused: Boolean? = null,
    val PositionTicks: Long? = null
)

class JellyfinApiService(
    private val baseUrl: String,
    private val accessToken: String,
    private val userId: String,
    private val config: JellyfinConfig? = null,
    private val languageTag: String = java.util.Locale.getDefault().toLanguageTag()
) {
    private val clientDeviceName = veloraClientDeviceName(BuildConfig.TV_BUILD)
    private val clientDeviceId = veloraClientDeviceId(config?.deviceId)

    // Expose baseUrl, accessToken, userId for external use (e.g., MPV URL selector)
    val serverBaseUrl: String get() = baseUrl
    val apiKey: String get() = accessToken
    fun getUserId(): String = userId
    fun getJellyfinConfig(): JellyfinConfig? = config
    
    // In-memory cache for episodes (keyed by seasonId)
    private val episodeCache = mutableMapOf<String, Pair<Long, List<JellyfinItem>>>()
    private val seasonCache = mutableMapOf<String, Pair<Long, List<JellyfinItem>>>()
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes cache
    
    // Creating the Android engine performs class loading and dispatcher setup.
    // Defer that work until the first request so the TV home screen can render
    // before background metadata loading starts.
    private val client by lazy(LazyThreadSafetyMode.NONE) {
        HttpClient(Android) {
            defaultRequest {
                header(HttpHeaders.AcceptLanguage, languageTag)
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            engine {
                // Opening a Jellyfin Live TV stream can legitimately take longer
                // than a normal metadata request while the tuner/provider is
                // allocating the live stream. Keep this bounded, but do not abort
                // at 10–15 seconds and then hand MPV an unusable manifest URL.
                connectTimeout = 45_000
                socketTimeout = 45_000
            }
        }
    }

    suspend fun getControllableSessions(): List<RemoteSessionInfo> = runCatching {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        client.get(URLBuilder().takeFrom("${base}Sessions").apply {
            parameters.append("ControllableByUserId", userId)
        }.buildString()) {
            header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
        }.body<List<RemoteSessionInfo>>()
            .filter { !it.Id.isNullOrBlank() && !it.DeviceName.isNullOrBlank() }
    }.getOrDefault(emptyList())

    suspend fun playOnRemoteSession(sessionId: String, itemId: String, positionMs: Long = 0L): Boolean = runCatching {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        client.post("${base}Sessions/$sessionId/Playing") {
            header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            contentType(ContentType.Application.Json)
            setBody("{\"ItemIds\":[\"$itemId\"],\"PlayCommand\":\"PlayNow\",\"StartPositionTicks\":${positionMs * 10000}}")
        }
        true
    }.getOrDefault(false)

    suspend fun getContinueWatching(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items/Resume").apply {
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "ImageTags,UserData,SeriesName,SeriesId,Type")
                parameters.append("SortBy", "DatePlayed")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Sort client-side by LastPlayedDate (most recently played first)
            val sorted = response.Items.sortedByDescending { item ->
                item.getLastPlayedDateForSort()
            }
            sorted
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    suspend fun getNextUp(limit: Int = 50): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Shows/NextUp").apply {
                parameters.append("UserId", userId)
                parameters.append("Limit", limit.toString())
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "ImageTags,UserData,SeriesName,SeriesId,Type") // Request ImageTags to get Thumb images
                parameters.append("EnableResumable", "false") // Next Up shows episodes you haven't started yet
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    /**
     * Get the next up episode for a specific series
     * This is the episode the user should watch next based on their watch history
     * @param seriesId The series ID to get the next up episode for
     * @return The next up episode, or null if the series is fully watched or not started
     */
    suspend fun getNextUpForSeries(seriesId: String): JellyfinItem? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Shows/NextUp").apply {
                parameters.append("UserId", userId)
                parameters.append("SeriesId", seriesId)
                parameters.append("Limit", "1")
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "ImageTags,UserData,SeriesName,SeriesId,IndexNumber,ParentIndexNumber,Type")
                parameters.append("EnableResumable", "true") // Include in-progress episodes
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Fetching NextUp for series: $seriesId")
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            val nextUpEpisode = response.Items.firstOrNull()
            if (nextUpEpisode != null) {
                android.util.Log.d("JellyfinAPI", "NextUp episode found")
            } else {
                android.util.Log.d("JellyfinAPI", "No NextUp episode found for series $seriesId (series may be fully watched or not started)")
            }
            nextUpEpisode
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching NextUp for series $seriesId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }
    
    /**
     * Get the first episode of a series (Season 1 Episode 1)
     * Used as a fallback when no unwatched episodes are found
     * @param seriesId The series ID
     * @return The first episode (S1E1), or null if no episodes exist
     */
    suspend fun getFirstEpisode(seriesId: String): JellyfinItem? {
        return try {
            android.util.Log.d("JellyfinAPI", "Getting first episode (S1E1) for series: $seriesId")
            
            // Get all seasons for the series
            val seasons = getSeasons(seriesId)
            if (seasons.isEmpty()) {
                android.util.Log.d("JellyfinAPI", "No seasons found for series $seriesId")
                return null
            }
            
            // Prefer Season 1 over Specials (Season 0)
            val sortedSeasons = seasons.sortedBy { it.IndexNumber ?: 0 }
            val regularSeasons = sortedSeasons.filter { (it.IndexNumber ?: 0) > 0 }
            
            // Get the first regular season (Season 1), or fall back to Specials if no regular seasons
            val firstSeason = regularSeasons.firstOrNull() ?: sortedSeasons.firstOrNull()
            if (firstSeason == null) {
                android.util.Log.d("JellyfinAPI", "No first season found for series $seriesId")
                return null
            }
            
            // Get episodes from the first season
            val episodes = getEpisodes(seriesId, firstSeason.Id)
            
            // Return the first episode (sorted by IndexNumber)
            val firstEpisode = episodes.sortedBy { it.IndexNumber ?: 0 }.firstOrNull()
            if (firstEpisode != null) {
                android.util.Log.d("JellyfinAPI", "First episode found")
            } else {
                android.util.Log.d("JellyfinAPI", "No episodes found in first season for series $seriesId")
            }
            firstEpisode
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error getting first episode for series $seriesId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }
    
    /**
     * Get the first unwatched episode for a series
     * This searches through all seasons to find the first episode that hasn't been watched
     * Used as a fallback when NextUp API returns no results (e.g., user hasn't started the series)
     * @param seriesId The series ID
     * @return The first unwatched episode, or null if all episodes are watched
     */
    suspend fun getFirstUnwatchedEpisode(seriesId: String): JellyfinItem? {
        return try {
            android.util.Log.d("JellyfinAPI", "Finding first unwatched episode for series: $seriesId")
            
            // Get all seasons for the series
            val seasons = getSeasons(seriesId)
            if (seasons.isEmpty()) {
                android.util.Log.d("JellyfinAPI", "No seasons found for series $seriesId")
                return null
            }
            
            // Separate regular seasons (1+) from Specials (0)
            val sortedSeasons = seasons.sortedBy { it.IndexNumber ?: 0 }
            val regularSeasons = sortedSeasons.filter { (it.IndexNumber ?: 0) > 0 }
            val specialsSeason = sortedSeasons.find { (it.IndexNumber ?: 0) == 0 }
            
            // First, look for unwatched episodes in regular seasons (Season 1+)
            for (season in regularSeasons) {
                val episodes = getEpisodes(seriesId, season.Id)
                
                // Find the first unwatched episode in this season
                for (episode in episodes.sortedBy { it.IndexNumber ?: 0 }) {
                    val isWatched = episode.UserData?.Played == true
                    val isInProgress = (episode.UserData?.PlayedPercentage ?: 0.0) > 0 && 
                                       (episode.UserData?.PlayedPercentage ?: 0.0) < 90
                    
                    // Return this episode if it's unwatched or in-progress
                    if (!isWatched || isInProgress) {
                        android.util.Log.d("JellyfinAPI", "First unwatched episode found")
                        return episode
                    }
                }
            }
            
            // If all regular seasons are watched, check Specials (Season 0) as fallback
            if (specialsSeason != null) {
                val episodes = getEpisodes(seriesId, specialsSeason.Id)
                for (episode in episodes.sortedBy { it.IndexNumber ?: 0 }) {
                    val isWatched = episode.UserData?.Played == true
                    val isInProgress = (episode.UserData?.PlayedPercentage ?: 0.0) > 0 && 
                                       (episode.UserData?.PlayedPercentage ?: 0.0) < 90
                    
                    if (!isWatched || isInProgress) {
                        android.util.Log.d("JellyfinAPI", "First unwatched special found")
                        return episode
                    }
                }
            }
            
            android.util.Log.d("JellyfinAPI", "All episodes watched for series $seriesId")
            null
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error finding first unwatched episode for series $seriesId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }

    suspend fun getRecentlyAddedMovies(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("SortBy", "DateCreated")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags") // Request ImageTags for image loading
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    suspend fun getRecentlyReleasedMovies(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("SortBy", "PremiereDate")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags") // Request ImageTags for image loading
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getRecentlyAddedMoviesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("ParentId", libraryId)
                parameters.append("SortBy", "DateCreated")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags,DateCreated") // Request ImageTags and DateCreated for image loading and sorting
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getRecentlyReleasedMoviesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("ParentId", libraryId)
                parameters.append("SortBy", "PremiereDate")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags,PremiereDate") // Request ImageTags and PremiereDate for image loading and sorting
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    suspend fun getRecentlyAddedShows(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("SortBy", "DateCreated")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags,ChildCount,RecursiveItemCount") // Request ImageTags, ChildCount and RecursiveItemCount
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            // Return all items - filtering based on settings will be done in UI layer
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    suspend fun getRecentlyAddedEpisodes(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Episode")
                parameters.append("SortBy", "DateCreated")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                // Request SeriesId, SeriesName, IndexNumber, ParentIndexNumber, ImageTags, and Type fields for episodes
                parameters.append("Fields", "SeriesId,SeriesName,IndexNumber,ParentIndexNumber,ImageTags,Type")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getRecentlyAddedShowsFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("ParentId", libraryId)
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("SortBy", "DateCreated")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags,ChildCount,RecursiveItemCount")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getRecentlyAddedEpisodesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("ParentId", libraryId)
                parameters.append("IncludeItemTypes", "Episode")
                parameters.append("SortBy", "DateCreated")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Recursive", "true")
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "SeriesId,SeriesName,IndexNumber,ParentIndexNumber,ImageTags,Type")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    fun getImageUrl(itemId: String, imageType: String = "Primary", imageTag: String? = null, maxWidth: Int? = null, maxHeight: Int? = null, quality: Int? = null): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        // Default to highest resolution for detail views, but allow smaller sizes for thumbnails
        val defaultMaxWidth = maxWidth ?: 7680
        val defaultMaxHeight = maxHeight ?: 4320
        val defaultQuality = quality ?: 100
        val urlBuilder = URLBuilder().takeFrom("${base}Items/$itemId/Images/$imageType").apply {
            parameters.append("maxWidth", defaultMaxWidth.toString())
            parameters.append("maxHeight", defaultMaxHeight.toString())
            parameters.append("quality", defaultQuality.toString())
            // Add image tag if provided (for person images)
            imageTag?.let { tag ->
                parameters.append("tag", tag)
            }
        }
        return urlBuilder.buildString()
    }
    
    // Get chapter image URL
    // API endpoint: /Items/{itemId}/Images/Chapter/{chapterIndex}
    fun getChapterImageUrl(itemId: String, chapterIndex: Int, imageTag: String? = null, maxWidth: Int = 320, maxHeight: Int = 180): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val urlBuilder = URLBuilder().takeFrom("${base}Items/$itemId/Images/Chapter/$chapterIndex").apply {
            parameters.append("maxWidth", maxWidth.toString())
            parameters.append("maxHeight", maxHeight.toString())
            parameters.append("quality", "90")
            imageTag?.let { tag ->
                parameters.append("tag", tag)
            }
        }
        return urlBuilder.buildString()
    }
    
    fun getImageRequestHeaders(): Headers {
        return Headers.Builder()
            .add("Authorization", "MediaBrowser Token=\"$accessToken\"")
            .build()
    }

    suspend fun getItemDetails(itemId: String): JellyfinItem? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            // Request full item details including MediaSources for video playback and UserData for resume functionality
            val url = URLBuilder().takeFrom("${base}Items/$itemId").apply {
                parameters.append("UserId", userId)
                // Request UserData fields to get PositionTicks for resume functionality, and IndexNumber/ParentIndexNumber for episodes
                // Also request Chapters for chapter markers
                parameters.append("Fields", "MediaSources,Genres,Overview,People,ProviderIds,UserData,ImageTags,IndexNumber,ParentIndexNumber,NextEpisodeId,Chapters")
            }.buildString()
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }
            val item: JellyfinItem = response.body()
            item
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching item details: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }

    // Get person details (biography, birthdate, etc.)
    // Uses /Users/{userId}/Items/{personId} endpoint which returns full item details
    suspend fun getPersonDetails(personId: String): PersonDetails? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            // Use the Items endpoint with userId for full details including Overview
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items/$personId").buildString()
            android.util.Log.d("JellyfinAPI", "Fetching person details")
            
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }
            val person: PersonDetails = response.body()
            android.util.Log.d("JellyfinAPI", "Person details fetched")
            android.util.Log.d("JellyfinAPI", "Person birth: ${person.birthDateValue}, death: ${person.deathDateValue}, locations: ${person.ProductionLocations}")
            person
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching person details: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }

    // Get all items (movies, series) that a person appears in (filmography)
    suspend fun getPersonFilmography(personId: String, limit: Int = 50): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Items").apply {
                parameters.append("UserId", userId)
                parameters.append("PersonIds", personId)
                parameters.append("Recursive", "true")
                parameters.append("IncludeItemTypes", "Movie,Series")
                parameters.append("SortBy", "PremiereDate,ProductionYear,SortName")
                parameters.append("SortOrder", "Descending")
                parameters.append("Fields", "PrimaryImageAspectRatio,MediaSourceCount,Overview,Genres,ProductionYear")
                parameters.append("Limit", limit.toString())
            }.buildString()
            android.util.Log.d("JellyfinAPI", "Fetching person filmography")
            
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }
            val itemsResponse: ItemsResponse = response.body()
            android.util.Log.d("JellyfinAPI", "Person filmography fetched: ${itemsResponse.Items.size} items")
            itemsResponse.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching person filmography: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    /**
     * Returns trailers managed by Jellyfin for an item.  Keeping this lookup
     * in the Jellyfin service lets the UI prefer local/remote server metadata
     * before consulting an optional external metadata provider.
     */
    suspend fun getLocalTrailers(itemId: String): List<JellyfinItem> {
        return getMediaItems("Items/$itemId/LocalTrailers", fields = "MediaSources")
    }

    suspend fun getRemoteTrailers(itemId: String): List<JellyfinItem> {
        return getMediaItems("Items/$itemId/RemoteTrailers", fields = "MediaSources")
    }

    /** Returns theme songs resolved by Jellyfin, including inherited parent media. */
    suspend fun getThemeSongs(itemId: String): List<JellyfinItem> {
        return getMediaItems("Items/$itemId/ThemeSongs")
    }

    /**
     * Header-authenticated audio URL for theme music. The access token is
     * deliberately not embedded in the URL; callers must use
     * getVideoRequestHeaders() when creating the media data source.
     */
    fun getThemeSongUrl(itemId: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return URLBuilder().takeFrom("${base}Audio/$itemId/universal").apply {
            parameters.append("UserId", userId)
            parameters.append("Container", "mp3,aac,m4a,flac,ogg,webm")
            parameters.append("TranscodingContainer", "ts")
            parameters.append("TranscodingProtocol", "hls")
            parameters.append("AudioCodec", "aac")
        }.buildString()
    }

    private suspend fun getMediaItems(path: String, fields: String? = null): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("$base$path").apply {
                parameters.append("UserId", userId)
                fields?.let { parameters.append("Fields", it) }
            }.buildString()
            client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body<List<JellyfinItem>>()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("JellyfinAPI", "Trailer metadata unavailable (${e::class.simpleName})")
            emptyList()
        }
    }

    /** Resolve a person returned without an id in a lightweight item response. */
    suspend fun findPersonIdByName(name: String): String? {
        val query = name.trim()
        if (query.isEmpty()) return null
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Persons").apply {
                parameters.append("UserId", userId)
                parameters.append("SearchTerm", query)
                parameters.append("Limit", "10")
            }.buildString()
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items.firstOrNull { it.Name.equals(query, ignoreCase = true) }?.Id
                ?: response.Items.firstOrNull()?.Id
        } catch (e: Exception) {
            android.util.Log.w("JellyfinAPI", "Unable to resolve person from name")
            null
        }
    }

    // Get person image URL
    fun getPersonImageUrl(personId: String, imageType: String = "Primary", tag: String? = null, maxWidth: Int? = null, maxHeight: Int? = null): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return URLBuilder().takeFrom("${base}Items/$personId/Images/$imageType").apply {
            tag?.let { parameters.append("tag", it) }
            maxWidth?.let { parameters.append("maxWidth", it.toString()) }
            maxHeight?.let { parameters.append("maxHeight", it.toString()) }
        }.buildString()
    }

    /**
     * Get video playback URL with server-side transcoding.
     * This requests Jellyfin to transcode the video to a compatible format.
     * 
     * @param itemId The item ID
     * @param mediaSourceId Optional media source ID
     * @param subtitleStreamIndex Optional subtitle stream index
     * @param targetVideoCodec Target video codec ("h264" or "hevc")
     * @param maxBitrateMbps Maximum bitrate in Mbps
     * @param audioCodec Target audio codec (default: "aac")
     * @return HLS URL for transcoded playback
     */
    fun getTranscodedVideoUrl(
        itemId: String,
        mediaSourceId: String? = null,
        subtitleStreamIndex: Int? = null,
        targetVideoCodec: String = "h264",
        maxBitrateMbps: Int = 40,
        audioCodec: String = "aac"
    ): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val sourceId = mediaSourceId ?: itemId
        val maxBitrate = maxBitrateMbps * 1_000_000 // Convert Mbps to bps
        
        // Use HLS for transcoded playback (allows progressive streaming while transcoding)
        val hlsUrl = URLBuilder().takeFrom("${base}Videos/$itemId/master.m3u8").apply {
            // Video transcoding parameters
            parameters.append("VideoCodec", targetVideoCodec.lowercase())
            parameters.append("VideoBitrate", maxBitrate.toString())
            
            // IMPORTANT: Set max resolution to preserve original quality
            // 4K support (3840x2160)
            parameters.append("MaxWidth", "3840")
            parameters.append("MaxHeight", "2160")
            
            // Set streaming bitrate high to prevent quality reduction
            parameters.append("MaxStreamingBitrate", maxBitrate.toString())
            
            // Audio parameters - high quality
            parameters.append("AudioCodec", audioCodec.lowercase())
            parameters.append("AudioBitrate", "640000") // 640 kbps for high quality audio
            parameters.append("AudioChannels", "6") // Up to 5.1 surround
            
            // Subtitle handling
            subtitleStreamIndex?.let {
                parameters.append("SubtitleStreamIndex", it.toString())
            }
            
            // Quality preservation parameters
            parameters.append("CopyTimestamps", "true")
            parameters.append("EnableAutoStreamCopy", "false") // Force transcoding
            parameters.append("RequireNonAnamorphic", "false")
            parameters.append("TranscodingMaxAudioChannels", "6")
            
            // Device profile hints for quality
            parameters.append("mediaSourceId", sourceId)
        }.buildString()

        android.util.Log.d("JellyfinAPI", "Server transcoding stream prepared")
        android.util.Log.d("JellyfinAPI", "   Video: $targetVideoCodec @ ${maxBitrateMbps}Mbps, Audio: $audioCodec @ 640kbps")
        android.util.Log.d("JellyfinAPI", "   Max Resolution: 3840x2160 (4K)")
        return hlsUrl
    }
    
    fun getVideoPlaybackUrl(
        itemId: String,
        mediaSourceId: String? = null,
        subtitleStreamIndex: Int? = null,
        preserveQuality: Boolean = false, // Set to true for HDR videos to preserve quality
        transcodeAudio: Boolean = false, // Set to true to transcode audio (for unsupported codecs like TrueHD)
        audioCodec: String? = null, // Target audio codec for transcoding (e.g., "ac3", "aac")
        quality: PlaybackQuality = PlaybackQuality.ORIGINAL
    ): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        // Jellyfin video playback URL format: /Videos/{itemId}/stream
        // Use MediaSourceId if provided, otherwise use itemId
        val sourceId = mediaSourceId ?: itemId
        val originalFirst = quality == PlaybackQuality.ORIGINAL
        // IMPORTANT: MPV/FFmpeg requires correct parameter casing
        // - mediaSourceId (camelCase, not MediaSourceId)
        // - static (lowercase, not Static)
        // Authentication is sent by the standard MediaBrowser header configured
        // on the native player. Keep the access token out of the playback URL.
        val url = URLBuilder().takeFrom("${base}Videos/$itemId/stream").apply {
            subtitleStreamIndex?.let { 
                // Add subtitle stream index if provided
                parameters.append("SubtitleStreamIndex", it.toString())
                // Don't set SubtitleDeliveryMethod - let Jellyfin handle it
                // We'll add the subtitle URL separately to MediaItem for better compatibility
                // Set a very high max bitrate to preserve quality if transcoding is needed
                parameters.append("maxStreamingBitrate", "1000000000") // 1 Gbps - effectively no limit
                // Copy timestamps to avoid re-encoding when possible
                parameters.append("CopyTimestamps", "true")
                // Don't set static=true when subtitles are needed (allows transcoding)
            } ?: run {
                // For HDR/high-quality videos, try direct play first if audio codec is supported
                // Only use remuxing/transcoding when audio needs to be transcoded
                if (preserveQuality && transcodeAudio) {
                    // Audio needs transcoding - use HLS for progressive playback (avoids long initial buffering)
                    // HLS allows playback to start while transcoding continues
                    val targetAudioCodec = audioCodec?.lowercase() ?: "aac"
                    val hlsUrl = URLBuilder().takeFrom("${base}Videos/$itemId/master.m3u8").apply {
                        // Set maximum resolution (8K support for future-proofing)
                        parameters.append("MaxWidth", "7680")
                        parameters.append("MaxHeight", "4320")
                        // Set very high bitrate to preserve quality (1 Gbps - effectively no limit)
                        parameters.append("maxStreamingBitrate", "1000000000")
                        // Try to preserve video codec when possible (remux instead of transcode)
                        parameters.append("VideoCodec", "copy")
                        // Transcode to specified audio codec (AC3 for universal compatibility, or AAC)
                        parameters.append("AudioCodec", targetAudioCodec)
                        if (targetAudioCodec == "ac3") {
                            // AC3 supports up to 5.1 channels, use 640 kbps for high quality
                            parameters.append("AudioBitrate", "640000") // 640 kbps
                        } else {
                            // AAC supports higher bitrates and more channels
                            parameters.append("AudioBitrate", "640000") // 640 kbps for high quality audio
                        }
                        // Copy timestamps to avoid re-encoding
                        parameters.append("CopyTimestamps", "true")
                        parameters.append("mediaSourceId", sourceId)
                    }.buildString()
                    android.util.Log.d("JellyfinAPI", "Using HLS for HDR video with audio transcoding to $targetAudioCodec (progressive playback): ${SensitiveDataRedactor.url(hlsUrl)}")
                    return hlsUrl
                } else if (transcodeAudio && audioCodec != null) {
                    // Non-HDR but audio transcoding requested (e.g., AAC to AC3)
                    // Use HLS for progressive playback
                    val targetAudioCodec = audioCodec.lowercase()
                    val hlsUrl = URLBuilder().takeFrom("${base}Videos/$itemId/master.m3u8").apply {
                        parameters.append("VideoCodec", "copy")
                        parameters.append("AudioCodec", targetAudioCodec)
                        if (targetAudioCodec == "ac3") {
                            parameters.append("AudioBitrate", "640000") // 640 kbps for AC3 (5.1 max)
                        } else {
                            parameters.append("AudioBitrate", "640000")
                        }
                        parameters.append("CopyTimestamps", "true")
                        parameters.append("maxStreamingBitrate", "1000000000")
                        parameters.append("mediaSourceId", sourceId)
                    }.buildString()
                    android.util.Log.d("JellyfinAPI", "Using HLS for audio transcoding to $targetAudioCodec: ${SensitiveDataRedactor.url(hlsUrl)}")
                    return hlsUrl
                } else if (preserveQuality && !transcodeAudio) {
                    // HDR video with supported audio - try direct play first for instant startup
                    // Use static=true for direct play (fastest startup, no remuxing delay)
                    if (originalFirst) parameters.append("static", "true")
                    android.util.Log.d("JellyfinAPI", "Using direct play for HDR video with supported audio (instant startup)")
                } else {
                    // Set static=true for direct play (no transcoding) when no subtitles and not HDR
                    // Use lowercase "static" for MPV/FFmpeg compatibility
                    if (originalFirst) parameters.append("static", "true")
                }
            }
            quality.maxWidth?.let { parameters.append("MaxWidth", it.toString()) }
            quality.maxHeight?.let { parameters.append("MaxHeight", it.toString()) }
            quality.maxBitrateKbps?.let {
                parameters.append("VideoBitrate", (it * 1000).toString())
                parameters.append("MaxStreamingBitrate", (it * 1000).toString())
            }
            // Add mediaSourceId with correct casing (camelCase, not MediaSourceId)
            parameters.append("mediaSourceId", sourceId)
        }.buildString()
        android.util.Log.d("JellyfinAPI", "Generated video playback request")
        return url
    }
    
    /**
     * Get video playback URL for MPV player.
     * 
     * MPV playback strategy (in order of preference):
     * 1. Direct Play: /original endpoint (bypasses transcoder completely)
     * 2. Direct Stream: /stream with copy codecs (remux only, no transcode)
     * 3. MP4 Transcode: /stream.mp4 (more stable than HLS transcoder)
     * 4. HLS: /master.m3u8 (last resort, can crash Jellyfin's transcoder)
     * 
     * This follows best practices to avoid Jellyfin transcoder crashes:
     * - Direct play/stream is preferred over transcoding
     * - MP4 transcoding is more stable than HLS transcoding
     * - HLS is only used when absolutely necessary
     * 
     * @param itemId The item ID
     * @param mediaSourceId Optional media source ID
     * @param subtitleStreamIndex Optional subtitle stream index
     * @param preferredMethod Optional preferred playback method (null = auto-detect)
     * @return URL for MPV playback using the best available method
     */
    /**
     * Build correct Jellyfin subtitle URL based on subtitle type
     * Production-safe URL builder matching official Jellyfin clients
     * 
     * @param itemId The Jellyfin item ID
     * @param mediaSourceId The media source ID
     * @param streamIndex The REAL Jellyfin stream index (NOT array position!)
     * @param isExternal Whether this is an external subtitle file on disk
     * @param path The filesystem path (for external subtitles)
     * @param codec The subtitle codec (e.g., "subrip", "ass", "pgs")
     * @return The correct subtitle URL for this subtitle type
     */
    fun buildJellyfinSubtitleUrl(
        itemId: String,
        mediaSourceId: String?,
        streamIndex: Int,
        isExternal: Boolean,
        codec: String?,
        path: String? = null
    ): String {
        val server = if (baseUrl.endsWith("/")) baseUrl.removeSuffix("/") else baseUrl
        
        // Determine file extension from codec or path
        val extension = when (codec?.lowercase()) {
            "srt", "subrip" -> "srt"
            "vtt", "webvtt" -> "vtt"
            "ass", "ssa", "substationalpha" -> "ass"
            "ttml" -> "ttml"
            "pgs", "hdmv_pgs_subtitle" -> "sup"
            else -> {
                // Fallback: extract from file path if available
                path?.substringAfterLast('.')?.lowercase() ?: "srt"
            }
        }
        
        // ✅ CORRECT URL FORMAT (CONFIRMED WORKING)
        // Authentication is supplied through request headers by the caller.
        // Keeping the token out of the URL prevents it leaking through logs,
        // caches, browser history and intermediary proxies.
        val url = "$server/Videos/$itemId/$mediaSourceId/Subtitles/$streamIndex/Stream.$extension"
        
        android.util.Log.d("JellyfinAPI", "Subtitle stream prepared (isExternal=$isExternal, codec=$codec, ext=$extension)")
        return url
    }
    
    /**
     * Converts a relative DeliveryUrl from Jellyfin into an absolute URL
     */
    fun resolveDeliveryUrl(deliveryUrl: String): String {
        return if (deliveryUrl.startsWith("http")) {
            // Already absolute
            deliveryUrl
        } else {
            // Relative URL, prepend base
            val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
            "$base$deliveryUrl"
        }
    }
    
    /**
     * Get PlaybackInfo to retrieve correct subtitle DeliveryUrl mapping
     * This is how official Jellyfin clients resolve subtitle stream indices
     * Note: Simplified version - just re-fetch the item details which should have updated DeliveryUrl
     */
    /**
     * Get PlaybackInfo to retrieve correct subtitle DeliveryUrl mapping and burner URLs
     * API request: GET /Items/{ItemId}/PlaybackInfo?UserId={userId}&StartTimeTicks=0&IsPlayback=true&AutoOpenLiveStream=true&SubtitleMethod=Burn
     */
    suspend fun getPlaybackInfo(
        itemId: String,
        mediaSourceId: String? = null,
        subtitleStreamIndex: Int? = null,
        autoOpenLiveStream: Boolean = true
    ): JellyfinPlaybackInfo? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Items/$itemId/PlaybackInfo").apply {
                parameters.append("UserId", userId)
                parameters.append("StartTimeTicks", "0")
                parameters.append("IsPlayback", "true")
                parameters.append("AutoOpenLiveStream", autoOpenLiveStream.toString())
                mediaSourceId?.takeIf { it.isNotBlank() }?.let {
                    parameters.append("MediaSourceId", it)
                }
                
                // Request server-side subtitle burning if index is provided
                if (subtitleStreamIndex != null) {
                    parameters.append("SubtitleStreamIndex", subtitleStreamIndex.toString())
                    parameters.append("SubtitleMethod", "Burn")
                }
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Fetching PlaybackInfo")
            
            // Allow POST as well, but GET is sufficient and easier for this
            val response: JellyfinPlaybackInfo = client.post(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
                // Empty body for POST
                setBody("{}")
                contentType(ContentType.Application.Json)
            }.body()
            
            if (response.MediaSources != null) {
                val source = response.MediaSources.firstOrNull()
                android.util.Log.d("JellyfinAPI", "✅ PlaybackInfo received. TranscodingUrl present: ${source?.TranscodingUrl != null}")
                if (source?.TranscodingUrl != null) {
                    android.util.Log.d("JellyfinAPI", "🔥 Burn-in URL: ${SensitiveDataRedactor.url(source.TranscodingUrl)}")
                }
            } else {
                android.util.Log.w("JellyfinAPI", "⚠️ PlaybackInfo returned no MediaSources")
            }
            
            response
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "❌ Failed to get PlaybackInfo: ${SensitiveDataRedactor.message(e)}")
            null
        }
    }

    fun getVideoRequestHeaders(): Map<String, String> {
        // Get DeviceId from config (should be stored during login)
        // If not available, use fallback (but it should be stored)
        val deviceId = clientDeviceId
        
        // Build the standard MediaBrowser header with token and device id.
        // Format: MediaBrowser Client="...", Device="...", DeviceId="...", Version="...", Token="..."
        // The token is included here so native playback remains authenticated.
        val mediaBrowserAuthHeader = "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$deviceId\", Version=\"${BuildConfig.VERSION_NAME}\", Token=\"$accessToken\""
        
        return mapOf(
            "Authorization" to mediaBrowserAuthHeader
        )
    }
    
    /**
     * Get Media Segments for an item (Skip Intro / Skip Credits)
     * Requires Jellyfin 10.10+ with Intro Skipper plugin
     * Returns skip markers for intro and credits if available
     */
    suspend fun getMediaSegments(itemId: String): SkipMarkers {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}MediaSegments/$itemId").apply {
                parameters.append("IncludeSegmentTypes", "Intro,Outro")
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Fetching MediaSegments for item: $itemId")
            
            val response: MediaSegmentsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            val segments = response.Items ?: emptyList()
            android.util.Log.d("JellyfinAPI", "Found ${segments.size} media segments")
            
            // Extract intro and credits (outro) segments
            val introSegment = segments.find { it.Type?.equals("Intro", ignoreCase = true) == true }
            val outroSegment = segments.find { it.Type?.equals("Outro", ignoreCase = true) == true }
            
            val markers = SkipMarkers(
                introStartMs = introSegment?.startMs,
                introEndMs = introSegment?.endMs,
                creditsStartMs = outroSegment?.startMs
            )
            
            android.util.Log.d("JellyfinAPI", "Skip markers: intro=${markers.introStartMs}-${markers.introEndMs}ms, credits=${markers.creditsStartMs}ms")
            markers
        } catch (e: Exception) {
            android.util.Log.d("JellyfinAPI", "MediaSegments not available (server may not support it): ${e::class.simpleName}")
            // Return empty markers if not supported
            SkipMarkers()
        }
    }
    
    /**
     * Build correct Jellyfin subtitle URL for streaming subtitles.
     * Authentication is supplied through the Jellyfin request headers.
     */
    fun buildSubtitleUrl(itemId: String, mediaSourceId: String, index: Int): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${base}Videos/$itemId/$mediaSourceId/Subtitles/$index/Stream"
    }

    suspend fun getLibraries(): List<JellyfinLibrary> {
        return try {
            val url = if (baseUrl.endsWith("/")) {
                "${baseUrl}Users/$userId/Views"
            } else {
                "$baseUrl/Users/$userId/Views"
            }
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Convert JellyfinItems to JellyfinLibraries
            response.Items.map { item ->
                JellyfinLibrary(
                    Id = item.Id,
                    Name = item.Name,
                    Type = item.Type,
                    CollectionType = item.CollectionType, // "movies", "tvshows", "music", etc.
                    ImageTags = item.ImageTags
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    suspend fun getLibraryItems(libraryId: String, limit: Int = 100, startIndex: Int = 0): ItemsResponse {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "false")
                parameters.append("IncludeItemTypes", "Movie,Series,Episode")
                parameters.append("Limit", limit.toString())
                parameters.append("StartIndex", startIndex.toString())
                parameters.append("Fields", "DateCreated,PremiereDate,Overview,UserData,ImageTags,ChildCount,RecursiveItemCount,Genres") // Include DateCreated, ChildCount and RecursiveItemCount for filtering empty shows
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            // Return all items - filtering based on settings will be done in UI layer
            response
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            ItemsResponse(Items = emptyList(), TotalRecordCount = 0)
        }
    }
    
    suspend fun getCollections(): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "BoxSet")
                parameters.append("Recursive", "true")
                parameters.append("Fields", "ImageTags,ChildCount")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getAllLibraryItems(libraryId: String, limit: Int = 100): List<JellyfinItem> {
        val allItems = mutableListOf<JellyfinItem>()
        var startIndex = 0
        var totalCount = 0
        
        do {
            val response = getLibraryItems(libraryId, limit, startIndex)
            allItems.addAll(response.Items)
            
            // Update total count from first response
            if (totalCount == 0) {
                totalCount = response.TotalRecordCount
            }
            
            // Move to next page
            startIndex += limit
        } while (allItems.size < totalCount && response.Items.isNotEmpty())
        
        // Return all items - filtering based on settings will be done in UI layer
        return allItems
    }
    
    suspend fun getSeasons(seriesId: String, forceRefresh: Boolean = false): List<JellyfinItem> {
        // Check cache first
        if (!forceRefresh) {
            seasonCache[seriesId]?.let { (timestamp, seasons) ->
                if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
                    android.util.Log.d("JellyfinAPI", "Using cached seasons for series $seriesId")
                    return seasons
                }
            }
        }
        
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Shows/${seriesId}/Seasons").apply {
                parameters.append("UserId", userId)
                parameters.append("Fields", "Overview,UserData,ImageTags")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            val seasons = response.Items.sortedBy { it.IndexNumber ?: 0 }
            
            // Cache the result
            seasonCache[seriesId] = System.currentTimeMillis() to seasons
            seasons
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching seasons for series $seriesId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getEpisodes(seriesId: String, seasonId: String, forceRefresh: Boolean = false): List<JellyfinItem> {
        // Check cache first
        if (!forceRefresh) {
            episodeCache[seasonId]?.let { (timestamp, episodes) ->
                if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
                    android.util.Log.d("JellyfinAPI", "Using cached episodes for season $seasonId")
                    return episodes
                }
            }
        }
        
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Shows/${seriesId}/Episodes").apply {
                parameters.append("UserId", userId)
                parameters.append("SeasonId", seasonId)
                parameters.append("Fields", "Overview,UserData,SeriesName,SeriesId,ImageTags,IndexNumber,ParentIndexNumber,Type")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            val episodes = response.Items.sortedBy { it.IndexNumber ?: 0 }
            
            // Cache the result
            episodeCache[seasonId] = System.currentTimeMillis() to episodes
            episodes
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching episodes for season $seasonId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    // Clear episode cache for a specific season (useful after playback state changes)
    fun invalidateEpisodeCache(seasonId: String? = null) {
        if (seasonId != null) {
            episodeCache.remove(seasonId)
        } else {
            episodeCache.clear()
        }
    }
    
    /**
     * Get next episodes starting from a specific episode index in a season
     * This is similar to the official Jellyfin Android TV app's createNextEpisodesRequest
     * @param seasonId The season ID
     * @param startIndex The episode index number to start from (1-based, but API uses 0-based for startIndex)
     * @param limit Maximum number of episodes to return
     */
    suspend fun getNextEpisodes(seasonId: String, startIndex: Int, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            // Use Users/{userId}/Items endpoint with parentId=seasonId and startIndex
            // Note: startIndex in API is 0-based, but episode IndexNumber is 1-based
            // We need to convert: if episode IndexNumber is 5, we want episodes starting from index 4 (0-based)
            val apiStartIndex = (startIndex - 1).coerceAtLeast(0) // Convert 1-based to 0-based
            
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("ParentId", seasonId)
                parameters.append("IncludeItemTypes", "Episode")
                parameters.append("StartIndex", apiStartIndex.toString())
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "Overview,UserData,SeriesName,SeriesId,ImageTags,IndexNumber,ParentIndexNumber")
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Fetching next episodes: seasonId=$seasonId, startIndex=$startIndex (API: $apiStartIndex)")
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            android.util.Log.d("JellyfinAPI", "Found ${response.Items.size} episodes starting from index $startIndex")
            response.Items.sortedBy { it.IndexNumber ?: 0 }
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching next episodes for season $seasonId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    /**
     * Get the next episode in the same season
     * Uses /Shows/{seriesId}/Episodes with SeasonId filter to stay within the current season
     * @param seriesId The series ID
     * @param seasonId The current season ID (required to filter within the same season)
     * @param currentEpisodeIndex The current episode's IndexNumber (1-based)
     * @param currentSeasonNumber The current season number (ParentIndexNumber) for logging
     * @return The next episode in the same season, or null if this is the last episode of the season
     */
    suspend fun getNextEpisodeInSeason(seriesId: String, seasonId: String, currentEpisodeIndex: Int, currentSeasonNumber: Int): JellyfinItem? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            
            // Get all episodes in the current season and find the next one
            val url = URLBuilder().takeFrom("${base}Shows/$seriesId/Episodes").apply {
                parameters.append("UserId", userId)
                parameters.append("SeasonId", seasonId) // Filter by season to stay within the same season
                parameters.append("Fields", "MediaSources,Overview,UserData,SeriesName,SeriesId,ImageTags,IndexNumber,ParentIndexNumber")
                parameters.append("SortBy", "IndexNumber")
                parameters.append("SortOrder", "Ascending")
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Fetching episodes in season: seriesId=$seriesId, seasonId=$seasonId, currentEpisode=$currentEpisodeIndex, season=$currentSeasonNumber")
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Find the episode with IndexNumber = currentEpisodeIndex + 1
            val nextEpisodeNumber = currentEpisodeIndex + 1
            val nextEpisode = response.Items.firstOrNull { it.IndexNumber == nextEpisodeNumber }
            
            if (nextEpisode != null) {
                android.util.Log.d("JellyfinAPI", "Next episode in season found")
            } else {
                android.util.Log.d("JellyfinAPI", "No next episode in S${currentSeasonNumber} after E${currentEpisodeIndex} (last episode of season)")
            }
            nextEpisode
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching next episode in season: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }
    
    /**
     * Get the next episode directly using the simpler StartIndex approach (across all seasons)
     * NOTE: This returns episodes across ALL seasons, use getNextEpisodeInSeason for same-season lookup
     * @param seriesId The series ID
     * @param currentEpisodeIndex The current episode's IndexNumber (1-based)
     * @return The next episode, or null if not found
     */
    suspend fun getNextEpisode(seriesId: String, currentEpisodeIndex: Int): JellyfinItem? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            // Use StartIndex = currentIndex + 1 (API uses 0-based, but IndexNumber is 1-based)
            // If current episode is IndexNumber 5, we want StartIndex=5 (which is index 5 in 0-based, meaning episode 6)
            val startIndex = currentEpisodeIndex // API StartIndex matches 1-based IndexNumber for episodes
            
            val url = URLBuilder().takeFrom("${base}Shows/$seriesId/Episodes").apply {
                parameters.append("UserId", userId)
                parameters.append("StartIndex", startIndex.toString())
                parameters.append("Limit", "1")
                parameters.append("Fields", "MediaSources,Overview,UserData,SeriesName,SeriesId,ImageTags,IndexNumber,ParentIndexNumber")
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Fetching next episode (all seasons): seriesId=$seriesId, StartIndex=$startIndex (current episode index=$currentEpisodeIndex)")
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            val nextEpisode = response.Items.firstOrNull()
            if (nextEpisode != null) {
                android.util.Log.d("JellyfinAPI", "Next episode found")
            } else {
                android.util.Log.d("JellyfinAPI", "No next episode found (this might be the last episode)")
            }
            nextEpisode
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching next episode: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            null
        }
    }
    
    suspend fun getUnwatchedEpisodeCount(seriesId: String): Int {
        return try {
            val seasons = getSeasons(seriesId)
            var unwatchedCount = 0
            
            seasons.forEach { season ->
                val episodes = getEpisodes(seriesId, season.Id)
                episodes.forEach { episode ->
                    val isWatched = episode.UserData?.Played == true || 
                                   episode.UserData?.PlayedPercentage == 100.0
                    if (!isWatched) {
                        unwatchedCount++
                    }
                }
            }
            
            unwatchedCount
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error getting unwatched episode count for series $seriesId: ${SensitiveDataRedactor.message(e)}")
            0
        }
    }
    
    suspend fun getMoviesByGenre(genre: String, excludeItemId: String? = null, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("Genres", genre)
                parameters.append("Recursive", "true")
                parameters.append("Limit", limit.toString())
                excludeItemId?.let { parameters.append("ExcludeItemIds", it) }
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching movies by genre: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getSeriesByGenre(genre: String, excludeItemId: String? = null, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("Genres", genre)
                parameters.append("Recursive", "true")
                parameters.append("Limit", limit.toString())
                excludeItemId?.let { parameters.append("ExcludeItemIds", it) }
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching series by genre: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    suspend fun getMoviesByPerson(personId: String, excludeItemId: String? = null, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("PersonIds", personId)
                parameters.append("Recursive", "true")
                parameters.append("Limit", limit.toString())
                excludeItemId?.let { parameters.append("ExcludeItemIds", it) }
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching movies by person: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }
    
    /**
     * Get continue watching items filtered by type (Movies only)
     */
    suspend fun getContinueWatchingMovies(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items/Resume").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("Fields", "ImageTags,UserData")
                parameters.append("SortBy", "DatePlayed")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Sort client-side by LastPlayedDate (most recently played first)
            response.Items.sortedByDescending { item ->
                item.getLastPlayedDateForSort()
            }
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching continue watching movies: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get top unwatched movies (highest rated unwatched movies)
     */
    suspend fun getTopUnwatchedMovies(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("Recursive", "true")
                parameters.append("IsPlayed", "false") // Only unwatched
                parameters.append("SortBy", "CommunityRating")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,CommunityRating,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching top unwatched movies: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get recently watched movies (movies that have been fully watched)
     */
    suspend fun getRecentlyWatchedMovies(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("Recursive", "true")
                parameters.append("IsPlayed", "true") // Only watched
                parameters.append("SortBy", "DatePlayed")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching recently watched movies: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get favorite movies
     */
    suspend fun getFavoriteMovies(limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("Recursive", "true")
                parameters.append("IsFavorite", "true")
                parameters.append("SortBy", "SortName")
                parameters.append("SortOrder", "Ascending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching favorite movies: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    // ==================== LIBRARY-SPECIFIC MOVIE METHODS ====================
    // These methods filter movies by a specific library ID to ensure
    // different libraries (e.g., "Movies" vs "Movies 4K") are treated separately
    
    /**
     * Get continue watching items from a specific library (Movies only)
     */
    suspend fun getContinueWatchingMoviesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items/Resume").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("ParentId", libraryId)
                parameters.append("Fields", "ImageTags,UserData")
                parameters.append("SortBy", "DatePlayed")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Sort client-side by LastPlayedDate (most recently played first)
            response.Items.sortedByDescending { item ->
                item.getLastPlayedDateForSort()
            }
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching continue watching movies from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get top unwatched movies from a specific library (highest rated unwatched movies)
     */
    suspend fun getTopUnwatchedMoviesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("IsPlayed", "false") // Only unwatched
                parameters.append("SortBy", "CommunityRating")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,CommunityRating,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching top unwatched movies from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get recently watched movies from a specific library (movies that have been fully watched)
     */
    suspend fun getRecentlyWatchedMoviesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("IsPlayed", "true") // Only watched
                parameters.append("SortBy", "DatePlayed")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching recently watched movies from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get favorite movies from a specific library
     */
    suspend fun getFavoriteMoviesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("IsFavorite", "true")
                parameters.append("SortBy", "SortName")
                parameters.append("SortOrder", "Ascending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching favorite movies from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    // ==================== LIBRARY-SPECIFIC TV SHOWS METHODS ====================
    // These methods filter TV shows/episodes by a specific library ID to ensure
    // different libraries are treated separately
    
    /**
     * Get continue watching episodes from a specific TV library
     */
    suspend fun getContinueWatchingEpisodesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items/Resume").apply {
                parameters.append("IncludeItemTypes", "Episode")
                parameters.append("ParentId", libraryId)
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "ImageTags,UserData,SeriesName,SeriesId,IndexNumber,ParentIndexNumber,Type")
                parameters.append("SortBy", "DatePlayed")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Sort client-side by LastPlayedDate (most recently played first)
            response.Items.sortedByDescending { item ->
                item.getLastPlayedDateForSort()
            }
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching continue watching episodes from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get next up episodes from a specific TV library
     */
    suspend fun getNextUpEpisodesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Shows/NextUp").apply {
                parameters.append("UserId", userId)
                parameters.append("ParentId", libraryId)
                parameters.append("Limit", limit.toString())
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "ImageTags,UserData,SeriesName,SeriesId,Type")
                parameters.append("EnableResumable", "false")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching next up episodes from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get recently released episodes from a specific TV library (sorted by premiere date)
     */
    suspend fun getRecentlyReleasedEpisodesFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Episode")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("SortBy", "PremiereDate")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                // Explicitly include Type field to ensure proper routing in UI
                parameters.append("Fields", "ImageTags,SeriesName,SeriesId,IndexNumber,ParentIndexNumber,PremiereDate,Type")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching recently released episodes from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get random unwatched TV shows from a specific library (for "Start Watching" suggestions)
     */
    suspend fun getRandomUnwatchedShowsFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("IsPlayed", "false") // Only unwatched
                parameters.append("SortBy", "Random")
                parameters.append("SortOrder", "Ascending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,Genres,CommunityRating,UserData,Overview")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching random unwatched shows from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get top rated TV shows from a specific library
     */
    suspend fun getTopRatedShowsFromLibrary(libraryId: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("SortBy", "CommunityRating")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,Genres,CommunityRating,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching top rated shows from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get TV shows by genre from a specific library
     */
    suspend fun getShowsByGenreFromLibrary(libraryId: String, genre: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("ParentId", libraryId)
                parameters.append("Recursive", "true")
                parameters.append("Genres", genre)
                parameters.append("SortBy", "Random")
                parameters.append("SortOrder", "Ascending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,Genres,CommunityRating,UserData,ChildCount,RecursiveItemCount")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching shows by genre '$genre' from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }
    
    /**
     * Get available genres from a specific TV library
     */
    suspend fun getGenresFromLibrary(libraryId: String): List<String> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Genres").apply {
                parameters.append("ParentId", libraryId)
                parameters.append("IncludeItemTypes", "Series")
                parameters.append("SortBy", "SortName")
                parameters.append("SortOrder", "Ascending")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items.mapNotNull { it.Name }
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching genres from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }

    /**
     * Get available genres from a specific Movie library
     */
    suspend fun getMovieGenresFromLibrary(libraryId: String): List<String> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Genres").apply {
                parameters.append("ParentId", libraryId)
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("SortBy", "SortName")
                parameters.append("SortOrder", "Ascending")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items.mapNotNull { it.Name }
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching movie genres from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }

    /**
     * Get movies by genre from a specific library
     */
    suspend fun getMoviesByGenreFromLibrary(libraryId: String, genre: String, limit: Int = 20): List<JellyfinItem> {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("ParentId", libraryId)
                parameters.append("IncludeItemTypes", "Movie")
                parameters.append("Genres", genre)
                parameters.append("Recursive", "true")
                parameters.append("SortBy", "CommunityRating,SortName")
                parameters.append("SortOrder", "Descending")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "Overview,Genres,CommunityRating,CriticRating,ProviderIds,UserData")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error fetching movies by genre '$genre' from library $libraryId: ${SensitiveDataRedactor.message(e)}")
            emptyList()
        }
    }

    /**
     * Mark an item as watched
     * POST /Users/{UserId}/PlayedItems/{ItemId}
     * Reference: https://api.jellyfin.org/
     */
    suspend fun markAsWatched(itemId: String): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${base}Users/$userId/PlayedItems/$itemId"
            
            val response = client.post(url) {
                header("Authorization", "MediaBrowser Token=\"$accessToken\"")
                // Jellyfin API for PlayedItems POST expects an empty body or a specific PlaybackReportingPostRequest
                // For simply marking as played, an empty POST body is sufficient.
            }

            val isSuccessful = response.status.value in 200..299
            android.util.Log.d("JellyfinAPI", "Mark watched response: ${response.status.value}")
            
            if (isSuccessful) {
                // Invalidate caches to ensure UI refreshes correctly
                // We need to invalidate both the specific item and potentially its season/series containers
                // Since we don't have the season ID handy here easily without looking it up,
                // we'll clear the entire episode cache to be safe and ensure freshness
                android.util.Log.d("JellyfinAPI", "Invalidating episode caches after marking as watched")
                episodeCache.clear()
                // itemDetailsCache.remove(itemId)
                android.util.Log.d("JellyfinAPI", "Marked item $itemId as watched")
            } else {
                android.util.Log.e("JellyfinAPI", "Failed to mark item $itemId as watched. Status: ${response.status.value}")
            }
            isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error marking item as watched: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            false
        }
    }
    
    /**
     * Mark an item as unwatched
     * DELETE /Users/{UserId}/PlayedItems/{ItemId}
     * Reference: https://api.jellyfin.org/
     */
    /**
     * Mark an item as unwatched
     * DELETE /Users/{UserId}/PlayedItems/{ItemId}
     * Reference: https://api.jellyfin.org/
     */
    suspend fun markAsUnwatched(itemId: String): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${base}Users/$userId/PlayedItems/$itemId"
            
            android.util.Log.d("JellyfinAPI", "Marking item as unwatched")
            
            val response = client.delete(url) {
                header("Authorization", "MediaBrowser Token=\"$accessToken\"")
            }

            val isSuccessful = response.status.value in 200..299
            android.util.Log.d("JellyfinAPI", "Mark unwatched response: ${response.status.value}")
            
            if (isSuccessful) {
                // Invalidate caches to ensure UI refreshes correctly
                android.util.Log.d("JellyfinAPI", "Invalidating episode caches after marking as unwatched")
                episodeCache.clear()
            }
            
            isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error marking item as unwatched: ${SensitiveDataRedactor.message(e)}")
            false
        }
    }

    /**
     * Report playback started to Jellyfin
     * POST /Sessions/Playing
     * This MUST be called before reportPlaybackProgress to establish a session
     * Reference: https://api.jellyfin.org/
     * 
     * @param itemId The item ID being played
     * @param positionTicks Starting playback position in ticks
     */
    suspend fun reportPlaybackStart(
        itemId: String,
        positionTicks: Long = 0,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${base}Sessions/Playing"
            
            val deviceId = config?.deviceId ?: ""
            val authHeader = if (deviceId.isNotEmpty()) {
                "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$deviceId\", Token=\"$accessToken\", Version=\"${BuildConfig.VERSION_NAME}\""
            } else {
                "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$clientDeviceId\", Version=\"${BuildConfig.VERSION_NAME}\""
            }
            
            // Build request body as JSON string
            val requestBody = buildString {
                append("{")
                append("\"ItemId\":\"$itemId\",")
                append("\"PositionTicks\":$positionTicks,")
                append("\"PlayMethod\":\"DirectStream\",")
                if (audioStreamIndex != null) append("\"AudioStreamIndex\":$audioStreamIndex,")
                if (subtitleStreamIndex != null) append("\"SubtitleStreamIndex\":$subtitleStreamIndex,")
                append("\"CanSeek\":true")
                append("}")
            }
            
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            android.util.Log.d("JellyfinAPI", "✅ Reported playback START for item $itemId at position $positionTicks ticks (status: ${response.status})")
            true
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "❌ Error reporting playback start: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            false
        }
    }

    /**
     * Report playback progress to Jellyfin
     * POST /Sessions/Playing/Progress
     * Reference: https://api.jellyfin.org/
     * 
     * @param itemId The item ID being played
     * @param positionTicks Current playback position in ticks (100-nanosecond intervals: 10,000,000 ticks = 1 second)
     * @param isPaused Whether playback is paused
     * @param isMuted Whether audio is muted
     * @param volumeLevel Volume level (0-100)
     * @param playbackRate Playback rate (1.0 = normal speed)
     */
    suspend fun reportPlaybackProgress(
        itemId: String,
        positionTicks: Long,
        isPaused: Boolean = false,
        isMuted: Boolean = false,
        volumeLevel: Int = 100,
        playbackRate: Double = 1.0,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${base}Sessions/Playing/Progress"
            
            val deviceId = config?.deviceId ?: ""
            val authHeader = if (deviceId.isNotEmpty()) {
                "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$deviceId\", Token=\"$accessToken\", Version=\"${BuildConfig.VERSION_NAME}\""
            } else {
                "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$clientDeviceId\", Version=\"${BuildConfig.VERSION_NAME}\""
            }
            
            // Build request body as JSON string
            val requestBody = buildString {
                append("{")
                append("\"ItemId\":\"$itemId\",")
                append("\"PositionTicks\":$positionTicks,")
                append("\"IsPaused\":$isPaused,")
                append("\"IsMuted\":$isMuted,")
                append("\"VolumeLevel\":$volumeLevel,")
                append("\"PlayMethod\":\"DirectStream\",")
                if (audioStreamIndex != null) append("\"AudioStreamIndex\":$audioStreamIndex,")
                if (subtitleStreamIndex != null) append("\"SubtitleStreamIndex\":$subtitleStreamIndex,")
                append("\"PlaybackRate\":$playbackRate")
                append("}")
            }
            
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            android.util.Log.d("JellyfinAPI", "📊 Reported playback PROGRESS for item $itemId at ${positionTicks / 10_000_000}s (status: ${response.status})")
            true
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "❌ Error reporting playback progress: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            false
        }
    }

    /**
     * Report playback stopped
     * POST /Sessions/Playing/Stopped
     * Reference: https://api.jellyfin.org/
     */
    suspend fun reportPlaybackStopped(
        itemId: String,
        positionTicks: Long,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${base}Sessions/Playing/Stopped"
            
            val deviceId = config?.deviceId ?: ""
            val authHeader = if (deviceId.isNotEmpty()) {
                "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$deviceId\", Token=\"$accessToken\", Version=\"${BuildConfig.VERSION_NAME}\""
            } else {
                "MediaBrowser Client=\"Velora\", Device=\"$clientDeviceName\", DeviceId=\"$clientDeviceId\", Version=\"${BuildConfig.VERSION_NAME}\""
            }
            
            // Build request body as JSON string
            val requestBody = buildString {
                append("{")
                append("\"ItemId\":\"$itemId\",")
                append("\"PositionTicks\":$positionTicks,")
                if (audioStreamIndex != null) append("\"AudioStreamIndex\":$audioStreamIndex,")
                if (subtitleStreamIndex != null) append("\"SubtitleStreamIndex\":$subtitleStreamIndex")
                append("}")
            }
            
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            android.util.Log.d("JellyfinAPI", "🛑 Reported playback STOPPED for item $itemId at ${positionTicks / 10_000_000}s (status: ${response.status})")
            true
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "❌ Error reporting playback stopped: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            false
        }
    }

    /**
     * Refresh a specific item's metadata on the Jellyfin server
     * POST /Items/{itemId}/Refresh
     * Triggers a rescan of the item's folder to detect new files (like external subtitles)
     * Reference: https://api.jellyfin.org/
     * 
     * @param itemId The ID of the item to refresh
     * @param metadataRefreshMode How to refresh metadata: "None", "ValidationOnly", "Default", "FullRefresh"
     * @param imageRefreshMode How to refresh images: "None", "ValidationOnly", "Default", "FullRefresh"
     * @param replaceAllMetadata Whether to replace all metadata
     * @param replaceAllImages Whether to replace all images
     */
    suspend fun refreshItemMetadata(
        itemId: String,
        metadataRefreshMode: String = "ValidationOnly",
        imageRefreshMode: String = "None",
        replaceAllMetadata: Boolean = false,
        replaceAllImages: Boolean = false
    ): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Items/$itemId/Refresh").apply {
                parameters.append("MetadataRefreshMode", metadataRefreshMode)
                parameters.append("ImageRefreshMode", imageRefreshMode)
                parameters.append("ReplaceAllMetadata", replaceAllMetadata.toString())
                parameters.append("ReplaceAllImages", replaceAllImages.toString())
            }.buildString()
            
            android.util.Log.d("JellyfinAPI", "Refreshing item metadata for $itemId")
            
            client.post(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }
            android.util.Log.d("JellyfinAPI", "Item metadata refresh triggered successfully for $itemId")
            true
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error refreshing item metadata for $itemId: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            false
        }
    }

    /**
     * Refresh library scan on the Jellyfin server
     * POST /Library/Refresh
     * Triggers a library scan to detect new or updated media
     * Reference: https://api.jellyfin.org/
     */
    suspend fun refreshLibrary(libraryId: String? = null): Boolean {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = if (libraryId != null) {
                URLBuilder().takeFrom("${base}Library/Refresh").apply {
                    parameters.append("libraryId", libraryId)
                }.buildString()
            } else {
                "${base}Library/Refresh"
            }
            
            android.util.Log.d("JellyfinAPI", "Triggering library refresh")
            
            client.post(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }
            android.util.Log.d("JellyfinAPI", "Library refresh triggered successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error triggering library refresh: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            false
        }
    }
    
    /**
     * Search for items (movies, TV shows, episodes) by query
     * GET /Users/{UserId}/Items?SearchTerm={query}
     * Reference: https://api.jellyfin.org/
     */
    suspend fun searchItems(query: String, limit: Int = 50): List<JellyfinItem> {
        return try {
            if (query.isBlank()) return emptyList()
            
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("SearchTerm", query)
                parameters.append("Recursive", "true")
                parameters.append("IncludeItemTypes", "Movie,Series,Episode")
                parameters.append("Limit", limit.toString())
                parameters.append("Fields", "ImageTags,UserData,SeriesName,SeriesId,ChildCount")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            response.Items
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error searching for items: ${SensitiveDataRedactor.message(e)}")
            android.util.Log.e("VeloraNetwork", "Request failed (${e::class.simpleName})")
            emptyList()
        }
    }

    /**
     * Find an item in the Jellyfin library by TMDB ID
     * This searches for movies or series that have a matching TMDB provider ID
     * 
     * @param tmdbId The TMDB ID to search for
     * @param itemType The type of item to search for ("Movie" or "Series")
     * @return The matching JellyfinItem if found, null otherwise
     */
    suspend fun findItemByTmdbId(tmdbId: Int, itemType: String = "Movie"): JellyfinItem? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("Recursive", "true")
                parameters.append("IncludeItemTypes", itemType)
                parameters.append("HasTmdbId", "true")
                parameters.append("Fields", "ProviderIds,ImageTags,UserData")
                parameters.append("Limit", "100") // Limit results, we'll filter by TMDB ID
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Find the item with matching TMDB ID
            val matchingItem = response.Items.find { item ->
                item.ProviderIds?.get("Tmdb") == tmdbId.toString()
            }
            
            if (matchingItem != null) {
                android.util.Log.d("JellyfinAPI", "Matching item found by provider ID")
            } else {
                android.util.Log.d("JellyfinAPI", "No $itemType found with TMDB ID $tmdbId")
            }
            
            matchingItem
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error finding item by TMDB ID: ${SensitiveDataRedactor.message(e)}")
            null
        }
    }

    /**
     * Find an item in the Jellyfin library by searching for the title
     * Falls back to this if TMDB ID search fails
     * 
     * @param title The title to search for
     * @param year The release year (optional, for more accurate matching)
     * @param itemType The type of item to search for ("Movie" or "Series")
     * @return The matching JellyfinItem if found, null otherwise
     */
    suspend fun findItemByTitle(title: String, year: String? = null, itemType: String = "Movie"): JellyfinItem? {
        return try {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = URLBuilder().takeFrom("${base}Users/$userId/Items").apply {
                parameters.append("SearchTerm", title)
                parameters.append("Recursive", "true")
                parameters.append("IncludeItemTypes", itemType)
                parameters.append("Fields", "ProductionYear,ImageTags,UserData")
                parameters.append("Limit", "20")
            }.buildString()
            
            val response: ItemsResponse = client.get(url) {
                header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
            }.body()
            
            // Find the best matching item
            val matchingItem = if (year != null) {
                // Try to match by year first for more accuracy
                response.Items.find { item ->
                    item.ProductionYear?.toString() == year && 
                    item.Name?.equals(title, ignoreCase = true) == true
                } ?: response.Items.find { item ->
                    item.Name?.equals(title, ignoreCase = true) == true
                } ?: response.Items.firstOrNull()
            } else {
                response.Items.find { item ->
                    item.Name?.equals(title, ignoreCase = true) == true
                } ?: response.Items.firstOrNull()
            }
            
            if (matchingItem != null) {
                android.util.Log.d("JellyfinAPI", "Matching item found by title")
            } else {
                android.util.Log.d("JellyfinAPI", "No $itemType found with title '$title'")
            }
            
            matchingItem
        } catch (e: Exception) {
            android.util.Log.e("JellyfinAPI", "Error finding item by title: ${SensitiveDataRedactor.message(e)}")
            null
        }
    }

}
