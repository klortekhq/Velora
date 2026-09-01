package com.klortek.velora.livetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveTvChannelNavigationTest {
    private val channels = listOf("one", "two", "three")

    @Test
    fun nextAndPreviousPreserveServerOrderAndWrap() {
        assertEquals("two", adjacentLiveTvChannelId(channels, "one", next = true))
        assertEquals("one", adjacentLiveTvChannelId(channels, "two", next = false))
        assertEquals("one", adjacentLiveTvChannelId(channels, "three", next = true))
        assertEquals("three", adjacentLiveTvChannelId(channels, "one", next = false))
    }

    @Test
    fun unknownOrEmptyChannelSetDoesNotInventAChannel() {
        assertNull(adjacentLiveTvChannelId(emptyList(), "one", next = true))
        assertNull(adjacentLiveTvChannelId(channels, "missing", next = true))
    }
}
