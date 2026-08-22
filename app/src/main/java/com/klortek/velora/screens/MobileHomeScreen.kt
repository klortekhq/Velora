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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
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
    apiService: JellyfinApiService?,
    onItemClick: (JellyfinItem) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onLiveTv: () -> Unit
) {
    val hero = continueWatching.firstOrNull() ?: recentMovies.firstOrNull() ?: recentShows.firstOrNull()
    Box(Modifier.fillMaxSize().background(MobileHomeBackground)) {
        MobileHomeHero(hero, apiService)
        LazyColumn(
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Velora", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onLiveTv) { Icon(Icons.Default.Tv, "Televisión en directo", tint = Color.White) }
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Buscar", tint = Color.White) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Ajustes", tint = Color.White) }
                }
            }
            if (continueWatching.isNotEmpty()) item { MobileHomeMediaRow("Seguir viendo", continueWatching, apiService, onItemClick, showProgress = true) }
            if (recentMovies.isNotEmpty()) item { MobileHomeMediaRow("Películas añadidas recientemente", recentMovies, apiService, onItemClick) }
            if (recentShows.isNotEmpty()) item { MobileHomeMediaRow("Series añadidas recientemente", recentShows, apiService, onItemClick) }
            if (recentEpisodes.isNotEmpty()) item { MobileHomeMediaRow("Episodios añadidos recientemente", recentEpisodes, apiService, onItemClick) }
            if (unwatched.isNotEmpty()) item { MobileHomeMediaRow("Sin terminar", unwatched, apiService, onItemClick) }
            if (popular.isNotEmpty()) item { MobileHomeMediaRow("Más populares", popular, apiService, onItemClick) }
        }
    }
}

@Composable
private fun MobileHomeHero(item: JellyfinItem?, apiService: JellyfinApiService?) {
    if (item == null || apiService == null) return
    val url = apiService.getImageUrl(item.Id, "Backdrop", null, maxWidth = 1280, maxHeight = 720, quality = 88) ?: return
    Box(Modifier.fillMaxWidth().height(430.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).headers(apiService.getImageRequestHeaders()).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build(),
            contentDescription = item.Name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MobileHomeBackground.copy(alpha = .35f), MobileHomeBackground))))
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
                val image = apiService?.getImageUrl(item.Id, "Primary", null, maxWidth = 420, maxHeight = 620, quality = 84)
                Column(Modifier.width(132.dp).clickable { onItemClick(item) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .1f))) {
                        if (image != null && apiService != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(image).headers(apiService.getImageRequestHeaders()).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build(), contentDescription = item.Name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (showProgress) {
                            val progress = ((item.UserData?.PlayedPercentage ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(4.dp).background(MobileHomeCyan))
                        }
                    }
                    Text(item.Name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
