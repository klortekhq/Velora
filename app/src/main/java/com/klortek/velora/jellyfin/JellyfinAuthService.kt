package com.klortek.velora.jellyfin

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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

enum class AuthenticationFailure {
    NONE,
    INVALID_SERVER,
    INVALID_CREDENTIALS,
    SERVER_ERROR,
    TIMEOUT,
    NETWORK
}

class JellyfinAuthService(
    private val baseUrl: String,
    private val context: Context? = null
) {
    private companion object {
        const val TAG = "JellyfinAuth"
    }

    var lastFailure: AuthenticationFailure = AuthenticationFailure.NONE
        private set

    private val client = HttpClient(Android) {
        // Authentication must fail promptly when a server accepts the socket
        // but never completes the response. Without a request timeout the
        // login screen could remain blocked indefinitely on TV and mobile.
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
    }

    private fun getDeviceId(): String {
        return context?.let(DeviceIdentity::get) ?: "velora-android"
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
        lastFailure = AuthenticationFailure.NONE
        return try {
            val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
            if (!ServerUrlValidator.isValid(normalizedBaseUrl)) {
                lastFailure = AuthenticationFailure.INVALID_SERVER
                return null
            }
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
                lastFailure = when (response.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AuthenticationFailure.INVALID_CREDENTIALS
                    else -> AuthenticationFailure.SERVER_ERROR
                }
                Log.w(TAG, "Authentication failed with HTTP ${response.status.value}")
                null
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            lastFailure = when {
                e is java.net.SocketTimeoutException ||
                    e::class.simpleName == "HttpRequestTimeoutException" ||
                    e::class.simpleName == "ConnectTimeoutException" -> AuthenticationFailure.TIMEOUT
                else -> AuthenticationFailure.NETWORK
            }
            // Do not print exception text or a stack trace: network exceptions can
            // include the configured server URL or request details.
            Log.w(TAG, "Authentication request failed (${e::class.simpleName})")
            null
        }
    }

    /** Release the Ktor engine when a one-shot login attempt is complete. */
    fun close() {
        client.close()
    }
}





