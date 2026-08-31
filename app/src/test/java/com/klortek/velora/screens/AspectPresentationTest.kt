package com.klortek.velora.screens

import androidx.media3.ui.AspectRatioFrameLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class AspectPresentationTest {
    @Test
    fun everyModeHasAnExplicitMedia3Presentation() {
        AspectMode.values().forEach { mode ->
            val presentation = aspectPresentation(mode)
            assertEquals(true, presentation.resizeMode in setOf(
                AspectRatioFrameLayout.RESIZE_MODE_FIT,
                AspectRatioFrameLayout.RESIZE_MODE_FILL,
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            ))
            assertEquals(true, presentation.forcedRatio >= 0f)
        }
    }

    @Test
    fun fillCropsAndStretchIsTheOnlyDistortingMode() {
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, aspectPresentation(AspectMode.FILL).resizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, aspectPresentation(AspectMode.STRETCH).resizeMode)
        assertEquals(16f / 9f, aspectPresentation(AspectMode.LETTERBOX).forcedRatio, 0.001f)
    }
}
