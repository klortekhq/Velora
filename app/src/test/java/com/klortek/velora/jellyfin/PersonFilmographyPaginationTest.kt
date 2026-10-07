package com.klortek.velora.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonFilmographyPaginationTest {
    @Test
    fun pageSizeIsClampedToSafeJellyfinBounds() {
        assertEquals(1, personFilmographyPageSize(0))
        assertEquals(100, personFilmographyPageSize(500))
        assertEquals(50, personFilmographyPageSize(50))
    }

    @Test
    fun personResultsAreCappedToProtectLargeFilmographies() {
        assertEquals(500, PERSON_FILMOGRAPHY_MAX_ITEMS)
    }
}
