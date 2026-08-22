package com.flex.elefin.livetv

import com.flex.elefin.BuildConfig
import com.flex.elefin.jellyfin.JellyfinConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
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
    val ImageTags: Map<String, String>? = null,
    val CurrentProgram: LiveTvProgram? = null
)

@Serializable
data class LiveTvProgram(
    val Id: String? = null,
    val Name: String? = null,
    val Overview: String? = null,
    val StartDate: String? = null,
    val EndDate: String? = null,
    val IsLive: Boolean? = null,
    val IsSports: Boolean? = null,
    val IsNews: Boolean? = null
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

        val url = URLBuilder().takeFrom("$baseUrl/LiveTv/Channels").apply {
            parameters.append("UserId", userId)
            parameters.append("AddCurrentProgram", "true")
            parameters.append("EnableImages", "true")
            parameters.append("EnableUserData", "true")
            parameters.append("Fields", "Overview,PrimaryImageAspectRatio")
        }.buildString()

        // Do not sort, filter, normalize, or merge here. Jellyfin returns the
        // provider's M3U-backed entries in its configured order; the UI must
        // display and play every entry exactly as returned.
        return client.get(url) {
            jellyfinHeaders()
        }.body<LiveTvChannelsResponse>().Items
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

