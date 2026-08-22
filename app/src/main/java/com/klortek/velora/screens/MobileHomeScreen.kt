package com.klortek.velora.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private val MobileHomeBackground = Color(0xFF090B10)
private val MobileHomeCyan = Color(0xFF18C9F1)

@Composable
fun MobileHomeScreen(
    continueWatching: List<JellyfinItem>,
    recentMovies: List<JellyfinItem>,
    recentShows: List<JellyfinItem>,
    recentEpisodes: List<JellyfinItem>,
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
    showLiveTv: Boolean
) {
    val hero = continueWatching.firstOrNull() ?: recentMovies.firstOrNull() ?: recentShows.firstOrNull()
    Box(Modifier.fillMaxSize().background(MobileHomeBackground)) {
        MobileHomeHero(hero, apiService, onItemClick)
        LazyColumn(
            contentPadding = PaddingValues(top = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp)
                .align(Alignment.TopCenter)
                .navigationBarsPadding()
        ) {
            item {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    if (showLiveTv) IconButton(onClick = onLiveTv) { Icon(Icons.Default.Tv, "Televisión en directo", tint = Color.White) }
                    IconButton(onClick = onDownloads) { Icon(Icons.Default.Download, "Descargas", tint = Color.White) }
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Buscar", tint = Color.White) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Ajustes", tint = Color.White) }
                }
            }
            item { MobileMediaPanel(libraries, onLibraryClick, onSearch) }
            if (continueWatching.isNotEmpty()) item { MobileHomeMediaRow("Seguir viendo", continueWatching, apiService, onItemClick, showProgress = true) }
            if (recentMovies.isNotEmpty()) item { MobileHomeMediaRow("Películas añadidas recientemente", recentMovies, apiService, onItemClick) }
            if (recentShows.isNotEmpty()) item { MobileHomeMediaRow("Series añadidas recientemente", recentShows, apiService, onItemClick) }
            if (recentEpisodes.isNotEmpty()) item { MobileHomeMediaRow("Episodios añadidos recientemente", recentEpisodes, apiService, onItemClick) }
            if (unwatched.isNotEmpty()) item { MobileHomeMediaRow("Sin terminar", unwatched, apiService, onItemClick) }
            if (popular.isNotEmpty()) item { MobileHomeMediaRow("Más populares", popular, apiService, onItemClick) }
        }
        MobileBottomNavigation(onHome = {}, onMedia = onSearch, onDownloads = onDownloads, modifier = Modifier.align(Alignment.BottomCenter))
    }
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
            Icon(Icons.Default.Search, "Buscar películas y series", tint = Color.White.copy(alpha = .8f))
            Text("Buscar películas y series", color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(start = 12.dp))
        }
        if (libraries.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                libraries.filter { it.CollectionType.equals("movies", true) || it.CollectionType.equals("tvshows", true) }.take(2).forEach { library ->
                    Box(Modifier.weight(1f).height(108.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .1f)).clickable { onLibraryClick(library) }) {
                        Text(
                            if (library.CollectionType.equals("tvshows", true)) "Series" else "Películas",
                            color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MobileLibraryScreen(
    library: JellyfinLibrary,
    items: List<JellyfinItem>,
    apiService: JellyfinApiService?,
    onBack: () -> Unit,
    onItemClick: (JellyfinItem) -> Unit
) {
    Column(Modifier.fillMaxSize().background(MobileHomeBackground).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
            Text(if (library.CollectionType.equals("tvshows", true)) "Series" else "Películas", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (items.isEmpty()) {
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
                    .align(Alignment.TopCenter)
            ) {
                gridItems(items, key = { it.Id }) { item ->
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
private fun MobileBottomNavigation(onHome: () -> Unit, onMedia: () -> Unit, onDownloads: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().background(Color(0xEE171A21)).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MobileBottomNavigationItem(Icons.Default.Home, "Inicio", onHome, selected = true)
        MobileBottomNavigationItem(Icons.Default.VideoLibrary, "Mis medios", onMedia)
        MobileBottomNavigationItem(Icons.Default.Download, "Descargas", onDownloads)
    }
}

@Composable
private fun MobileBottomNavigationItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, selected: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 4.dp)) {
        Icon(icon, label, tint = if (selected) MobileHomeCyan else Color.White.copy(alpha = .82f), modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) MobileHomeCyan else Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MobileHomeMediaRow(
    title: String,
    items: List<JellyfinItem>,
    apiService: JellyfinApiService?,
    onItemClick: (JellyfinItem) -> Unit,
    showProgress: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.Id }) { item ->
                val image = remember(item.Id, apiService) {
                    apiService?.getImageUrl(item.Id, "Primary", null, maxWidth = 320, maxHeight = 480, quality = 78)
                }
                Column(Modifier.width(132.dp).clickable { onItemClick(item) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
