package com.klortek.velora.livetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveTvChannelSourceStateTest {
    @Test
    fun selectedSourceSurvivesZappingAwayAndBackAcrossTransportedStates() {
        val channelIds = listOf("A", "B")
        val initial = LiveTvChannelSourceState(channelIds, listOf("S1", "B-source"))
        initial.rememberSource("A", "S2")

        val nextId = adjacentLiveTvChannelId(channelIds, "A", next = true)!!
        val atB = LiveTvChannelSourceState(channelIds, initial.mediaSourceIdsFor(channelIds))
        assertEquals("B", nextId)
        assertEquals("B-source", atB.mediaSourceIdFor(nextId))

        val previousId = adjacentLiveTvChannelId(channelIds, nextId, next = false)!!
        val backAtA = LiveTvChannelSourceState(channelIds, atB.mediaSourceIdsFor(channelIds))
        assertEquals("A", previousId)
        assertEquals("S2", backAtA.mediaSourceIdFor(previousId))
        assertEquals(listOf("S2", "B-source"), backAtA.mediaSourceIdsFor(channelIds))
    }

    @Test
    fun channelWithoutSourceStaysNullAndDoesNotInheritPreviousChannelsSource() {
        val channelIds = listOf("A", "B", "C")
        val initial = LiveTvChannelSourceState(channelIds, listOf("S2", null, "C-source"))
        val nextId = adjacentLiveTvChannelId(channelIds, "A", next = true)!!
        val atB = LiveTvChannelSourceState(channelIds, initial.mediaSourceIdsFor(channelIds))

        assertEquals("B", nextId)
        assertNull(atB.mediaSourceIdFor(nextId))
        assertEquals(listOf("S2", null, "C-source"), atB.mediaSourceIdsFor(channelIds))

        val restored = LiveTvChannelSourceState(channelIds, atB.mediaSourceIdsFor(channelIds))
        assertEquals("S2", restored.mediaSourceIdFor("A"))
        assertNull(restored.mediaSourceIdFor("B"))
        assertEquals("C-source", restored.mediaSourceIdFor("C"))
    }

    @Test
    fun transportedSourcesFollowChannelIdsWhenTheListIsReordered() {
        val original = LiveTvChannelSourceState(listOf("A", "B", "C"), listOf("S2", null, "C-source"))
        val reorderedIds = listOf("C", "A", "B", "unknown")
        val transportedSources = original.mediaSourceIdsFor(reorderedIds)

        assertEquals(listOf("C-source", "S2", null, null), transportedSources)
        val restored = LiveTvChannelSourceState(reorderedIds, transportedSources)
        assertEquals("S2", restored.mediaSourceIdFor("A"))
        assertEquals("C-source", restored.mediaSourceIdFor("C"))
        assertNull(restored.mediaSourceIdFor("B"))
        assertNull(restored.mediaSourceIdFor("unknown"))
    }

    @Test
    fun missingOrShortSourceListsDoNotInventSourcesForOtherChannels() {
        val channelIds = listOf("A", "B")
        val legacy = LiveTvChannelSourceState(channelIds)
        legacy.rememberSource("A", "S2")
        val restored = LiveTvChannelSourceState(channelIds, legacy.mediaSourceIdsFor(channelIds))

        assertEquals("S2", restored.mediaSourceIdFor("A"))
        assertNull(restored.mediaSourceIdFor("B"))

        val shortList = LiveTvChannelSourceState(channelIds, listOf("S2"))
        assertEquals(listOf("S2", null), shortList.mediaSourceIdsFor(channelIds))
        assertNull(shortList.mediaSourceIdFor("unknown"))
    }

    @Test
    fun clearingASourceAndBlankIdsRemainNullAfterTransport() {
        val channelIds = listOf("A", "B", "C")
        val state = LiveTvChannelSourceState(channelIds, listOf("S2", " ", "C-source"))
        state.rememberSource("A", null)
        state.rememberSource("C", "")

        val restored = LiveTvChannelSourceState(channelIds, state.mediaSourceIdsFor(channelIds))
        assertEquals(listOf<String?>(null, null, null), restored.mediaSourceIdsFor(channelIds))
    }

    @Test
    fun separatePlaybackSessionsAndTransportSnapshotsDoNotShareChanges() {
        val channelIds = listOf("A", "B")
        val first = LiveTvChannelSourceState(channelIds, listOf("S1", null))
        val snapshot = first.mediaSourceIdsFor(channelIds)
        val second = LiveTvChannelSourceState(channelIds, snapshot)
        first.rememberSource("A", "S2")
        second.rememberSource("B", "B-source")

        assertEquals(listOf("S1", null), snapshot)
        assertEquals("S1", second.mediaSourceIdFor("A"))
        assertEquals("S2", first.mediaSourceIdFor("A"))
        assertNull(first.mediaSourceIdFor("B"))
    }
}
