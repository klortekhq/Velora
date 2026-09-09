@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.klortek.velora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.offline.OfflineDownload
import com.klortek.velora.offline.OfflineDownloadManager
import com.klortek.velora.offline.OfflineDownloadQuality
import com.klortek.velora.offline.OfflineStorageEngine
import com.klortek.velora.platform.PlatformCapabilities

class OfflineDownloadsActivity : ComponentActivity() {
    companion object {
        fun createIntent(context: Context): Intent = Intent(context, OfflineDownloadsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PlatformCapabilities.supportsOfflineDownloads) {
            finish()
            return
        }
        setContent {
            JellyfinAppTheme {
                OfflineDownloadsScreen(
                    onBack = { finish() },
                    onPlay = { entry ->
                        lifecycleScope.launch {
                            val managedEntry = OfflineStorageEngine.materialize(this@OfflineDownloadsActivity, entry) ?: entry
                            if (OfflineDownloadManager.verifyIntegrity(this@OfflineDownloadsActivity, managedEntry)) {
                                startActivity(JellyfinVideoPlayerActivity.createIntent(this@OfflineDownloadsActivity, managedEntry.itemId, itemName = managedEntry.name, localPath = managedEntry.localPath))
                            } else {
                                android.widget.Toast.makeText(
                                    this@OfflineDownloadsActivity,
                                    getString(R.string.offline_integrity_failed),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onDelete = { entry -> OfflineDownloadManager.delete(this, entry); refresh++ },
                    onPause = { entry -> OfflineDownloadManager.pause(this, entry); refresh++ },
                    onResume = { entry -> OfflineDownloadManager.resume(this, entry); refresh++ }
                )
            }
        }
    }

    private var refresh by mutableStateOf(0)

    @androidx.compose.runtime.Composable
    private fun OfflineDownloadsScreen(
        onBack: () -> Unit,
        onPlay: (OfflineDownload) -> Unit,
        onDelete: (OfflineDownload) -> Unit,
        onPause: (OfflineDownload) -> Unit,
        onResume: (OfflineDownload) -> Unit
    ) {
        refresh
        val config = remember { JellyfinConfig(this@OfflineDownloadsActivity) }
        var entries by remember { mutableStateOf(emptyList<OfflineDownload>()) }
        LaunchedEffect(refresh) {
            entries = OfflineDownloadManager.refresh(
                context = this@OfflineDownloadsActivity,
                accountServerUrl = config.serverUrl,
                accountUserId = config.userId
            )
        }
        LaunchedEffect(entries.any { !it.isComplete }) {
            while (entries.any { !it.isComplete }) {
                kotlinx.coroutines.delay(1500)
                refresh++
            }
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back)) }
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 10.dp))
                Text(stringResource(R.string.nav_downloads), style = MaterialTheme.typography.headlineSmall)
            }
            if (entries.isEmpty()) {
                Text(stringResource(R.string.downloads_empty), color = Color.Gray, modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.stableKey }) { entry ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = entry.isComplete) { onPlay(entry) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (entry.isComplete) stringResource(R.string.offline_available)
                                    else if (entry.speedBytesPerSecond > 0L && entry.etaSeconds != null) {
                                        stringResource(
                                            R.string.offline_transfer_stats,
                                            entry.progress,
                                            formatTransferRate(entry.speedBytesPerSecond),
                                            formatDuration(entry.etaSeconds)
                                        )
                                    } else stringResource(R.string.downloading_progress, entry.progress),
                                    color = Color.Gray
                                )
                                Text(
                                    OfflineDownloadQuality.fromStorageKey(entry.quality).label,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (entry.isComplete) {
                                IconButton(onClick = { onPlay(entry) }) { Icon(Icons.Default.PlayArrow, stringResource(R.string.action_play)) }
                            } else if (entry.state == com.klortek.velora.offline.OfflineDownloadState.PAUSED) {
                                Button(onClick = { onResume(entry) }) { Text(stringResource(R.string.action_resume)) }
                            } else {
                                Button(onClick = { onPause(entry) }) { Text(stringResource(R.string.action_pause)) }
                            }
                            Button(onClick = { onDelete(entry) }) { Icon(Icons.Default.Delete, stringResource(R.string.action_delete)) }
                        }
                    }
                }
            }
        }
    }

    private fun formatTransferRate(bytesPerSecond: Long): String = when {
        bytesPerSecond >= 1024L * 1024L -> "%.1f MB".format(bytesPerSecond / (1024.0 * 1024.0))
        bytesPerSecond >= 1024L -> "%.0f KB".format(bytesPerSecond / 1024.0)
        else -> "$bytesPerSecond B"
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60L
        val remaining = seconds % 60L
        return if (minutes > 0L) "${minutes}m ${remaining}s" else "${remaining}s"
    }
}
