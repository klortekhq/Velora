package com.klortek.velora.livetv

/** Pure, deterministic filtering used by both touch and remote Live TV UIs. */
fun filterLiveTvChannels(
    channels: List<LiveTvChannel>,
    favoritesOnly: Boolean = false,
    group: String? = null
): List<LiveTvChannel> = channels.filter { channel ->
    (!favoritesOnly || channel.UserData?.IsFavorite == true) &&
        (group == null || liveTvChannelMatchesGroup(channel, group))
}

fun liveTvGroups(channels: List<LiveTvChannel>): List<String> = channels
    .flatMap { channel ->
        channel.Tags.orEmpty() + listOfNotNull(channel.ChannelType, channel.ServiceName)
    }
    .filter { it.isNotBlank() }
    .distinctBy { it.lowercase() }
    .sortedWith(String.CASE_INSENSITIVE_ORDER)

/**
 * Filters already-grouped channels without discarding alternate sources.
 * Jellyfin can attach favourite/group metadata to only one provider row; the
 * visible channel must remain a single row with every selectable source.
 */
fun filterLiveTvChannelGroups(
    groups: List<LiveTvChannelGroup>,
    favoritesOnly: Boolean = false,
    group: String? = null
): List<LiveTvChannelGroup> = groups.filter { channelGroup ->
        (!favoritesOnly || channelGroup.channels.any { it.UserData?.IsFavorite == true }) &&
        (group == null || channelGroup.channels.any { channel ->
            liveTvChannelMatchesGroup(channel, group)
        })
}

private fun liveTvChannelMatchesGroup(channel: LiveTvChannel, group: String): Boolean =
    channel.Tags.orEmpty().any { it.equals(group, ignoreCase = true) } ||
        channel.ChannelType?.equals(group, ignoreCase = true) == true ||
        channel.ServiceName?.equals(group, ignoreCase = true) == true

/** A single visible channel with all Jellyfin entries that share its identity. */
data class LiveTvChannelGroup(
    val channelId: String,
    val channels: List<LiveTvChannel>,
) {
    /** Providers may attach guide, favourite or artwork metadata to only one
     * of several source rows representing the same visible channel. */
    val primary: LiveTvChannel
        get() = channels.maxByOrNull { channel ->
            (if (channel.CurrentProgram != null) 4 else 0) +
                (if (channel.UserData?.IsFavorite == true) 2 else 0) +
                (if (!channel.ImageTags.isNullOrEmpty()) 1 else 0) +
                (if (!channel.ChannelNumber.isNullOrBlank()) 1 else 0)
        } ?: channels.first()
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
    return groups.map { (key, entries) ->
        // Some Jellyfin providers repeat the same channel row without a
        // MediaSource. Do not turn that transport duplicate into a fake
        // selectable option. Distinct source IDs (for example principal and
        // IPTV) remain separate and therefore selectable.
        val uniqueEntries = entries.distinctBy { channel ->
            val sourceId = liveTvMediaSourceId(channel)
            sourceId ?: listOf(
                channel.Type.orEmpty(),
                channel.ChannelNumber.orEmpty(),
                channel.Name.trim().lowercase(),
                channel.Tags.orEmpty().sorted().joinToString("|")
            ).joinToString("|")
        }
        LiveTvChannelGroup(key, uniqueEntries)
    }
}

/** Human-readable source label for the picker, never exposing URLs or tokens. */
fun liveTvSourceLabel(
    channel: LiveTvChannel,
    optionNumber: Int,
    fallbackLabel: String = "Opción $optionNumber"
): String =
    channel.Tags.orEmpty().firstOrNull { it.isNotBlank() }
        ?: channel.ChannelType?.takeIf { it.isNotBlank() }
        ?: channel.ServiceName?.takeIf { it.isNotBlank() }
        ?: channel.MediaSources.orEmpty().firstOrNull()?.Name?.takeIf { it.isNotBlank() }
        ?: channel.Type?.takeIf { it.isNotBlank() && !it.equals("TvChannel", ignoreCase = true) }
        ?: channel.ChannelNumber?.takeIf { it.isNotBlank() }?.let { "Canal $it" }
        ?: fallbackLabel

fun liveTvMediaSourceId(channel: LiveTvChannel): String? =
    channel.MediaSources.orEmpty().firstOrNull()?.Id?.takeIf { it.isNotBlank() }
