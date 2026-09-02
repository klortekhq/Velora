package com.klortek.velora.jellyfin

import android.content.Context
import android.util.Log
import android.provider.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.klortek.velora.BuildConfig

@Serializable
data class AuthenticationRequest(
    val Username: String,
    val Pw: String
)

@Serializable
data class AuthenticationResponse(
    val AccessToken: String,
    val User: UserInfo
)

@Serializable
data class UserInfo(
    val Id: String,
    val Name: String
)

class JellyfinAuthService(
    private val baseUrl: String,
    private val context: Context? = null
) {
    private companion object {
        const val TAG = "JellyfinAuth"
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
    }

    private fun getDeviceId(): String {
        return try {
            context?.let {
                Settings.Secure.getString(it.contentResolver, Settings.Secure.ANDROID_ID)
            } ?: "android-tv-device"
        } catch (e: Exception) {
            "android-tv-device"
        }
    }

    /**
     * Normalize the base URL for API calls.
     * The URL should already be properly formatted by ServerDiscovery,
     * so we just clean it up (remove trailing slash).
     */
    private fun normalizeBaseUrl(url: String): String {
        return url.trim().removeSuffix("/")
    }

    suspend fun authenticate(username: String, password: String): AuthenticationResponse? {
        return try {
            val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
            val url = if (normalizedBaseUrl.endsWith("/")) {
                "${normalizedBaseUrl}Users/authenticatebyname"
            } else {
                "$normalizedBaseUrl/Users/authenticatebyname"
            }
            
            val deviceId = getDeviceId()
            val deviceName = "Android TV"
            val clientName = "Velora"
            val clientVersion = BuildConfig.VERSION_NAME
            
            val embyAuthHeader = "MediaBrowser Client=\"$clientName\", Device=\"$deviceName\", DeviceId=\"$deviceId\", Version=\"$clientVersion\""
            
            val requestBody = AuthenticationRequest(Username = username, Pw = password)
            
            val response: HttpResponse = client.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header("X-Emby-Authorization", embyAuthHeader)
                setBody(requestBody)
            }
            
            if (response.status == HttpStatusCode.OK) {
                response.body<AuthenticationResponse>()
            } else {
                // Log the error response
                val errorBody = try {
                    response.bodyAsText()
                } catch (e: Exception) {
                    "Could not read error body: ${e.message}"
                }
                Log.w(TAG, "Authentication failed with HTTP ${response.status.value}")
                null
            }
        } catch (e: Exception) {
            // Do not print exception text or a stack trace: network exceptions can
            // include the configured server URL or request details.
            Log.w(TAG, "Authentication request failed (${e::class.simpleName})")
            null
        }
    }
}





