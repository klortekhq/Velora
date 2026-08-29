package com.klortek.velora.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.klortek.velora.jellyfin.JellyfinApiService
import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.JellyfinLibrary
import com.klortek.velora.jellyfin.AppSettings

private val MobileHomeBackground = Color(0xFF090B10)
private val MobileHomeCyan = Color(0xFF18C9F1)

@Composable
fun MobileHomeScreen(
    continueWatching: List<JellyfinItem>,
    recentMovies: List<JellyfinItem>,
    recentShows: List<JellyfinItem>,
    popular: List<JellyfinItem>,
    unwatched: List<JellyfinItem>,
    libraries: List<JellyfinLibrary>,
    apiService: JellyfinApiService?,
    onItemClick: (JellyfinItem) -> Unit,
    onLibraryClick: (JellyfinLibrary) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onDownloads: () -> Unit,
    onLiveTv: () -> Unit,
    showLiveTv: Boolean,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onContinueWatchingLongClick: (JellyfinItem) -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(MobileHomeBackground)) {
        // Mobile home is a scrollable Plex-style dashboard.  Do not render the
        // TV hero layer here: it is a separate, fixed-height surface that used
        // to sit behind the LazyColumn and leave the selected episode title
        // permanently overlaid on the first rows.
        LazyColumn(
            contentPadding = PaddingValues(top = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp)
                .align(Alignment.TopCenter)
                .navigationBarsPadding()
        ) {
            if (continueWatching.isNotEmpty()) item {
                MobileHomeMediaRow(
                    title = "Seguir viendo",
                    items = continueWatching,
                    apiService = apiService,
                    onItemClick = onItemClick,
                    onLongClick = onContinueWatchingLongClick,
                    showProgress = true
                )
            }
            if (recentMovies.isNotEmpty()) item { MobileHomeMediaRow("Películas añadidas recientemente", recentMovies, apiService, onItemClick) }
            if (recentShows.isNotEmpty()) item { MobileHomeMediaRow("Series añadidas recientemente", recentShows, apiService, onItemClick) }
            if (unwatched.isNotEmpty()) item { MobileHomeMediaRow("Sin terminar", unwatched, apiService, onItemClick) }
            if (popular.isNotEmpty()) item { MobileHomeMediaRow("Más populares", popular, apiService, onItemClick) }
        }
        val movieLibrary = libraries.firstOrNull { it.CollectionType.equals("movies", true) }
        val seriesLibrary = libraries.firstOrNull { it.CollectionType.equals("tvshows", true) }
        MobileBottomNavigation(
            onHome = {},
            onMovies = { movieLibrary?.let { onMovies() } ?: onSearch() },
            onSeries = { seriesLibrary?.let { onSeries() } ?: onSearch() },
            onLiveTv = onLiveTv,
            showMovies = movieLibrary != null,
            showSeries = seriesLibrary != null,
            showLiveTv = showLiveTv,
            onDownloads = onDownloads,
            onSearch = onSearch,
            onSettings = onSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MobileHeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = .08f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(25.dp)) }
}

@Composable
private fun MobileMediaPanel(
    libraries: List<JellyfinLibrary>,
    onLibraryClick: (JellyfinLibrary) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .1f)).clickable(onClick = onSearch).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, stringResource(com.klortek.velora.R.string.search_movies_series), tint = Color.White.copy(alpha = .8f))
            Text(stringResource(com.klortek.velora.R.string.search_movies_series), color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
fun MobileLibraryScreen(
    library: JellyfinLibrary,
    items: List<JellyfinItem>,
    recommendations: List<JellyfinItem> = emptyList(),
    apiService: JellyfinApiService?,
    onBack: () -> Unit,
    onItemClick: (JellyfinItem) -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var mobileSortMode by remember {
        mutableStateOf(
            when (settings.getSortType()) {
                "DateAdded" -> LibrarySortMode.DateAdded
                "DateReleased" -> LibrarySortMode.PremiereDate
                "Runtime" -> LibrarySortMode.Runtime
                "CriticRating" -> LibrarySortMode.CriticRating
                "CommunityRating" -> LibrarySortMode.CommunityRating
                else -> LibrarySortMode.Name
            }
        )
    }
    var sortDescending by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var playbackFilter by remember { mutableStateOf(LibraryPlaybackFilter.All) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(if (recommendations.isNotEmpty()) 0 else 1) }
    val availableGenres = remember(items) { availableLibraryGenres(items) }
    val sortedItems = remember(items, mobileSortMode, sortDescending, favoritesOnly, playbackFilter, selectedGenre) {
        queryLibraryItems(items, mobileSortMode, sortDescending, favoritesOnly, playbackFilter, selectedGenre)
    }

    Column(Modifier.fillMaxSize().background(MobileHomeBackground).navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = .12f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                if (library.CollectionType.equals("tvshows", true)) "Series" else "Películas",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = .1f))
                    .clickable { showSortDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text("↕", color = MobileHomeCyan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MobileLibraryTab("Recomendaciones", selectedTab == 0 && recommendations.isNotEmpty()) { selectedTab = 0 }
            MobileLibraryTab("Todo", selectedTab == 1 || recommendations.isEmpty()) { selectedTab = 1 }
        }
        val visibleItems = if (selectedTab == 0 && recommendations.isNotEmpty()) recommendations else sortedItems
        if (visibleItems.isEmpty()) {
            Text("No hay contenido disponible", color = Color.White.copy(alpha = .75f), modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 132.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1080.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                gridItems(visibleItems, key = { it.Id }) { item ->
                    val image = remember(item.Id, apiService) {
                        apiService?.getImageUrl(item.Id, "Primary", null, maxWidth = 420, maxHeight = 620, quality = 84)
                    }
                    Column(Modifier.clickable { onItemClick(item) }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .1f))) {
                            if (image != null && apiService != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(image).headers(apiService.getImageRequestHeaders()).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build(), contentDescription = item.Name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Text(item.Name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        Dialog(onDismissRequest = { showSortDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(.9f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF171A21),
                    contentColor = Color.White
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ordenar y filtrar", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Ordenar por", color = MobileHomeCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        MobileLibrarySortOption("Nombre", mobileSortMode == LibrarySortMode.Name) {
                            mobileSortMode = LibrarySortMode.Name
                            settings.setSortType("Alphabetically")
                        }
                        MobileLibrarySortOption("Fecha de incorporación", mobileSortMode == LibrarySortMode.DateAdded) {
                            mobileSortMode = LibrarySortMode.DateAdded
                            settings.setSortType("DateAdded")
                        }
                        MobileLibrarySortOption("Fecha de estreno", mobileSortMode == LibrarySortMode.PremiereDate) {
                            mobileSortMode = LibrarySortMode.PremiereDate
                            settings.setSortType("DateReleased")
                        }
                        MobileLibrarySortOption("Duración", mobileSortMode == LibrarySortMode.Runtime) {
                            mobileSortMode = LibrarySortMode.Runtime
                            settings.setSortType("Runtime")
                        }
                        MobileLibrarySortOption("Valoración de la crítica", mobileSortMode == LibrarySortMode.CriticRating) {
                            mobileSortMode = LibrarySortMode.CriticRating
                            settings.setSortType("CriticRating")
                        }
                        MobileLibrarySortOption("Valoración de la comunidad", mobileSortMode == LibrarySortMode.CommunityRating) {
                            mobileSortMode = LibrarySortMode.CommunityRating
                            settings.setSortType("CommunityRating")
                        }
                        Text("Filtros", color = MobileHomeCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        MobileLibraryCheckOption("Favoritos", favoritesOnly) { favoritesOnly = !favoritesOnly }
                        Text("Estado de reproducción", color = MobileHomeCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        MobileLibrarySortOption("Todos", playbackFilter == LibraryPlaybackFilter.All) { playbackFilter = LibraryPlaybackFilter.All }
                        MobileLibrarySortOption("Vistos", playbackFilter == LibraryPlaybackFilter.Watched) { playbackFilter = LibraryPlaybackFilter.Watched }
                        MobileLibrarySortOption("No vistos", playbackFilter == LibraryPlaybackFilter.Unwatched) { playbackFilter = LibraryPlaybackFilter.Unwatched }
                        if (availableGenres.isNotEmpty()) {
                            Text("Género", color = MobileHomeCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                            MobileLibrarySortOption("Todos los géneros", selectedGenre == null) { selectedGenre = null }
                            availableGenres.forEach { genre ->
                                MobileLibrarySortOption(localizedGenreName(genre), selectedGenre.equals(genre, ignoreCase = true)) {
                                    selectedGenre = genre
                                }
                            }
                        }
                        Text("Dirección", color = MobileHomeCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        MobileLibrarySortOption("Ascendente", !sortDescending) { sortDescending = false }
                        MobileLibrarySortOption("Descendente", sortDescending) { sortDescending = true }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showSortDialog = false },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) { Text(stringResource(com.klortek.velora.R.string.action_back)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileLibraryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color(0xFF071218) else Color.White.copy(alpha = .85f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MobileHomeCyan else Color.White.copy(alpha = .08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

@Composable
private fun MobileLibrarySortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) MobileHomeCyan.copy(alpha = .18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (selected) "●" else "○", color = if (selected) MobileHomeCyan else Color.White.copy(alpha = .7f), style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun MobileLibraryCheckOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (selected) "☑" else "☐", color = if (selected) MobileHomeCyan else Color.White.copy(alpha = .7f), style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun MobileHomeHero(item: JellyfinItem?, apiService: JellyfinApiService?, onItemClick: (JellyfinItem) -> Unit) {
    if (item == null || apiService == null) return
    val url = apiService.getImageUrl(item.Id, "Backdrop", null, maxWidth = 960, maxHeight = 540, quality = 80) ?: return
    Box(Modifier.fillMaxWidth().height(390.dp).clickable { onItemClick(item) }) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).headers(apiService.getImageRequestHeaders()).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build(),
            contentDescription = item.Name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MobileHomeBackground.copy(alpha = .38f), MobileHomeBackground))))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(if (item.Type == "Episode") item.SeriesName ?: item.Name else item.Name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.Type == "Episode") {
                Text("S${item.ParentIndexNumber ?: "?"} E${item.IndexNumber ?: "?"} · ${item.Name}", color = Color.White.copy(alpha = .86f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Continuar viendo", color = MobileHomeCyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MobileBottomNavigation(
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onLiveTv: () -> Unit,
    showMovies: Boolean,
    showSeries: Boolean,
    showLiveTv: Boolean,
    onDownloads: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color(0xEE171A21))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val itemModifier = Modifier.width(76.dp)
        MobileBottomNavigationItem(Icons.Default.Home, stringResource(com.klortek.velora.R.string.nav_home), onHome, selected = true, modifier = itemModifier)
        if (showMovies) MobileBottomNavigationItem(Icons.Default.Movie, stringResource(com.klortek.velora.R.string.nav_movies), onMovies, modifier = itemModifier)
        if (showSeries) MobileBottomNavigationItem(Icons.Default.Tv, stringResource(com.klortek.velora.R.string.nav_series), onSeries, modifier = itemModifier)
        if (showLiveTv) MobileBottomNavigationItem(Icons.Default.Tv, "TV", onLiveTv, modifier = itemModifier)
        MobileBottomNavigationItem(Icons.Default.Search, "Buscar", onSearch, modifier = itemModifier)
        MobileBottomNavigationItem(Icons.Default.Download, stringResource(com.klortek.velora.R.string.nav_downloads), onDownloads, modifier = itemModifier)
        MobileBottomNavigationItem(Icons.Default.Settings, stringResource(com.klortek.velora.R.string.nav_settings), onSettings, modifier = itemModifier)
    }
}

@Composable
private fun MobileBottomNavigationItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, selected: Boolean = false, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 4.dp)) {
        Icon(icon, label, tint = if (selected) MobileHomeCyan else Color.White.copy(alpha = .82f), modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) MobileHomeCyan else Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MobileHomeMediaRow(
    title: String,
    items: List<JellyfinItem>,
    apiService: JellyfinApiService?,
    onItemClick: (JellyfinItem) -> Unit,
    onLongClick: ((JellyfinItem) -> Unit)? = null,
    showProgress: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.Id }) { item ->
                val image = remember(item.Id, apiService) {
                    apiService?.getImageUrl(item.Id, "Primary", null, maxWidth = 320, maxHeight = 480, quality = 78)
                }
                Column(
                    Modifier
                        .width(132.dp)
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = onLongClick?.let { callback -> { callback(item) } }
                        ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .1f))) {
                        if (image != null && apiService != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(image).headers(apiService.getImageRequestHeaders()).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build(), contentDescription = item.Name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (showProgress) {
                            val progress = ((item.UserData?.PlayedPercentage ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(4.dp).background(MobileHomeCyan))
                        }
                    }
                    val displayName = if (showProgress && !item.SeriesName.isNullOrBlank()) {
                        val season = item.ParentIndexNumber?.let { "T$it" }.orEmpty()
                        val episode = item.IndexNumber?.let { "E$it" }.orEmpty()
                        val marker = listOf(season, episode).filter { it.isNotBlank() }.joinToString(" ")
                        if (marker.isNotBlank()) "${item.SeriesName} · $marker\n${item.Name}" else "${item.SeriesName}\n${item.Name}"
                    } else item.Name
                    Text(displayName, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
