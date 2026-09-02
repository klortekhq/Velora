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

    @Test
    fun containerUsesTheSameSelectionInPortraitAndFullscreen() {
        val sourceRatio = 2.0f
        assertEquals(4f / 3f, containerAspectRatio(AspectMode.FOUR_THREE, sourceRatio)!!, 0.001f)
        assertEquals(16f / 9f, containerAspectRatio(AspectMode.LETTERBOX, sourceRatio)!!, 0.001f)
        assertEquals(2.39f, containerAspectRatio(AspectMode.CINEMA, sourceRatio)!!, 0.001f)
        assertEquals(sourceRatio, containerAspectRatio(AspectMode.FIT, sourceRatio)!!, 0.001f)
        assertEquals(16f / 9f, containerAspectRatio(AspectMode.FILL, sourceRatio)!!, 0.001f)
        assertEquals(16f / 9f, containerAspectRatio(AspectMode.STRETCH, sourceRatio)!!, 0.001f)
        assertEquals(null, containerAspectRatio(AspectMode.FILL, sourceRatio, fillContainer = true))
        assertEquals(null, containerAspectRatio(AspectMode.STRETCH, sourceRatio, fillContainer = true))
    }

    @Test
    fun invalidSourceRatioFallsBackToSafeContainerRatio() {
        assertEquals(0.1f, containerAspectRatio(AspectMode.ORIGINAL, 0f)!!, 0.001f)
    }
}
