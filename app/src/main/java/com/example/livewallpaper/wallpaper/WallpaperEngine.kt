package com.example.livewallpaper.wallpaper

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.livewallpaper.settings.WallpaperPreferencesRepository
import com.example.livewallpaper.wallpaper.renderers.VideoRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Frame-loop controller owned by [LiveWallpaperService.LiveEngine].
 *
 * This app has a single wallpaper: a user-picked looping video. The
 * [VideoRenderer] drives the surface directly via MediaPlayer, so there is no
 * canvas draw loop at all — this controller only forwards surface,
 * visibility and config events.
 *
 * Battery design:
 * - Playback is paused whenever the engine reports invisible (covers
 *   home-screen hidden, screen off, fullscreen app). No decoding while hidden.
 * - No wake locks, no foreground service, no polling.
 * - Defensive player lifecycle so a torn-down surface (fold/unfold, DeX,
 *   rotation) never kills the wallpaper.
 */
class WallpaperEngine(
    private val host: Host,
    private val appContext: Context
) {
    /**
     * What the controller needs from the hosting Engine.
     * Member names are prefixed with `engine` to avoid clashing with the
     * `Engine` base-class API (getSurfaceHolder/isVisible/isPreview).
     */
    interface Host {
        val engineHolder: SurfaceHolder
        val engineIsVisible: Boolean
        val engineIsPreview: Boolean
        fun engineSetTouchEventsEnabled(enabled: Boolean)
        fun engineSetOffsetNotificationsEnabled(enabled: Boolean)
    }

    private val prefs: WallpaperPreferencesRepository =
        WallpaperPreferencesRepository.get(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var config: WallpaperConfig = WallpaperConfig.DEFAULT

    private var renderer: WallpaperRenderer = VideoRenderer(appContext)
    private var lastVideoUri: String? = config.videoUri

    private var surfaceValid = false
    private var configJob: Job? = null

    // ---- Lifecycle (called from LiveEngine) --------------------------------

    fun onCreate() {
        // Video needs no touch or parallax events; leave them off to save power.
        host.engineSetTouchEventsEnabled(false)
        host.engineSetOffsetNotificationsEnabled(false)
        // Live config: keeps running after the settings Activity closes and
        // swaps video in place when the user picks a new one.
        configJob = scope.launch {
            prefs.config.collect { next ->
                config = next
                if (next.videoUri != lastVideoUri) {
                    recreateRenderer()
                } else {
                    renderer.onConfigChanged(next)
                }
            }
        }
    }

    fun onSurfaceCreated() {
        surfaceValid = true
        renderer.onConfigChanged(config)
        renderer.onSurfaceAttached(host.engineHolder)
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            renderer.onSurfaceChanged(width, height)
        }
        if (surfaceValid) {
            renderer.onSurfaceAttached(host.engineHolder)
        }
    }

    fun onSurfaceDestroyed() {
        surfaceValid = false
        renderer.onSurfaceDetached()
    }

    fun onVisibilityChanged(visible: Boolean) {
        // visible=false covers: screen off, fullscreen app, lock screen hiding
        // the home wallpaper. The renderer pauses playback in all cases.
        renderer.onVisibilityChanged(visible)
    }

    fun onOffsetsChanged(xOffset: Float, yOffset: Float) = Unit

    fun onTouchEvent(event: MotionEvent) = Unit

    fun onDestroy() {
        configJob?.cancel()
        scope.cancel()
        renderer.release()
    }

    // ---- Renderer swap ------------------------------------------------------

    private fun recreateRenderer() {
        lastVideoUri = config.videoUri
        try {
            renderer.release()
        } catch (_: Exception) {
        }
        renderer = VideoRenderer(appContext)
        renderer.onConfigChanged(config)
        if (surfaceValid) {
            renderer.onSurfaceAttached(host.engineHolder)
        }
    }
}
