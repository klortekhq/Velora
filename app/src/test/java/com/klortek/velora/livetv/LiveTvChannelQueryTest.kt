package com.klortek.velora.livetv

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTvChannelQueryTest {
    private val channels = listOf(
        LiveTvChannel("1", "Noticias", UserData = LiveTvUserData(true), Tags = listOf("General")),
        LiveTvChannel("2", "Deportes", Tags = listOf("Deportes")),
        LiveTvChannel("3", "Cine", Tags = listOf("General", "Películas"))
    )

    @Test
    fun favoritesAndGroupsAreAppliedWithoutMutatingServerOrder() {
        assertEquals(listOf("Noticias"), filterLiveTvChannels(channels, favoritesOnly = true).map { it.Name })
        assertEquals(listOf("Noticias", "Cine"), filterLiveTvChannels(channels, group = "general").map { it.Name })
        assertEquals(listOf("Deportes", "General", "Películas"), liveTvGroups(channels))
    }
}
