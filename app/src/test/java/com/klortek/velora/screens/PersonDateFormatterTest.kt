package com.klortek.velora.screens

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonDateFormatterTest {
    @Test
    fun formatsDatesWithTheSelectedLocale() {
        assertEquals("15 ene 1980", formatPersonDate("1980-01-15", Locale("es", "ES")))
        assertEquals("Jan 15, 1980", formatPersonDate("1980-01-15", Locale.US))
    }

    @Test
    fun preservesDatePartWhenJellyfinValueIsInvalid() {
        assertEquals("1980-99-15", formatPersonDate("1980-99-15T00:00:00Z", Locale.US))
    }
}
