package com.klortek.velora.livetv

import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class LiveTvChannelQueryTest {
    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun useUtcForDeterministicGuideAssertions() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreDefaultTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

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

    @Test
    fun programmeTimeAndProgressHandleGuideData() {
        val program = LiveTvProgram(
            Name = "Informativo",
            StartDate = "2026-08-30T10:00:00Z",
            EndDate = "2026-08-30T11:00:00Z"
        )

        assertEquals("10:00 – 11:00", formatProgramTimeRange(program))
        assertEquals(0f, requireNotNull(programProgress(program, 1788084000000L)), 0.01f)
        assertEquals(0.5f, requireNotNull(programProgress(program, 1788085800000L)), 0.01f)
        assertEquals(1f, requireNotNull(programProgress(program, 1788087600000L)), 0.01f)
    }
}
