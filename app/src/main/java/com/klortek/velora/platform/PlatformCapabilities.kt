package com.klortek.velora.platform

import com.klortek.velora.BuildConfig

/** Capabilities exposed to the product UI, kept independent from individual screens. */
object PlatformCapabilities {
    /** TV builds are streaming-only; mobile/tablet builds may offer managed offline media. */
    val supportsOfflineDownloads: Boolean
        get() = !BuildConfig.TV_BUILD
}
