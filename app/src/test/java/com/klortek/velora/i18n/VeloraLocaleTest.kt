package com.klortek.velora.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VeloraLocaleTest {
    @Test
    fun languageCatalogHasUniqueSupportedTagsAndAnAutomaticOption() {
        val tags = VeloraLocale.languages.map { it.tag }

        assertEquals(tags.size, tags.toSet().size)
        assertEquals(VeloraLocale.AUTO, tags.first())
        assertTrue(tags.containsAll(listOf("es", "en", "fr", "de")))
    }

    @Test
    fun subtitleModesAreStablePreferenceValues() {
        assertEquals("off", VeloraLocale.SUBTITLES_OFF)
        assertEquals("preferred", VeloraLocale.SUBTITLES_PREFERRED)
        assertEquals("forced", VeloraLocale.SUBTITLES_FORCED)
        assertEquals("auto", VeloraLocale.SUBTITLES_AUTO)
    }
}
