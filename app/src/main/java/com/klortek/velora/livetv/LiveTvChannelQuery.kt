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
