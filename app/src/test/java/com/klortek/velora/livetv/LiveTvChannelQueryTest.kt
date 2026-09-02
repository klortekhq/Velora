package com.klortek.velora.livetv

import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.TimeZone
import com.klortek.velora.jellyfin.MediaSource

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

    @Test
    fun channelsWithTheSameJellyfinIdBecomeOneRowAndKeepEverySource() {
        val main = LiveTvChannel("same-id", "DAZN F1", Type = "Principal", MediaSources = listOf(MediaSource(Id = "source-main")))
        val iptv = LiveTvChannel("same-id", "DAZN F1", Type = "IPTV", MediaSources = listOf(MediaSource(Id = "source-iptv")))
        val other = LiveTvChannel("other-id", "DAZN 2")

        val groups = groupLiveTvChannels(listOf(main, iptv, other))

        assertEquals(listOf("same-id", "other-id"), groups.map { it.channelId })
        assertEquals(listOf("Principal", "IPTV"), groups.first().channels.map { liveTvSourceLabel(it, 1) })
        assertEquals(2, groups.first().channels.size)
        assertEquals("source-iptv", liveTvMediaSourceId(groups.first().channels[1]))
    }

    @Test
    fun groupedChannelCountRepresentsVisibleRows() {
        val duplicate = LiveTvChannel("same-id", "DAZN F1")
        assertEquals(1, groupLiveTvChannels(listOf(duplicate, duplicate)).size)
    }
}
