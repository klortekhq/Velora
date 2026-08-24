package com.klortek.velora.continuewatching

import android.content.Context
import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.JellyfinConfig

/**
 * Local-only Continue Watching dismissals.
 *
 * We intentionally do not modify Jellyfin's Played state or playback position.
 * A dismissal is tied to the current playback fingerprint. If the item is played
 * again and Jellyfin reports a different position/LastPlayedDate, it automatically
 * becomes visible again.
 */
object ContinueWatchingDismissStore {
    private const val PREFS_NAME = "velora_continue_watching_dismissed_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun itemKey(context: Context, itemId: String): String {
        val config = JellyfinConfig(context)
        return "${config.serverUrl}|${config.userId}|$itemId"
    }

    private fun fingerprint(item: JellyfinItem): String {
        val position = item.UserData?.PositionTicks ?: 0L
        val lastPlayed = item.UserData?.LastPlayedDate.orEmpty()
        return "$lastPlayed|$position"
    }

    fun dismiss(context: Context, item: JellyfinItem) {
        prefs(context).edit().putString(itemKey(context, item.Id), fingerprint(item)).apply()
    }

    fun restore(context: Context, itemId: String) {
        prefs(context).edit().remove(itemKey(context, itemId)).apply()
    }

    fun isDismissed(context: Context, item: JellyfinItem): Boolean {
        val store = prefs(context)
        val key = itemKey(context, item.Id)
        val saved = store.getString(key, null) ?: return false
        val current = fingerprint(item)
        if (saved == current) return true

        // Playback changed since dismissal: show it again automatically.
        store.edit().remove(key).apply()
        return false
    }

    fun filterVisible(context: Context, items: List<JellyfinItem>): List<JellyfinItem> =
        items.filterNot { isDismissed(context, it) }
}
