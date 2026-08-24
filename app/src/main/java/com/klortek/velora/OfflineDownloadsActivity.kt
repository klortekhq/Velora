package com.klortek.velora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.offline.OfflineDownload
import com.klortek.velora.offline.OfflineDownloadManager

class OfflineDownloadsActivity : ComponentActivity() {
    companion object {
        fun createIntent(context: Context): Intent = Intent(context, OfflineDownloadsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JellyfinAppTheme {
                OfflineDownloadsScreen(
                    onBack = { finish() },
                    onPlay = { entry -> startActivity(JellyfinVideoPlayerActivity.createIntent(this, entry.itemId, itemName = entry.name, localPath = entry.localPath)) },
                    onDelete = { entry -> OfflineDownloadManager.delete(this, entry); refresh++ }
                )
            }
        }
    }

    private var refresh by mutableStateOf(0)

    @androidx.compose.runtime.Composable
    private fun OfflineDownloadsScreen(onBack: () -> Unit, onPlay: (OfflineDownload) -> Unit, onDelete: (OfflineDownload) -> Unit) {
        refresh
        val entries = OfflineDownloadManager.refresh(this)
        LaunchedEffect(entries.any { !it.isComplete }) {
            while (entries.any { !it.isComplete }) {
                kotlinx.coroutines.delay(1500)
                refresh++
            }
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 10.dp))
                Text("Descargas", style = MaterialTheme.typography.headlineSmall)
            }
            if (entries.isEmpty()) {
                Text("No hay contenido descargado", color = Color.Gray, modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.downloadId }) { entry ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = entry.isComplete) { onPlay(entry) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (entry.isComplete) "Disponible sin conexión" else "Descargando… ${entry.progress}%", color = Color.Gray)
                            }
                            if (entry.isComplete) IconButton(onClick = { onPlay(entry) }) { Icon(Icons.Default.PlayArrow, "Reproducir") }
                            Button(onClick = { onDelete(entry) }) { Icon(Icons.Default.Delete, "Eliminar") }
                        }
                    }
                }
            }
        }
    }
}
