package com.klortek.velora.playback

/**
 * Decides whether the optional GL post-processing surface is safe to use.
 *
 * GL is an enhancement, never the playback path. If Jellyfin does not expose
 * a reliable video codec before playback starts, stay on Media3's standard
 * SurfaceView: an unknown codec must not be routed through a surface that can
 * black-screen software-decoded AV1 on TV hardware.
 */
object VideoRenderPolicy {
    fun shouldUseGlEnhancements(requested: Boolean, declaredVideoCodec: String?): Boolean {
        if (!requested) return false
        val codec = declaredVideoCodec?.trim()?.lowercase().orEmpty()
        if (codec.isEmpty()) return false
        return codec != "av1" && codec != "av01"
    }
}
