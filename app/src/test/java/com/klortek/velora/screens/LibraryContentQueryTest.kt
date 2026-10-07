package com.klortek.velora.screens

import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class LibraryContentQueryTest {
    private val watchedDrama = JellyfinItem(
        Id = "watched",
        Name = "Zeta",
        DateCreated = "2026-01-03T00:00:00Z",
        CommunityRating = 7.2f,
        Genres = listOf("Drama"),
        UserData = UserData(Played = true, IsFavorite = true)
    )
    private val unwatchedComedy = JellyfinItem(
        Id = "unwatched",
        Name = "Alpha",
        DateCreated = "2026-01-02T00:00:00Z",
        CommunityRating = 8.9f,
        Genres = listOf("Comedy"),
        UserData = UserData(Played = false, IsFavorite = false)
    )
    private val unwatchedDrama = JellyfinItem(
        Id = "other",
        Name = "Beta",
        DateCreated = "2026-01-01T00:00:00Z",
        CommunityRating = 8.1f,
        Genres = listOf("Drama"),
        UserData = UserData(Played = false, IsFavorite = true)
    )

    private val items = listOf(watchedDrama, unwatchedComedy, unwatchedDrama)

    @Test
    fun nameSortHonorsDirectionAndUsesStableTieBreakers() {
        assertEquals(
            listOf("Alpha", "Beta", "Zeta"),
            queryLibraryItems(
                items = items,
                sortMode = LibrarySortMode.Name,
                descending = false,
                favoritesOnly = false,
                playbackFilter = LibraryPlaybackFilter.All
            )
                .map { it.Name }
        )
        assertEquals(
            listOf("Zeta", "Beta", "Alpha"),
            queryLibraryItems(
                items = items,
                sortMode = LibrarySortMode.Name,
                descending = true,
                favoritesOnly = false,
                playbackFilter = LibraryPlaybackFilter.All
            )
                .map { it.Name }
        )
    }

    @Test
    fun filtersCanBeCombinedBeforeSorting() {
        val result = queryLibraryItems(
            items = items,
            sortMode = LibrarySortMode.CommunityRating,
            descending = true,
            favoritesOnly = true,
            playbackFilter = LibraryPlaybackFilter.Unwatched,
            genre = "drama"
        )

        assertEquals(listOf("other"), result.map { it.Id })
    }

    @Test
    fun availableGenresAreDistinctCaseInsensitivelyAndSorted() {
        val genres = availableLibraryGenres(
            listOf(
                watchedDrama.copy(Genres = listOf("Drama", " Acción ")),
                unwatchedDrama.copy(Genres = listOf("drama", "Comedy"))
            )
        )

        assertEquals(listOf("Acción", "Comedy", "Drama"), genres)
    }

    @Test
    fun largeLibrariesRemainQueryableAtSupportedStressSizes() {
        listOf(1_000, 10_000, 50_000).forEach { size ->
            val syntheticItems = List(size) { index ->
                JellyfinItem(
                    Id = "item-$index",
                    Name = "Title ${size - index}",
                    DateCreated = "2026-01-${(index % 28) + 1}".padEnd(19, 'T'),
                    CommunityRating = (index % 100) / 10f,
                    Genres = listOf(if (index % 2 == 0) "Drama" else "Comedy"),
                    UserData = UserData(
                        Played = index % 3 == 0,
                        IsFavorite = index % 5 == 0
                    )
                )
            }

            val elapsedMillis = measureTimeMillis {
                val result = queryLibraryItems(
                    items = syntheticItems,
                    sortMode = LibrarySortMode.CommunityRating,
                    descending = true,
                    favoritesOnly = false,
                    playbackFilter = LibraryPlaybackFilter.Unwatched,
                    genre = "drama"
                )
                assertEquals((0 until size).count { it % 2 == 0 && it % 3 != 0 }, result.size)
                assertTrue(result.zipWithNext().all { (left, right) ->
                    (left.CommunityRating ?: -1f) >= (right.CommunityRating ?: -1f)
                })
            }

            // This is a regression guard, not a device benchmark: a pathological
            // query should not turn a paged library interaction into a timeout.
            assertTrue("${size} items took ${elapsedMillis}ms", elapsedMillis < 10_000)
        }
    }
}
