package com.klortek.velora.livetv

import com.klortek.velora.jellyfin.MediaSource

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
        get() = channels.maxByOrNull(::liveTvChannelPrimaryScore) ?: channels.first()
}

private fun liveTvChannelPrimaryScore(channel: LiveTvChannel): Int =
    (if (channel.CurrentProgram != null) 4 else 0) +
        (if (channel.UserData?.IsFavorite == true) 2 else 0) +
        (if (!channel.ImageTags.isNullOrEmpty()) 1 else 0) +
        (if (!channel.ChannelNumber.isNullOrBlank()) 1 else 0)

/**
 * Collapses duplicate provider entries into one channel row.
 *
 * Jellyfin normally gives every provider row a stable Id, but IPTV/tuner
 * integrations can emit different Ids for the same visible channel. In that
 * case the user-facing identity is the channel number plus normalized name;
 * the provider/source metadata remains inside the selectable options. When
 * that metadata is missing, the Jellyfin Id remains the safe fallback.
 */
fun groupLiveTvChannels(channels: List<LiveTvChannel>): List<LiveTvChannelGroup> {
    val groups = linkedMapOf<String, MutableList<LiveTvChannel>>()
    val aliases = mutableMapOf<String, String>()
    channels.forEach { channel ->
        val identityName = channel.Name.trim().lowercase().replace(Regex("\\s+"), " ")
        val identityNumber = channel.ChannelNumber?.trim().orEmpty()
        val visibleIdentity = when {
            identityName.isNotBlank() && identityNumber.isNotBlank() ->
                "visible:$identityNumber|$identityName"
            identityName.isNotBlank() -> "visible:$identityName"
            else -> null
        }
        val idIdentity = channel.Id.takeIf { it.isNotBlank() }
        val identities = listOfNotNull(idIdentity, visibleIdentity)
        val matchedGroups = identities.mapNotNull { aliases[it] }.distinct()
        val key = matchedGroups.firstOrNull() ?: identities.firstOrNull() ?: "fallback:${groups.size}"

        // A source may arrive with an ID already seen through another visible
        // identity. Merge those aliases so both the ID and name/number keep
        // pointing to the same user-facing row.
        matchedGroups.drop(1).forEach { otherKey ->
            if (otherKey != key) {
                groups.remove(otherKey)?.let { groups.getOrPut(key) { mutableListOf() }.addAll(it) }
                aliases.entries.filter { it.value == otherKey }.forEach { aliases[it.key] = key }
            }
        }
        identities.forEach { aliases[it] = key }
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
        val uniqueEntries = entries.fold(linkedMapOf<String, LiveTvChannel>()) { unique, channel ->
            val sourceId = liveTvMediaSourceId(channel)
            val source = channel.MediaSources.orEmpty().firstOrNull()
            val sourceKey = sourceId ?: listOf(
                channel.Type.orEmpty(),
                channel.ChannelNumber.orEmpty(),
                channel.Name.trim().lowercase(),
                channel.Tags.orEmpty().sorted().joinToString("|"),
                source?.Name.orEmpty().trim().lowercase(),
                source?.LiveStreamId.orEmpty(),
                source?.Protocol.orEmpty().lowercase()
            ).joinToString("|")
            val existing = unique[sourceKey]
            if (existing == null || liveTvChannelPrimaryScore(channel) > liveTvChannelPrimaryScore(existing)) {
                unique[sourceKey] = channel
            }
            unique
        }.values.toList()
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
    channel.MediaSources.orEmpty().firstOrNull()?.let { source ->
        source.Id?.takeIf { it.isNotBlank() }
            ?: source.LiveStreamId?.takeIf { it.isNotBlank() }
    }

/** Keep the source selected in the Live TV picker after PlaybackInfo returns. */
fun selectLiveTvPlaybackSource(
    sources: List<MediaSource>,
    requestedId: String?
): MediaSource? {
    if (requestedId.isNullOrBlank()) return sources.firstOrNull()
    return sources.firstOrNull { source ->
        source.Id == requestedId || source.LiveStreamId == requestedId
    } ?: sources.firstOrNull()
}
