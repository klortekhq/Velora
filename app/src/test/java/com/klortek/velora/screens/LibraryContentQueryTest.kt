package com.klortek.velora.screens

import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.UserData
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
