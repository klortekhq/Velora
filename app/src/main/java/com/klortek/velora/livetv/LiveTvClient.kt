package com.klortek.velora.livetv

import com.klortek.velora.BuildConfig
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.jellyfin.MediaSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class LiveTvChannelsResponse(
    val Items: List<LiveTvChannel> = emptyList(),
    val TotalRecordCount: Int = 0
)

@Serializable
data class LiveTvChannel(
    val Id: String,
    val Name: String,
    val ChannelNumber: String? = null,
    val Type: String? = null,
    /** Provider/source classification when Jellyfin exposes it (for example IPTV). */
    val ChannelType: String? = null,
    /** Human-readable provider name when several sources share one channel. */
    val ServiceName: String? = null,
    val ImageTags: Map<String, String>? = null,
    val UserData: LiveTvUserData? = null,
    val Tags: List<String>? = null,
    /** Optional source descriptors returned by Jellyfin for multi-source channels. */
    val MediaSources: List<MediaSource>? = null,
    val CurrentProgram: LiveTvProgram? = null,
    val UpcomingProgram: LiveTvProgram? = null
)

@Serializable
data class LiveTvProgram(
    val Id: String? = null,
    val ChannelId: String? = null,
    val Name: String? = null,
    val SeriesName: String? = null,
    val EpisodeTitle: String? = null,
    val SeasonNumber: Int? = null,
    val EpisodeNumber: Int? = null,
    val Overview: String? = null,
    val StartDate: String? = null,
    val EndDate: String? = null,
    val IsLive: Boolean? = null,
    val IsSports: Boolean? = null,
    val IsNews: Boolean? = null
)

@Serializable
data class LiveTvUserData(
    val IsFavorite: Boolean = false
)

@Serializable
data class LiveTvProgramsResponse(
    val Items: List<LiveTvProgram> = emptyList()
)

class LiveTvClient(private val config: JellyfinConfig) {
    private val baseUrl = config.serverUrl.removeSuffix("/")
    private val accessToken = config.accessToken
    private val userId = config.userId

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            connectTimeout = 10_000
            socketTimeout = 20_000
        }
    }

    val imageHeaders: Headers
        get() = Headers.Builder()
            .add("X-Emby-Token", accessToken)
            .build()

    suspend fun getChannels(): List<LiveTvChannel> {
        if (!config.isConfigured()) return emptyList()

        // Jellyfin may cap an unbounded request. Fetch pages while preserving
        // the server/provider order so every M3U-backed channel remains
        // playable and channel surfing does not silently skip entries.
        val pageSize = 100
        val channels = mutableListOf<LiveTvChannel>()
        var startIndex = 0
        var totalRecordCount: Int? = null
        do {
            val page = getChannelsPage(startIndex, pageSize)
            channels += page.Items
            totalRecordCount = page.TotalRecordCount.takeIf { it > 0 } ?: totalRecordCount
            startIndex += page.Items.size
        } while (page.Items.isNotEmpty() && startIndex < (totalRecordCount ?: Int.MAX_VALUE))

        return channels
    }

    /** Load only the next six hours so a large EPG is never rendered eagerly. */
    suspend fun getUpcomingPrograms(channelIds: List<String>): Map<String, LiveTvProgram> {
        if (!config.isConfigured() || channelIds.isEmpty()) return emptyMap()
        val nowMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = dateFormat.format(Date(nowMillis))
        val until = dateFormat.format(Date(nowMillis + 6 * 60 * 60 * 1000L))
        val programs = channelIds.chunked(500).flatMap { channelChunk ->
            val url = URLBuilder().takeFrom("$baseUrl/LiveTv/Programs").apply {
                parameters.append("UserId", userId)
                channelChunk.forEach { parameters.append("ChannelIds", it) }
                parameters.append("MinStartDate", now)
                parameters.append("MaxStartDate", until)
                parameters.append("MaxEndDate", until)
                parameters.append("EnableImages", "false")
                parameters.append("Fields", "Overview")
                parameters.append("Limit", channelChunk.size.toString())
            }.buildString()

            client.get(url) { jellyfinHeaders() }
                .body<LiveTvProgramsResponse>()
                .Items
        }

        return programs
            .filter { !it.ChannelId.isNullOrBlank() }
            .groupBy { it.ChannelId!! }
            .mapValues { (_, programs) -> programs.minByOrNull { it.StartDate.orEmpty() }!! }
    }

    private suspend fun getChannelsPage(startIndex: Int, limit: Int): LiveTvChannelsResponse {
        val url = URLBuilder().takeFrom("$baseUrl/LiveTv/Channels").apply {
            parameters.append("UserId", userId)
            parameters.append("StartIndex", startIndex.toString())
            parameters.append("Limit", limit.toString())
            parameters.append("AddCurrentProgram", "true")
            parameters.append("EnableImages", "true")
            parameters.append("EnableUserData", "true")
            parameters.append("Fields", "Overview,PrimaryImageAspectRatio,MediaSources")
        }.buildString()

        return client.get(url) {
            jellyfinHeaders()
        }.body()
    }

    suspend fun setFavorite(channelId: String, favorite: Boolean) {
        if (!config.isConfigured()) return
        val path = "$baseUrl/Users/$userId/FavoriteItems/$channelId"
        if (favorite) client.post(path) { jellyfinHeaders() }
        else client.delete(path) { jellyfinHeaders() }
    }

    fun channelImageUrl(channelId: String, maxWidth: Int = 320): String {
        return "$baseUrl/Items/$channelId/Images/Primary?maxWidth=$maxWidth&quality=90"
    }

    fun close() {
        client.close()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jellyfinHeaders() {
        header(HttpHeaders.Authorization, "MediaBrowser Token=\"$accessToken\"")
        header("X-Emby-Token", accessToken)
        header(
            "X-Emby-Authorization",
            "MediaBrowser Client=\"Velora\", Device=\"Android TV\", DeviceId=\"${config.deviceId}\", Version=\"${BuildConfig.VERSION_NAME}\""
        )
    }
}

fun programProgress(program: LiveTvProgram?, nowMillis: Long = System.currentTimeMillis()): Float? {
    if (program == null) return null
    val start = parseJellyfinDate(program.StartDate)?.time ?: return null
    val end = parseJellyfinDate(program.EndDate)?.time ?: return null
    if (end <= start) return null
    return ((nowMillis - start).toDouble() / (end - start).toDouble()).toFloat().coerceIn(0f, 1f)
}

fun formatProgramTimeRange(program: LiveTvProgram?): String? {
    if (program == null) return null
    val start = parseJellyfinDate(program.StartDate) ?: return null
    val end = parseJellyfinDate(program.EndDate) ?: return null
    val output = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${output.format(start)} – ${output.format(end)}"
}

private fun parseJellyfinDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null
    return try {
        val normalized = value.take(19)
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(normalized)
    } catch (_: Exception) {
        null
    }
}
