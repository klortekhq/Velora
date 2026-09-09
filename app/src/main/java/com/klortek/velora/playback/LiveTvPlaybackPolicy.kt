package com.klortek.velora.playback

import com.klortek.velora.jellyfin.MediaSource

/**
 * Jellyfin 12 remuxes M3U tuner sources through the allocated LiveStreamId.
 * A provider or an older server may still report a direct path, but using it
 * would bypass the server's IPTV normalization and can make playback fail.
 */
internal fun shouldUseLiveTvDirectSource(source: MediaSource?): Boolean {
    return source?.SupportsDirectPlay == true &&
        source.Protocol?.equals("M3U", ignoreCase = true) != true
}
