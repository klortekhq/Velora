package com.klortek.velora.livetv

/** Per-playback source choices, transported as a list aligned with channel IDs. */
class LiveTvChannelSourceState(
    channelIds: List<String>,
    mediaSourceIds: List<String?> = emptyList()
) {
    private val sourcesByChannel = channelIds.mapIndexed { index, channelId ->
        channelId to mediaSourceIds.getOrNull(index)?.takeIf { it.isNotBlank() }
    }.toMap().toMutableMap()

    fun mediaSourceIdFor(channelId: String): String? = sourcesByChannel[channelId]

    /** An explicit choice overrides the list's default for this channel only. */
    fun rememberSource(channelId: String, mediaSourceId: String?) {
        sourcesByChannel[channelId] = mediaSourceId?.takeIf { it.isNotBlank() }
    }

    /** Keep null slots so a source can never shift to a different channel. */
    fun mediaSourceIdsFor(channelIds: List<String>): List<String?> =
        channelIds.map(::mediaSourceIdFor)
}
