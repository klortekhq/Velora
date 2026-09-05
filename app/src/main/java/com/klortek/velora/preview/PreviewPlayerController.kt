package com.klortek.velora.preview

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * A single muted player for Home previews. It deliberately owns no UI so the
 * same instance can be attached to a TV PlayerView and reused across focus
 * changes without rebuilding the decoder.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PreviewPlayerController(context: Context) {
    private val dataSourceFactory = DefaultHttpDataSource.Factory()

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                false
            )
        }

    private var currentItemId: String? = null

    fun play(itemId: String, url: String, headers: Map<String, String>) {
        if (itemId.isBlank() || url.isBlank()) return
        if (currentItemId == itemId) {
            player.playWhenReady = true
            return
        }
        currentItemId = itemId
        dataSourceFactory.setDefaultRequestProperties(headers)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        currentItemId = null
        player.pause()
        player.clearMediaItems()
    }

    fun release() {
        currentItemId = null
        player.release()
    }
}
