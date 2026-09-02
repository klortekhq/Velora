package com.klortek.velora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.livetv.LiveTvChannel
import com.klortek.velora.livetv.LiveTvClient
import com.klortek.velora.livetv.LiveTvProgram
import com.klortek.velora.livetv.formatProgramTimeRange
import com.klortek.velora.livetv.programProgress
import com.klortek.velora.livetv.filterLiveTvChannels
import com.klortek.velora.livetv.liveTvGroups
import com.klortek.velora.livetv.groupLiveTvChannels
import com.klortek.velora.livetv.LiveTvChannelGroup
import com.klortek.velora.livetv.liveTvSourceLabel
import com.klortek.velora.livetv.liveTvMediaSourceId
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class LiveTvActivity : ComponentActivity() {
    companion object {
        fun createIntent(context: Context): Intent = Intent(context, LiveTvActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JellyfinAppTheme {
                val config = remember { JellyfinConfig(this@LiveTvActivity) }
                LiveTvScreen(
                    config = config,
                    onBack = { finish() },
                    onPlay = { channel, channelList ->
                        startActivity(
                            JellyfinVideoPlayerActivity.createIntent(
                                context = this@LiveTvActivity,
                                itemId = channel.Id,
                                itemName = channel.Name,
                                isLiveTv = true,
                                liveTvMediaSourceId = liveTvMediaSourceId(channel),
                                liveTvChannelIds = channelList.map { it.Id },
                                liveTvChannelNames = channelList.map { it.Name }
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveTvScreen(
    config: JellyfinConfig,
    onBack: () -> Unit,
    onPlay: (LiveTvChannel, List<LiveTvChannel>) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isMobile = LocalConfiguration.current.screenWidthDp < 600
    val client = remember(config.serverUrl, config.accessToken, config.userId) {
        LiveTvClient(config)
    }
    DisposableEffect(client) {
        onDispose { client.close() }
    }

    var refreshKey by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var channels by remember { mutableStateOf<List<LiveTvChannel>>(emptyList()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var programDetails by remember { mutableStateOf<Pair<String, LiveTvProgram>?>(null) }
    var sourceSelection by remember { mutableStateOf<LiveTvChannelGroup?>(null) }
    val scope = rememberCoroutineScope()
    val firstChannelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(client, refreshKey) {
        isLoading = true
        loadError = null
        try {
            val loadedChannels = client.getChannels()
            val upcoming = runCatching {
                client.getUpcomingPrograms(loadedChannels.map { it.Id })
            }.getOrDefault(emptyMap())
            channels = loadedChannels.map { channel ->
                channel.copy(UpcomingProgram = upcoming[channel.Id])
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveTvActivity", "Could not load Live TV channels", e)
            loadError = "error"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            firstChannelFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = if (isMobile) 16.dp else 34.dp, vertical = if (isMobile) 12.dp else 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMobile) {
                LiveTvTouchButton(Icons.Default.ArrowBack, stringResource(R.string.back), onBack)
            } else {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                }
            }

            Spacer(modifier = Modifier.width(if (isMobile) 8.dp else 12.dp))

            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isMobile) 26.dp else 28.dp)
            )

            Spacer(modifier = Modifier.width(if (isMobile) 6.dp else 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.live_tv),
                    style = if (isMobile) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!isLoading && loadError == null) {
                    Text(
                        text = stringResource(R.string.live_tv_channel_count, groupLiveTvChannels(channels).size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                    )
                }
            }

            if (isMobile) {
                LiveTvTouchButton(
                    Icons.Default.Refresh,
                    stringResource(R.string.live_tv_refresh),
                    onClick = { if (!isLoading) refreshKey++ },
                    enabled = !isLoading
                )
            } else {
                IconButton(
                    onClick = { refreshKey++ },
                    enabled = !isLoading,
                    colors = IconButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.live_tv_refresh))
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isMobile) 12.dp else 14.dp))

        if (channels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveTvFilterPill(stringResource(R.string.live_tv_all_channels), !favoritesOnly && selectedGroup == null) {
                    favoritesOnly = false
                    selectedGroup = null
                }
                LiveTvFilterPill(stringResource(R.string.live_tv_favorites), favoritesOnly) {
                    favoritesOnly = !favoritesOnly
                    if (favoritesOnly) selectedGroup = null
                }
                liveTvGroups(channels).forEach { group ->
                    LiveTvFilterPill(group, selectedGroup.equals(group, ignoreCase = true)) {
                        selectedGroup = if (selectedGroup.equals(group, ignoreCase = true)) null else group
                        favoritesOnly = false
                    }
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.live_tv_loading),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            loadError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(
                                if (loadError == "favorite") R.string.live_tv_favorite_update_error else R.string.live_tv_error
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loadError ?: "",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            channels.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.live_tv_no_channels),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            else -> {
                val visibleChannels = filterLiveTvChannels(channels, favoritesOnly, selectedGroup)
                val visibleGroups = groupLiveTvChannels(visibleChannels)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleGroups, key = { it.channelId }) { channelGroup ->
                        val channel = channelGroup.primary
                        LiveTvChannelRow(
                            channel = channel,
                            client = client,
                            channelGroupCount = channelGroup.channels.size,
                            compact = isMobile,
                            focusRequester = if (channel.Id == channels.firstOrNull()?.Id) {
                                firstChannelFocusRequester
                            } else {
                                null
                            },
                            onClick = {
                                if (channelGroup.channels.size > 1) sourceSelection = channelGroup
                                else onPlay(channel, visibleGroups.map { it.primary })
                            },
                            onShowProgram = { program -> programDetails = channel.Name to program },
                            onToggleFavorite = {
                                val favorite = channel.UserData?.IsFavorite != true
                                scope.launch {
                                    runCatching { client.setFavorite(channel.Id, favorite) }
                                        .onSuccess {
                                            channels = channels.map { current ->
                                                if (current.Id == channel.Id) current.copy(UserData = com.klortek.velora.livetv.LiveTvUserData(favorite)) else current
                                            }
                                        }
                                        .onFailure {
                                            android.util.Log.e("LiveTvActivity", "Could not update Live TV favorite", it)
                                            loadError = "favorite"
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    programDetails?.let { (channelName, program) ->
        LiveTvProgramDialog(
            channelName = channelName,
            program = program,
            onDismiss = { programDetails = null }
        )
    }

    sourceSelection?.let { group ->
        LiveTvSourceDialog(
            group = group,
            onDismiss = { sourceSelection = null },
            onSelect = { selected ->
                sourceSelection = null
                onPlay(selected, groupLiveTvChannels(channels).map { it.primary })
            }
        )
    }
}

@Composable
private fun LiveTvSourceDialog(
    group: LiveTvChannelGroup,
    onDismiss: () -> Unit,
    onSelect: (LiveTvChannel) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text(group.primary.Name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.live_tv_source_count, group.channels.size),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            group.channels.forEachIndexed { index, channel ->
                Button(
                    onClick = { onSelect(channel) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(liveTvSourceLabel(channel, index + 1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.live_tv_cancel))
            }
        }
    }
}

@Composable
private fun LiveTvChannelRow(
    channel: LiveTvChannel,
    client: LiveTvClient,
    channelGroupCount: Int = 1,
    compact: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onShowProgram: (LiveTvProgram) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val program = channel.CurrentProgram
    val progress = programProgress(program)
    val timeRange = formatProgramTimeRange(program)

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        scale = CardDefaults.scale(focusedScale = 1.01f),
        colors = CardDefaults.colors(
            containerColor = if (focused) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (focused) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 8.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = if (compact) 84.dp else 88.dp, height = if (compact) 48.dp else 46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(client.channelImageUrl(channel.Id))
                        .headers(client.imageHeaders)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(150)
                        .build(),
                    contentDescription = channel.Name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.Name,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (channel.UserData?.IsFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(if (channel.UserData?.IsFavorite == true) R.string.live_tv_remove_favorite else R.string.live_tv_add_favorite),
                            tint = if (channel.UserData?.IsFavorite == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
                        )
                    }
                    if (program != null) {
                        IconButton(onClick = { onShowProgram(program) }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.live_tv_program_details),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
                            )
                        }
                    }
                }

                if (channelGroupCount > 1) {
                    Text(
                        text = stringResource(R.string.live_tv_source_count, channelGroupCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = program?.Name?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.live_tv_no_program),
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                channel.UpcomingProgram?.Name?.takeIf { it.isNotBlank() }?.let { nextTitle ->
                    Text(
                        text = stringResource(R.string.live_tv_next_program, nextTitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (progress != null || timeRange != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(if (compact) 140.dp else 190.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress ?: 0f)
                                    .height(3.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (timeRange != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = timeRange,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTvProgramDialog(
    channelName: String,
    program: LiveTvProgram,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text(
                text = program.Name.orEmpty().ifBlank { stringResource(R.string.live_tv_no_program) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.live_tv_channel)}: $channelName",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
            )
            val seriesName = program.SeriesName?.takeIf { it.isNotBlank() }
            val episodeTitle = program.EpisodeTitle?.takeIf { it.isNotBlank() }
            if (seriesName != null || episodeTitle != null) {
                Text(
                    text = listOfNotNull(
                        seriesName,
                        episodeTitle,
                        program.SeasonNumber?.let { season ->
                            program.EpisodeNumber?.let { episode -> "S$season E$episode" }
                        }
                    ).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .86f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            formatProgramTimeRange(program)?.let { range ->
                Text(text = range, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
            }
            program.Overview?.takeIf { it.isNotBlank() }?.let { overview ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = overview, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .9f))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.live_tv_close_details))
            }
        }
    }
}

@Composable
private fun LiveTvFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun LiveTvTouchButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 1f else .45f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
    }
}
