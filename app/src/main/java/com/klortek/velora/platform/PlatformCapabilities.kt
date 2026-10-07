package com.klortek.velora.platform

import com.klortek.velora.BuildConfig

enum class PlatformSurface {
    MOBILE_TABLET,
    TV,
    BROWSER,
    IOS_MOBILE,
    TVOS,
    TIZEN,
    WEBOS,
    VIDAA
}

fun supportsOfflineDownloads(surface: PlatformSurface): Boolean = when (surface) {
    PlatformSurface.MOBILE_TABLET, PlatformSurface.IOS_MOBILE -> true
    PlatformSurface.TV,
    PlatformSurface.BROWSER,
    PlatformSurface.TVOS,
    PlatformSurface.TIZEN,
    PlatformSurface.WEBOS,
    PlatformSurface.VIDAA -> false
}

/** Capabilities exposed to the product UI, kept independent from individual screens. */
object PlatformCapabilities {
    /** TV builds are streaming-only; mobile/tablet builds may offer managed offline media. */
    val supportsOfflineDownloads: Boolean
        get() = supportsOfflineDownloads(
            if (BuildConfig.TV_BUILD) PlatformSurface.TV else PlatformSurface.MOBILE_TABLET
        )
}
