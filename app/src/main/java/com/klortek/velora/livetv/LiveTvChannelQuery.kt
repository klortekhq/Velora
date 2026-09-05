package com.klortek.velora.livetv

/** Pure, deterministic filtering used by both touch and remote Live TV UIs. */
fun filterLiveTvChannels(
    channels: List<LiveTvChannel>,
    favoritesOnly: Boolean = false,
    group: String? = null
): List<LiveTvChannel> = channels.filter { channel ->
    (!favoritesOnly || channel.UserData?.IsFavorite == true) &&
        (group == null || channel.Tags.orEmpty().any { it.equals(group, ignoreCase = true) })
}

fun liveTvGroups(channels: List<LiveTvChannel>): List<String> = channels
    .flatMap { it.Tags.orEmpty() }
    .filter { it.isNotBlank() }
    .distinctBy { it.lowercase() }
    .sortedWith(String.CASE_INSENSITIVE_ORDER)

/** A single visible channel with all Jellyfin entries that share its identity. */
data class LiveTvChannelGroup(
    val channelId: String,
    val channels: List<LiveTvChannel>,
) {
    val primary: LiveTvChannel get() = channels.first()
}

/**
 * Collapses duplicate provider entries into one channel row. Jellyfin's Id is
 * the authoritative identity; the fallback only protects malformed provider
 * data where Id is empty, without merging unrelated numbered channels.
 */
fun groupLiveTvChannels(channels: List<LiveTvChannel>): List<LiveTvChannelGroup> {
    val groups = linkedMapOf<String, MutableList<LiveTvChannel>>()
    channels.forEach { channel ->
        val key = channel.Id.ifBlank {
            "fallback:${channel.ChannelNumber.orEmpty()}|${channel.Name.trim().lowercase()}"
        }
        val sources = channel.MediaSources.orEmpty()
        if (sources.size <= 1) {
            groups.getOrPut(key) { mutableListOf() }.add(channel)
        } else {
            // Jellyfin can expose alternatives either as duplicate channel rows
            // or as several MediaSources on one row. Normalize the latter to
            // the same picker model, retaining only the selected source in each
            // option so PlaybackInfo receives the correct MediaSourceId.
            groups.getOrPut(key) { mutableListOf() }.addAll(
                sources.map { source -> channel.copy(MediaSources = listOf(source)) }
            )
        }
    }
    return groups.map { (key, entries) -> LiveTvChannelGroup(key, entries.toList()) }
}

/** Human-readable source label for the picker, never exposing URLs or tokens. */
fun liveTvSourceLabel(
    channel: LiveTvChannel,
    optionNumber: Int,
    fallbackLabel: String = "Opción $optionNumber"
): String =
    channel.Tags.orEmpty().firstOrNull { it.isNotBlank() }
        ?: channel.Type?.takeIf { it.isNotBlank() }
        ?: channel.MediaSources.orEmpty().firstOrNull()?.Id?.takeIf { it.isNotBlank() }?.let { "Fuente $optionNumber" }
        ?: channel.ChannelNumber?.takeIf { it.isNotBlank() }?.let { "Canal $it" }
        ?: fallbackLabel

fun liveTvMediaSourceId(channel: LiveTvChannel): String? =
    channel.MediaSources.orEmpty().firstOrNull()?.Id?.takeIf { it.isNotBlank() }
