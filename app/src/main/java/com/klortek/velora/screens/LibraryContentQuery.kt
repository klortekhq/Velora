package com.klortek.velora.screens

import com.klortek.velora.jellyfin.JellyfinItem

/**
 * The deterministic query used by both movie and series library surfaces.
 * Keeping this outside Compose makes sorting/filtering testable and prevents
 * the touch and TV layouts from drifting apart.
 */
enum class LibrarySortMode {
    Name,
    DateAdded,
    PremiereDate,
    Runtime,
    CriticRating,
    CommunityRating,
    Random
}

enum class LibraryPlaybackFilter { All, Watched, Unwatched }

fun SortType.toLibrarySortMode(): LibrarySortMode = when (this) {
    SortType.Alphabetically -> LibrarySortMode.Name
    SortType.DateAdded -> LibrarySortMode.DateAdded
    SortType.DateReleased -> LibrarySortMode.PremiereDate
    SortType.Runtime -> LibrarySortMode.Runtime
    SortType.CriticRating -> LibrarySortMode.CriticRating
    SortType.CommunityRating -> LibrarySortMode.CommunityRating
    SortType.Random -> LibrarySortMode.Random
}

fun SortType.isDescendingLibrarySort(): Boolean = this in setOf(
    SortType.DateAdded,
    SortType.DateReleased,
    SortType.CriticRating,
    SortType.CommunityRating
)

fun PlaybackFilter.toLibraryPlaybackFilter(): LibraryPlaybackFilter = when (this) {
    PlaybackFilter.Watched -> LibraryPlaybackFilter.Watched
    PlaybackFilter.Unwatched -> LibraryPlaybackFilter.Unwatched
    PlaybackFilter.All, PlaybackFilter.Favorites -> LibraryPlaybackFilter.All
}

fun queryLibraryItems(
    items: List<JellyfinItem>,
    sortMode: LibrarySortMode,
    descending: Boolean,
    favoritesOnly: Boolean,
    playbackFilter: LibraryPlaybackFilter,
    genre: String? = null
): List<JellyfinItem> {
    val filtered = items.asSequence()
        .filter { item -> !favoritesOnly || item.UserData?.IsFavorite == true }
        .filter { item ->
            when (playbackFilter) {
                LibraryPlaybackFilter.All -> true
                LibraryPlaybackFilter.Watched -> item.UserData?.Played == true
                LibraryPlaybackFilter.Unwatched -> item.UserData?.Played != true
            }
        }
        .filter { item ->
            genre == null || item.Genres.orEmpty().any { it.equals(genre, ignoreCase = true) }
        }
        .toList()

    val sorted = when (sortMode) {
        LibrarySortMode.Name -> filtered.sortedWith(
            compareBy<JellyfinItem> { it.Name.trim().lowercase() }
                .thenBy { it.Id }
        )
        LibrarySortMode.DateAdded -> filtered.sortedWith(
            compareBy<JellyfinItem> { it.DateCreated.orEmpty() }.thenBy { it.Id }
        )
        LibrarySortMode.PremiereDate -> filtered.sortedWith(
            compareBy<JellyfinItem> { it.PremiereDate.orEmpty() }.thenBy { it.Id }
        )
        LibrarySortMode.Runtime -> filtered.sortedWith(
            compareBy<JellyfinItem> { it.RunTimeTicks ?: 0L }.thenBy { it.Name.lowercase() }
        )
        LibrarySortMode.CriticRating -> filtered.sortedWith(
            compareBy<JellyfinItem> { it.CriticRating ?: -1f }.thenBy { it.Name.lowercase() }
        )
        LibrarySortMode.CommunityRating -> filtered.sortedWith(
            compareBy<JellyfinItem> { it.CommunityRating ?: -1f }.thenBy { it.Name.lowercase() }
        )
        LibrarySortMode.Random -> filtered.shuffled()
    }

    return if (descending) sorted.asReversed() else sorted
}

fun availableLibraryGenres(items: List<JellyfinItem>): List<String> =
    items.asSequence()
        .flatMap { it.Genres.orEmpty().asSequence() }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy { it.lowercase() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .toList()
