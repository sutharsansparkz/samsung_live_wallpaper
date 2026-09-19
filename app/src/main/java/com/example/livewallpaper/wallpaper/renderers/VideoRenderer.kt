package com.example.livewallpaper.wallpaper.renderers

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import com.example.livewallpaper.wallpaper.WallpaperConfig
import com.example.livewallpaper.wallpaper.WallpaperRenderer

/**
 * Plays a user-picked video as the wallpaper.
 *
 * Unlike the canvas renderers, this drives the surface directly: a [MediaPlayer]
 * renders into the wallpaper [Surface] with center-crop scaling, looping forever.
 * The engine parks its canvas loop ([drivesOwnSurface]) and only forwards
 * surface/visibility/config events.
 *
 * Battery behaviour: playback is paused whenever the engine reports invisible
 * (screen off, fullscreen app), so no decoding happens while hidden. Video
 * decoding itself uses the hardware codec and is comparable to watching a video.
 */
class VideoRenderer(private val context: Context) : WallpaperRenderer {

    override val drivesOwnSurface = true

    private var player: MediaPlayer? = null
    private var attachedSurface: Surface? = null
    private var config: WallpaperConfig = WallpaperConfig.DEFAULT
    private var visible = false
    private var prepared = false

    override fun onSurfaceChanged(width: Int, height: Int) = Unit

    override fun drawFrame(
        canvas: android.graphics.Canvas,
        elapsedNanos: Long,
        config: WallpaperConfig
    ) = Unit // never called: drivesOwnSurface skips the canvas loop

    override fun onSurfaceAttached(holder: SurfaceHolder) {
        val surface = holder.surface
        val current = player
        if (current != null && prepared && attachedSurface === surface && surface?.isValid == true) {
            if (visible) startQuietly(current)
            return
        }
        rebuild(surface)
    }

    override fun onSurfaceDetached() {
        teardown()
        attachedSurface = null
    }

    override fun onVisibilityChanged(visible: Boolean) {
        this.visible = visible
        val p = player ?: return
        if (!prepared) return
        if (visible) {
            startQuietly(p)
        } else {
            try {
                if (p.isPlaying) p.pause()
            } catch (_: Exception) {
            }
        }
    }

    override fun onConfigChanged(config: WallpaperConfig) {
        val uriChanged = config.videoUri != this.config.videoUri
        this.config = config
        applyVolume()
        if (uriChanged) {
            val surface = attachedSurface
            if (surface != null && surface.isValid) {
                rebuild(surface)
            } else {
                teardown()
            }
        }
    }

    override fun release() {
        teardown()
        attachedSurface = null
    }

    private fun rebuild(surface: Surface?) {
        teardown()
        val uriString = config.videoUri
        attachedSurface = surface
        if (uriString.isNullOrBlank() || surface == null || !surface.isValid) return
        try {
            val mp = MediaPlayer()
            mp.setDataSource(context, Uri.parse(uriString))
            mp.setSurface(surface)
            // Center-crop: fill the screen like a wallpaper, no letterboxing.
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            mp.isLooping = true
            if (config.videoMuted) mp.setVolume(0f, 0f)
            mp.setOnPreparedListener {
                prepared = true
                if (visible) startQuietly(it)
            }
            mp.setOnErrorListener { _, _, _ ->
                prepared = false
                true // swallow: keep last frame instead of killing the wallpaper
            }
            mp.prepareAsync()
            player = mp
        } catch (_: Exception) {
            teardown()
        }
    }

    private fun applyVolume() {
        val p = player ?: return
        try {
            if (config.videoMuted) p.setVolume(0f, 0f) else p.setVolume(1f, 1f)
        } catch (_: Exception) {
        }
    }

    private fun startQuietly(p: MediaPlayer) {
        try {
            p.start()
        } catch (_: Exception) {
        }
    }

    private fun teardown() {
        prepared = false
        val p = player
        player = null
        if (p != null) {
            try {
                p.reset()
            } catch (_: Exception) {
            }
            try {
                p.release()
            } catch (_: Exception) {
            }
        }
    }
}
