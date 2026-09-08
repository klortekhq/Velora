package com.klortek.velora.jellyfin

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.klortek.velora.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

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

/** Jellyfin installations use both 400 and 401/403 for rejected credentials. */
internal fun authenticationFailureForHttpStatus(statusCode: Int): AuthenticationFailure =
    when (statusCode) {
        400, 401, 403 -> AuthenticationFailure.INVALID_CREDENTIALS
        else -> AuthenticationFailure.SERVER_ERROR
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

    private val deviceName = if (BuildConfig.TV_BUILD) "Android TV" else "Android"

    // Keep authentication on the same OkHttp transport used by server
    // discovery. This avoids an Android-engine-specific stall observed on
    // some Fire TV firmware while the server was returning an auth response.
    // The bounded call timeout guarantees that a broken endpoint cannot leave
    // the login screen spinning indefinitely.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
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
        return withContext(Dispatchers.IO) {
            try {
                val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
                if (!ServerUrlValidator.isValid(normalizedBaseUrl)) {
                    lastFailure = AuthenticationFailure.INVALID_SERVER
                    return@withContext null
                }
                val url = "$normalizedBaseUrl/Users/authenticatebyname"
                val deviceId = getDeviceId()
                val embyAuthHeader = "MediaBrowser Client=\"Velora\", Device=\"$deviceName\", DeviceId=\"$deviceId\", Version=\"${BuildConfig.VERSION_NAME}\""
                val body = json.encodeToString(AuthenticationRequest(Username = username, Pw = password))
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("X-Emby-Authorization", embyAuthHeader)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 200) {
                        val responseBody = response.body?.string().orEmpty()
                        json.decodeFromString<AuthenticationResponse>(responseBody)
                    } else {
                        lastFailure = authenticationFailureForHttpStatus(response.code)
                        Log.w(TAG, "Authentication failed with HTTP ${response.code}")
                        null
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                lastFailure = when {
                    e is java.net.SocketTimeoutException ||
                        e::class.simpleName == "InterruptedIOException" -> AuthenticationFailure.TIMEOUT
                    else -> AuthenticationFailure.NETWORK
                }
                // Do not print exception text or a stack trace: network exceptions can
                // include the configured server URL or request details.
                Log.w(TAG, "Authentication request failed (${e::class.simpleName})")
                null
            }
        }
    }

    /** Release resources when a one-shot login attempt is complete. */
    fun close() {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}





