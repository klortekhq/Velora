package com.klortek.velora.preview

import android.content.Context
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
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
    private var fadeAnimator: ValueAnimator? = null
    private var targetVolume = 0f

    fun play(url: String, headers: Map<String, String>, volume: Float = 0.7f) {
        if (url.isBlank()) return
        targetVolume = volume.coerceIn(0f, 1f)
        dataSourceFactory.setDefaultRequestProperties(headers)
        if (currentUrl == url) {
            fadeTo(targetVolume, FADE_IN_MS)
            player.playWhenReady = true
            return
        }

        val startNewSource = {
            currentUrl = url
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )
            player.volume = 0f
            player.prepare()
            player.playWhenReady = true
            fadeTo(targetVolume, FADE_IN_MS)
        }

        if (currentUrl == null) {
            startNewSource()
        } else {
            fadeTo(0f, FADE_OUT_MS) { startNewSource() }
        }
    }

    fun stop() {
        if (currentUrl == null && !player.isPlaying) return
        fadeTo(0f, FADE_OUT_MS) {
            currentUrl = null
            player.pause()
            player.clearMediaItems()
        }
    }

    fun release() {
        fadeAnimator?.cancel()
        fadeAnimator = null
        player.release()
    }

    private fun fadeTo(target: Float, durationMs: Long, onEnd: (() -> Unit)? = null) {
        fadeAnimator?.cancel()
        val start = player.volume
        fadeAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator -> player.volume = animator.animatedValue as Float }
            doOnEnd(onEnd)
            start()
        }
    }

    private fun ValueAnimator.doOnEnd(action: (() -> Unit)?) {
        if (action == null) return
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (fadeAnimator !== this@doOnEnd) return
                fadeAnimator = null
                action()
            }
        })
    }

    private companion object {
        const val FADE_IN_MS = 500L
        const val FADE_OUT_MS = 180L
    }
}
