package com.klortek.velora.jellyfin

import android.content.Context
import java.util.UUID

/** Anonymous, app-scoped Jellyfin device identity. */
object DeviceIdentity {
    private const val PREFERENCES = "velora_device_identity"
    private const val KEY_ID = "device_id"

    fun get(context: Context): String {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE
        )
        preferences.getString(KEY_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }

        val generated = "velora-" + UUID.randomUUID().toString().replace("-", "")
        preferences.edit().putString(KEY_ID, generated).apply()
        return generated
    }
}
