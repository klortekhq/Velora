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
        val groups = groupLiveTvChannels(listOf(duplicate, duplicate))
        assertEquals(1, groups.size)
        assertEquals(1, groups.single().channels.size)
    }

    @Test
    fun oneChannelWithSeveralMediaSourcesAlsoBecomesSelectableOptions() {
        val channel = LiveTvChannel(
            "single-row",
            "DAZN F1",
            MediaSources = listOf(
                MediaSource(Id = "source-main"),
                MediaSource(Id = "source-iptv")
            )
        )

        val group = groupLiveTvChannels(listOf(channel)).single()

        assertEquals(2, group.channels.size)
        assertEquals(
            listOf("source-main", "source-iptv"),
            group.channels.map(::liveTvMediaSourceId)
        )
    }

    @Test
    fun playbackInfoKeepsTheSelectedSourceInsteadOfAlwaysUsingTheFirst() {
        val primary = MediaSource(Id = "source-main", LiveStreamId = "stream-main")
        val iptv = MediaSource(Id = "source-iptv", LiveStreamId = "stream-iptv")

        assertEquals(
            "source-iptv",
            selectLiveTvPlaybackSource(listOf(primary, iptv), "source-iptv")?.Id
        )
        assertEquals(
            "source-iptv",
            selectLiveTvPlaybackSource(listOf(primary, iptv), "stream-iptv")?.Id
        )
        assertEquals(
            "source-main",
            selectLiveTvPlaybackSource(listOf(primary, iptv), "missing")?.Id
        )
    }

    @Test
    fun unnamedSourceIdsStillRemainDistinctWhenProviderSuppliesDescriptors() {
        val channel = LiveTvChannel(
            "descriptor-only",
            "DAZN F1",
            MediaSources = listOf(
                MediaSource(Name = "Principal", LiveStreamId = "stream-main", Protocol = "hls"),
                MediaSource(Name = "IPTV", LiveStreamId = "stream-iptv", Protocol = "hls")
            )
        )

        val group = groupLiveTvChannels(listOf(channel)).single()

        assertEquals(2, group.channels.size)
        assertEquals(listOf("Principal", "IPTV"), group.channels.map { it.MediaSources?.single()?.Name })
    }

    @Test
    fun liveStreamIdIsUsedWhenProviderOmitsMediaSourceId() {
        val channel = LiveTvChannel(
            "stream-only",
            "Canal IPTV",
            MediaSources = listOf(MediaSource(Name = "IPTV", LiveStreamId = "stream-iptv"))
        )

        assertEquals("stream-iptv", liveTvMediaSourceId(channel))
    }

    @Test
    fun sourceLabelAcceptsLocalizedFallbackWhenProviderHasNoLabel() {
        val channel = LiveTvChannel(
            "unlabelled",
            "Canal",
            MediaSources = listOf(MediaSource(Id = "internal-source-id"))
        )

        assertEquals("Option 2", liveTvSourceLabel(channel, 2, "Option 2"))
    }

    @Test
    fun sourceLabelPrefersProviderClassificationOverTechnicalType() {
        val channel = LiveTvChannel(
            "iptv",
            "DAZN F1",
            Type = "TvChannel",
            ChannelType = "IPTV",
            ServiceName = "Lista local",
            MediaSources = listOf(MediaSource(Id = "source-iptv", Name = "Fuente IPTV"))
        )

        assertEquals("IPTV", liveTvSourceLabel(channel, 1))
    }

    @Test
    fun channelTypeAndServiceNameAreAvailableAsLiveTvGroups() {
        val channel = LiveTvChannel(
            "iptv-group",
            "DAZN F1",
            ChannelType = "IPTV",
            ServiceName = "Lista local"
        )

        assertEquals(listOf("IPTV", "Lista local"), liveTvGroups(listOf(channel)))
        assertEquals(
            listOf("DAZN F1"),
            filterLiveTvChannels(listOf(channel), group = "lista LOCAL").map { it.Name }
        )
    }

    @Test
    fun primaryRowKeepsGuideAndUserMetadataFromTheBestDuplicate() {
        val stale = LiveTvChannel("same-id", "Canal", MediaSources = listOf(MediaSource(Id = "source-main")))
        val current = LiveTvChannel(
            "same-id",
            "Canal IPTV",
            UserData = LiveTvUserData(IsFavorite = true),
            CurrentProgram = LiveTvProgram(Name = "Ahora"),
            MediaSources = listOf(MediaSource(Id = "source-iptv"))
        )

        val group = groupLiveTvChannels(listOf(stale, current)).single()

        assertEquals("Canal IPTV", group.primary.Name)
        assertEquals("Ahora", group.primary.CurrentProgram?.Name)
        assertEquals(2, group.channels.size)
    }

    @Test
    fun duplicateRowsWithTheSameSourceKeepTheRichestMetadata() {
        val stale = LiveTvChannel(
            "same-id",
            "Canal",
            MediaSources = listOf(MediaSource(Id = "source-main"))
        )
        val enriched = LiveTvChannel(
            "same-id",
            "Canal",
            UserData = LiveTvUserData(IsFavorite = true),
            CurrentProgram = LiveTvProgram(Name = "Ahora"),
            MediaSources = listOf(MediaSource(Id = "source-main"))
        )

        val group = groupLiveTvChannels(listOf(stale, enriched)).single()

        assertEquals(1, group.channels.size)
        assertEquals("Ahora", group.primary.CurrentProgram?.Name)
        assertEquals(true, group.primary.UserData?.IsFavorite)
    }

    @Test
    fun filteringAGroupKeepsAlternateSourcesWhenMetadataIsOnOneRow() {
        val main = LiveTvChannel(
            "same-id",
            "DAZN F1",
            Tags = listOf("Deportes"),
            MediaSources = listOf(MediaSource(Id = "source-main"))
        )
        val iptv = LiveTvChannel(
            "same-id",
            "DAZN F1",
            UserData = LiveTvUserData(IsFavorite = true),
            MediaSources = listOf(MediaSource(Id = "source-iptv"))
        )
        val grouped = groupLiveTvChannels(listOf(main, iptv))

        val favorites = filterLiveTvChannelGroups(grouped, favoritesOnly = true)
        val sports = filterLiveTvChannelGroups(grouped, group = "deportes")

        assertEquals(2, favorites.single().channels.size)
        assertEquals(
            listOf("source-main", "source-iptv"),
            favorites.single().channels.map(::liveTvMediaSourceId)
        )
        assertEquals(2, sports.single().channels.size)
    }
}
