package com.klortek.velora.player.mpv

import com.klortek.velora.player.mpv.AspectMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpvAspectContainerTest {
    @Test
    fun explicitModesConstrainTheFullscreenContainer() {
        assertEquals(4f / 3f, mpvForcedContainerAspectRatio(AspectMode.FOUR_THREE)!!, 0.001f)
        assertEquals(16f / 9f, mpvForcedContainerAspectRatio(AspectMode.LETTERBOX)!!, 0.001f)
        assertEquals(2.39f, mpvForcedContainerAspectRatio(AspectMode.CINEMA)!!, 0.001f)
    }

    @Test
    fun nativeModesKeepTheFullPlayerSurface() {
        listOf(
            AspectMode.FIT,
            AspectMode.FILL,
            AspectMode.STRETCH,
            AspectMode.ORIGINAL
        ).forEach { mode -> assertNull(mpvForcedContainerAspectRatio(mode)) }
    }
}
