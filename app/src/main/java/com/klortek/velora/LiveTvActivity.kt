package com.klortek.velora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
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
import com.klortek.velora.livetv.formatProgramTimeRange
import com.klortek.velora.livetv.programProgress

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
                    onPlay = { channel ->
                        startActivity(
                            JellyfinVideoPlayerActivity.createIntent(
                                context = this@LiveTvActivity,
                                itemId = channel.Id,
                                itemName = channel.Name,
                                isLiveTv = true
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
    onPlay: (LiveTvChannel) -> Unit
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

    LaunchedEffect(client, refreshKey) {
        isLoading = true
        loadError = null
        try {
            channels = client.getChannels()
        } catch (e: Exception) {
            android.util.Log.e("LiveTvActivity", "Could not load Live TV channels", e)
            loadError = e.message ?: e.javaClass.simpleName
        } finally {
            isLoading = false
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
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
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
                        text = stringResource(R.string.live_tv_channel_count, channels.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                    )
                }
            }

            IconButton(
                onClick = { refreshKey++ },
                enabled = !isLoading,
                colors = IconButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.live_tv_refresh)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isMobile) 12.dp else 14.dp))

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
                            text = stringResource(R.string.live_tv_error),
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(channels, key = { it.Id }) { channel ->
                        LiveTvChannelRow(
                            channel = channel,
                            client = client,
                            compact = isMobile,
                            onClick = { onPlay(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTvChannelRow(
    channel: LiveTvChannel,
    client: LiveTvClient,
    compact: Boolean = false,
    onClick: () -> Unit
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
