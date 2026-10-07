package com.klortek.velora.livetv

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTvClientPagingTest {
    @Test
    fun channelPagingStopsAtServerTotalWithoutDroppingFinalPartialPage() {
        val pages = listOf(
            listOf("1", "2"),
            listOf("3", "4"),
            listOf("5")
        )

        val flattened = pages.flatten()

        assertEquals(listOf("1", "2", "3", "4", "5"), flattened)
        assertEquals(5, flattened.size)
    }

    @Test
    fun programmeRequestsCanBeChunkedWithoutChangingChannelOrder() {
        val channelIds = (1..1001).map(Int::toString)
        val chunks = channelIds.chunked(500)

        assertEquals(listOf(500, 500, 1), chunks.map { it.size })
        assertEquals(channelIds, chunks.flatten())
    }
}
