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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.klortek.velora.jellyfin.JellyfinApiService
import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.RemoteSessionInfo
import com.klortek.velora.jellyfin.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private val MobileBackground = Color(0xFF090A0D)
private val MobileCyan = Color(0xFF16C8F2)

@Composable
fun MobileMovieDetailsLayout(
    item: JellyfinItem,
    backdropUrl: String,
    apiService: JellyfinApiService?,
    onBack: (() -> Unit)?,
    onPlay: () -> Unit,
    onRestart: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    var selectedSection by remember { mutableStateOf("Reparto") }
    var similarMovies by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(item.Id, item.Genres, apiService) {
        val genre = item.Genres?.firstOrNull()
        if (apiService != null && genre != null) {
            similarMovies = withContext(Dispatchers.IO) {
                runCatching { apiService.getMoviesByGenre(genre, excludeItemId = item.Id, limit = 12) }
                    .getOrDefault(emptyList())
            }
        }
    }
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        MobileBackdrop(backdropUrl, apiService, item.Name)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .align(Alignment.TopCenter)
                .background(MobileBackground.copy(alpha = 0.62f))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MobileBackButton(onBack)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MobileArtwork(apiService?.getImageUrl(item.Id, "Primary", null, maxWidth = 600, maxHeight = 900, quality = 88), apiService, item.Name, Modifier.widthIn(max = 155.dp).fillMaxWidth(0.38f).aspectRatio(0.67f))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.Name, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                    MobileMetadata(item)
                    item.Overview?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha = .86f), maxLines = 5, overflow = TextOverflow.Ellipsis) }
                }
            }
            MobilePlayButton(onPlay)
            MobileActionRow(onShuffle = onPlay, onRestart = onRestart ?: onPlay, onDownload = onDownload, onAudio = { showAudioDialog = true }, onRemote = { showRemoteDialog = true }, hasAudio = (item.MediaSources?.firstOrNull()?.MediaStreams?.count { it.Type == "Audio" } ?: 0) > 1)
            MobileDetailTabs(selectedSection) { selectedSection = it }
            when (selectedSection) {
                "Reparto" -> MobilePeople(item, apiService)
                "Equipo" -> MobileCrew(item, apiService)
                "Estudios" -> Text("No hay información de estudios disponible", color = Color.White.copy(alpha = .72f))
                "Detalles" -> MobileFileDetails(item)
                "Similares" -> MobileSimilarMovies(similarMovies, apiService)
                else -> MobilePeople(item, apiService)
            }
        }
    }
    if (showAudioDialog) MobileAudioSelectionDialog(item, { showAudioDialog = false })
    if (showRemoteDialog) MobileRemotePlaybackDialog(item, apiService, { showRemoteDialog = false })
}

@Composable
fun MobileSeriesDetailsLayout(
    item: JellyfinItem,
    backdropUrl: String,
    seasons: List<JellyfinItem>,
    selectedSeasonIndex: Int,
    episodes: List<JellyfinItem>,
    apiService: JellyfinApiService?,
    onBack: (() -> Unit)?,
    onSeasonSelected: (Int) -> Unit,
    onPlay: (JellyfinItem?) -> Unit,
    onRestart: ((JellyfinItem?) -> Unit)? = null,
    onDownload: ((JellyfinItem) -> Unit)? = null
) {
    Box(Modifier.fillMaxSize()) {
        MobileBackdrop(backdropUrl, apiService, item.Name)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .align(Alignment.TopCenter)
                .background(MobileBackground.copy(alpha = .64f))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MobileBackButton(onBack)
            Text(item.Name, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            MobileMetadata(item)
            item.Overview?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha = .9f), maxLines = 5, overflow = TextOverflow.Ellipsis) }
            MobilePlayButton { onPlay(episodes.firstOrNull { (it.UserData?.PositionTicks ?: 0L) > 0L } ?: episodes.firstOrNull()) }
            MobileActionRow(
                onShuffle = { onPlay(episodes.shuffled().firstOrNull()) },
                onRestart = { onRestart?.invoke(episodes.firstOrNull()) ?: onPlay(episodes.firstOrNull()) },
                onDownload = episodes.firstOrNull()?.let { { onDownload?.invoke(it) } }
            )
            if (seasons.isNotEmpty()) {
                Text("Temporadas", color = Color.White, style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                    items(seasons.indices.toList()) { index ->
                        val selected = index == selectedSeasonIndex
                        Text("Temporada ${seasons[index].IndexNumber ?: index + 1}", color = if (selected) Color.Black else Color.White, modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(if (selected) MobileCyan else Color.White.copy(alpha = .16f)).clickable { onSeasonSelected(index) }.padding(horizontal = 18.dp, vertical = 10.dp))
                    }
                }
            }
            Text("Episodios", color = Color.White, style = MaterialTheme.typography.titleMedium)
            episodes.forEach { episode ->
                MobileEpisodeCard(episode, apiService, { onDownload?.invoke(episode) }) { onPlay(episode) }
            }
            MobilePeople(item, apiService)
        }
    }
}

@Composable private fun MobileBackdrop(url: String, apiService: JellyfinApiService?, name: String) {
    Box(Modifier.fillMaxSize().background(MobileBackground)) {
        if (url.isNotEmpty() && apiService != null) AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).headers(apiService.getImageRequestHeaders()).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build(),
            contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .22f), MobileBackground.copy(alpha = .95f)))))
    }
}

@Composable private fun MobileBackButton(onBack: (() -> Unit)?) {
    androidx.tv.material3.IconButton(onClick = { onBack?.invoke() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, "Atrás", tint = Color.White) }
}

@Composable private fun MobilePlayButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color(0xFF168DB4))) {
        Icon(Icons.Default.PlayArrow, "Reproducir", Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Reproducir", fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun MobileActionRow(onShuffle: () -> Unit, onRestart: () -> Unit, onDownload: (() -> Unit)? = null, onAudio: (() -> Unit)? = null, onRemote: (() -> Unit)? = null, hasAudio: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MobileActionButton("Aleatorio", Icons.Default.Shuffle, onShuffle)
        MobileActionButton("Reiniciar", Icons.Default.Replay, onRestart)
        if (hasAudio) MobileActionButton("Audio", Icons.Default.VolumeUp, onAudio ?: {})
        MobileActionButton("Transmitir", Icons.Default.Cast, onRemote ?: {})
        onDownload?.let { MobileActionButton("Descargar", Icons.Default.Download, it) }
    }
}

@Composable private fun MobileActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White)
        }
        Text(label, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MobileAudioSelectionDialog(item: JellyfinItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val streams = item.MediaSources?.firstOrNull()?.MediaStreams?.filter { it.Type == "Audio" }.orEmpty()
    val settings = remember(context) { AppSettings(context) }
    var selected by remember { mutableStateOf(settings.getAudioPreference(item.Id)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f)), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color(0xFF17191D)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pista de audio", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (streams.isEmpty()) Text("No hay pistas de audio disponibles", color = Color.White.copy(alpha = .7f), modifier = Modifier.padding(vertical = 18.dp))
                LazyColumn { items(streams, key = { it.Index ?: it.hashCode() }) { stream ->
                    val index = stream.Index
                    val title = stream.DisplayTitle ?: stream.DisplayLanguage ?: stream.Language ?: "Audio"
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                        selected = index
                        settings.setAudioPreference(item.Id, index)
                        onDismiss()
                    }.padding(horizontal = 10.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (selected == index) Icons.Default.Check else Icons.Default.VolumeUp, title, tint = if (selected == index) MobileCyan else Color.White.copy(alpha = .7f), modifier = Modifier.size(24.dp))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            stream.Codec?.let { Text(it.uppercase(), color = Color.White.copy(alpha = .62f), style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                } }
                Text("Cancelar", color = MobileCyan, modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(vertical = 14.dp))
            }
        }
    }
}

@Composable
private fun MobileRemotePlaybackDialog(item: JellyfinItem, apiService: JellyfinApiService?, onDismiss: () -> Unit) {
    var sessions by remember { mutableStateOf<List<RemoteSessionInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sentTo by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(apiService) {
        sessions = apiService?.getControllableSessions().orEmpty()
        loading = false
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f)), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color(0xFF17191D)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reproducción remota", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                when {
                    loading -> Text("Buscando dispositivos…", color = Color.White.copy(alpha = .7f), modifier = Modifier.padding(vertical = 18.dp))
                    sessions.isEmpty() -> Text("No hay dispositivos disponibles", color = Color.White.copy(alpha = .7f), modifier = Modifier.padding(vertical = 18.dp))
                    else -> sessions.forEach { session ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .06f)).clickable {
                            session.Id?.let { id -> scope.launch { if (apiService?.playOnRemoteSession(id, item.Id, item.UserData?.PositionTicks?.div(10_000L) ?: 0L) == true) sentTo = session.DeviceName } }
                        }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cast, "Dispositivo", tint = MobileCyan, modifier = Modifier.size(28.dp))
                            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                                Text(session.DeviceName ?: "Dispositivo", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(listOfNotNull(session.UserName, session.Client).joinToString(" · "), color = Color.White.copy(alpha = .62f), style = MaterialTheme.typography.bodySmall)
                            }
                            if (sentTo == session.DeviceName) Icon(Icons.Default.Check, "Enviado", tint = MobileCyan)
                        }
                    }
                }
                Text("Cerrar", color = MobileCyan, modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(vertical = 12.dp))
            }
        }
    }
}

@Composable private fun MobileMetadata(item: JellyfinItem) {
    val values = listOfNotNull(item.ProductionYear?.toString(), item.formattedRuntime?.replace("hours", "h")?.replace("hour", "h")?.replace("mins", "m")?.replace("min", "m"), item.OfficialRating, item.CommunityRating?.let { "★ ${"%.1f".format(it)}" })
    Text(values.joinToString("  ·  "), color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium)
    item.Genres?.takeIf { it.isNotEmpty() }?.let { Text(it.take(3).joinToString("  ·  ") { localizedGenreName(it) }, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall) }
}

@Composable private fun MobileArtwork(url: String?, apiService: JellyfinApiService?, name: String, modifier: Modifier) {
    if (url != null && apiService != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(url).headers(apiService.getImageRequestHeaders()).build(), contentDescription = name, modifier = modifier.clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
    else Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .12f)))
}

@Composable private fun MobileEpisodeCard(episode: JellyfinItem, apiService: JellyfinApiService?, onDownload: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .42f)).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val image = remember(episode.Id, apiService) {
            apiService?.getImageUrl(episode.Id, "Primary", null, maxWidth = 500, maxHeight = 280, quality = 82)
        }
        MobileArtwork(image, apiService, episode.Name, Modifier.width(140.dp).aspectRatio(1.65f))
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("E${episode.IndexNumber ?: ""} · ${episode.Name}", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(episode.formattedRuntime ?: "", color = Color.White.copy(alpha = .7f)); Text(episode.Overview ?: "", color = Color.White.copy(alpha = .78f), maxLines = 2, overflow = TextOverflow.Ellipsis) }
        androidx.tv.material3.IconButton(onClick = onDownload) { Icon(Icons.Default.Download, "Descargar", tint = MobileCyan) }
    }
}

@Composable private fun MobilePeople(item: JellyfinItem, apiService: JellyfinApiService?) {
    val people = item.People?.filter { it.Type == "Actor" }?.take(12).orEmpty()
    if (people.isNotEmpty()) {
        Text("Reparto", color = Color.White, style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(people) { CastMemberCard(person = it, apiService = apiService) } }
    }
}

@Composable
private fun MobileDetailTabs(selected: String, onSelected: (String) -> Unit) {
    val tabs = listOf("Reparto", "Equipo", "Estudios", "Detalles", "Similares")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(tabs) { tab ->
            Text(
                tab,
                color = if (selected == tab) Color.White else Color.White.copy(alpha = .82f),
                fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected == tab) MobileCyan else Color.White.copy(alpha = .12f))
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun MobileCrew(item: JellyfinItem, apiService: JellyfinApiService?) {
    val crew = item.People?.filter { it.Type != "Actor" }.orEmpty()
    if (crew.isEmpty()) {
        Text("No hay información del equipo", color = Color.White.copy(alpha = .72f))
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(crew.take(12)) { CastMemberCard(person = it, apiService = apiService) }
        }
    }
}

@Composable
private fun MobileFileDetails(item: JellyfinItem) {
    val source = item.MediaSources?.firstOrNull()
    val streams = source?.MediaStreams.orEmpty()
    val video = streams.firstOrNull { it.Type == "Video" }
    val audio = streams.filter { it.Type == "Audio" }
    val resolution = if (video?.Width != null && video.Height != null) "${video.Width} × ${video.Height}" else null
    val frameRate = video?.RealFrameRate ?: video?.AverageFrameRate
    val videoDetails = listOfNotNull(video?.Codec?.uppercase(), resolution, frameRate?.let { "${String.format("%.2f", it)} fps" }, video?.VideoRangeType ?: video?.VideoRange).joinToString("  ·  ")
    val audioDetails = audio.mapNotNull { stream ->
        val language = stream.DisplayLanguage ?: stream.Language
        val codec = stream.Codec?.uppercase()
        listOfNotNull(language, codec).joinToString("  ·  ").takeIf { it.isNotBlank() }
    }.distinct()
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = .28f)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Información del archivo", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Vídeo", color = MobileCyan, fontWeight = FontWeight.SemiBold)
        Text(videoDetails.ifBlank { "Información de vídeo no disponible" }, color = Color.White.copy(alpha = .78f))
        Text("Audio", color = MobileCyan, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        if (audioDetails.isEmpty()) Text("Información de audio no disponible", color = Color.White.copy(alpha = .78f))
        else audioDetails.forEach { Text(it, color = Color.White.copy(alpha = .78f)) }
        source?.Container?.takeIf { it.isNotBlank() }?.let {
            Text("Formato", color = MobileCyan, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            Text(it.uppercase(), color = Color.White.copy(alpha = .78f))
        }
    }
}

@Composable
private fun MobileSimilarMovies(items: List<JellyfinItem>, apiService: JellyfinApiService?) {
    if (items.isEmpty()) {
        Text("No hay títulos similares disponibles", color = Color.White.copy(alpha = .72f))
        return
    }
    val context = LocalContext.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.Id }) { movie ->
            val image = remember(movie.Id, apiService) { apiService?.getImageUrl(movie.Id, "Primary", null, maxWidth = 320, maxHeight = 480, quality = 80) }
            Column(Modifier.width(132.dp).clickable {
                context.startActivity(
                    com.klortek.velora.JellyfinVideoPlayerActivity.createIntent(
                        context = context,
                        itemId = movie.Id,
                        resumePositionMs = movie.UserData?.PositionTicks?.let { it / 10_000L } ?: 0L,
                        itemName = movie.Name
                    )
                )
            }) {
                MobileArtwork(image, apiService, movie.Name, Modifier.fillMaxWidth().height(188.dp))
                Text(movie.Name, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}
