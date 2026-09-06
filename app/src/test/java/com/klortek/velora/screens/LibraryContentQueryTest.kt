package com.klortek.velora.screens

import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryContentQueryTest {
    private val items = listOf(
        JellyfinItem(
            Id = "a", Name = "Zulu", DateCreated = "2024-01-01", Genres = listOf("Drama"),
            RunTimeTicks = 60L, CommunityRating = 7.2f,
            UserData = UserData(IsFavorite = true, Played = true)
        ),
        JellyfinItem(
            Id = "b", Name = "Alpha", DateCreated = "2025-01-01", Genres = listOf("Comedy"),
            RunTimeTicks = 120L, CommunityRating = 8.4f
        ),
        JellyfinItem(
            Id = "c", Name = "Beta", DateCreated = "2023-01-01", Genres = listOf("Drama", "Comedy"),
            RunTimeTicks = 90L, CommunityRating = 6.1f
        )
    )

    @Test
    fun queryAppliesGenreFavoriteAndPlaybackFiltersBeforeSorting() {
        val result = queryLibraryItems(
            items = items,
            sortMode = LibrarySortMode.Name,
            descending = false,
            favoritesOnly = false,
            playbackFilter = LibraryPlaybackFilter.Unwatched,
            genre = "drama"
        )

        assertEquals(listOf("Beta"), result.map { it.Name })
    }

    @Test
    fun querySupportsAllSortModesAndDirection() {
        assertEquals(listOf("Alpha", "Beta", "Zulu"), queryLibraryItems(items, LibrarySortMode.Name, false, false, LibraryPlaybackFilter.All).map { it.Name })
        assertEquals(listOf("Zulu", "Beta", "Alpha"), queryLibraryItems(items, LibrarySortMode.Name, true, false, LibraryPlaybackFilter.All).map { it.Name })
        assertEquals(listOf("Beta", "Zulu", "Alpha"), queryLibraryItems(items, LibrarySortMode.DateAdded, false, false, LibraryPlaybackFilter.All).map { it.Name })
        assertEquals(listOf("Alpha", "Zulu", "Beta"), queryLibraryItems(items, LibrarySortMode.DateAdded, true, false, LibraryPlaybackFilter.All).map { it.Name })
        assertEquals(listOf("Zulu", "Beta", "Alpha"), queryLibraryItems(items, LibrarySortMode.Runtime, false, false, LibraryPlaybackFilter.All).map { it.Name })
        assertEquals(listOf("Alpha", "Zulu", "Beta"), queryLibraryItems(items, LibrarySortMode.CommunityRating, true, false, LibraryPlaybackFilter.All).map { it.Name })
    }

    @Test
    fun availableGenresAreDistinctCaseInsensitiveAndAlphabetical() {
        val genres = availableLibraryGenres(items + items[0].copy(Id = "d", Genres = listOf(" drama ")))

        assertEquals(listOf("Comedy", "Drama"), genres)
        assertTrue(genres.zipWithNext().all { (a, b) -> a.compareTo(b, ignoreCase = true) < 0 })
    }

    @Test
    fun queryRemainsUsableWithLargeSyntheticLibraries() {
        listOf(1_000, 10_000, 50_000).forEach { size ->
            val synthetic = List(size) { index ->
                JellyfinItem(
                    Id = "synthetic-$index",
                    Name = "Title ${index % 997}",
                    DateCreated = "2025-${(index % 12 + 1).toString().padStart(2, '0')}-01",
                    Genres = if (index % 2 == 0) listOf("Drama") else listOf("Comedy"),
                    CommunityRating = (index % 100) / 10f,
                    UserData = UserData(Played = index % 3 == 0)
                )
            }

            val result = queryLibraryItems(
                items = synthetic,
                sortMode = LibrarySortMode.Name,
                descending = false,
                favoritesOnly = false,
                playbackFilter = LibraryPlaybackFilter.Unwatched,
                genre = "Drama"
            )

            val expected = (0 until size).count { index -> index % 2 == 0 && index % 3 != 0 }
            assertEquals(expected, result.size)
            assertTrue(result.zipWithNext().all { (left, right) ->
                left.Name.lowercase() <= right.Name.lowercase() ||
                    left.Name.equals(right.Name, ignoreCase = true)
            })
        }
    }
}
