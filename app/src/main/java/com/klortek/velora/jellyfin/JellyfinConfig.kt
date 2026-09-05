package com.klortek.velora.jellyfin

import android.content.Context
import android.content.SharedPreferences

class JellyfinConfig(context: Context) {
    private val secureCredentials = SecureCredentialStore(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "jellyfin_config",
        Context.MODE_PRIVATE
    )

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) {
            // Store the URL as-is - ServerDiscovery handles normalization
            // Remove trailing slash for consistency
            val cleanUrl = value.trim().removeSuffix("/")
            prefs.edit().putString("server_url", cleanUrl).apply()
        }

    var accessToken: String
        get() = readSecret("access_token")
        set(value) {
            secureCredentials.write("access_token", value)
            prefs.edit().remove("access_token").apply()
        }

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        set(value) = prefs.edit().putString("user_id", value).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String
        get() = readSecret("password")
        set(value) {
            secureCredentials.write("password", value)
            prefs.edit().remove("password").apply()
        }
    
    var deviceId: String
        get() = prefs.getString("device_id", "") ?: ""
        set(value) = prefs.edit().putString("device_id", value).apply()

    fun isConfigured(): Boolean {
        val url = serverUrl
        return ServerUrlValidator.isValid(url) && accessToken.isNotEmpty() && userId.isNotEmpty()
    }

    fun hasCredentials(): Boolean {
        return serverUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
    }

    fun clearAuth(): Unit {
        prefs.edit().apply {
            remove("access_token")
            remove("user_id")
            remove("username")
            remove("password")
            apply()
        }
        secureCredentials.remove("access_token")
        secureCredentials.remove("password")
    }

    /**
     * One-time migration from the pre-Keystore plaintext preferences. The
     * legacy value is removed synchronously after it has been encrypted so a
     * process death cannot leave the old copy behind after a successful read.
     */
    private fun readSecret(name: String): String {
        secureCredentials.read(name)?.let { encrypted ->
            // Clean up any stale legacy copy left by an interrupted migration.
            if (prefs.contains(name)) prefs.edit().remove(name).commit()
            return encrypted
        }

        val legacy = prefs.getString(name, null).orEmpty()
        if (legacy.isBlank()) return ""
        secureCredentials.write(name, legacy)
        prefs.edit().remove(name).commit()
        return legacy
    }
}






