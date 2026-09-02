package com.klortek.velora.preview

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** Owns one background audio player for the Home screen and releases it safely. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ThemeMusicController(context: Context) {
    private val dataSourceFactory = DefaultHttpDataSource.Factory()
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
        }
    private var currentUrl: String? = null

    fun play(url: String, headers: Map<String, String>, volume: Float = 0.7f) {
        if (url.isBlank()) return
        dataSourceFactory.setDefaultRequestProperties(headers)
        if (currentUrl != url) {
            currentUrl = url
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )
            player.prepare()
        }
        player.volume = volume.coerceIn(0f, 1f)
        player.playWhenReady = true
    }

    fun stop() {
        currentUrl = null
        player.pause()
        player.clearMediaItems()
    }

    fun release() = player.release()
}
