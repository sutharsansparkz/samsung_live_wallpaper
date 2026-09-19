package com.example.livewallpaper.wallpaper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.livewallpaper.settings.WallpaperPreferencesRepository
import com.example.livewallpaper.wallpaper.renderers.RendererFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Frame-loop controller owned by [LiveWallpaperService.LiveEngine].
 *
 * Plain class (not an `Engine` subclass) on purpose: `Engine` is an inner
 * class of [android.service.wallpaper.WallpaperService], so the thin inner
 * class in [LiveWallpaperService] only forwards callbacks here. All
 * frame-loop, visibility, battery and renderer logic lives in this file,
 * and new wallpaper styles plug in via [RendererFactory] without touching
 * service code.
 *
 * Battery design:
 * - Draws ONLY while the engine reports visible (covers home-screen hidden,
 *   screen off, fullscreen app). Screen-off => visible=false => loop parks.
 * - No wake locks, no foreground service, no polling while invisible.
 * - FPS budget from user settings (15/30/60, capped at 30 in battery-saver;
 *   picker previews always capped at 30).
 * - Defensive lockCanvas/unlock so a torn-down surface (fold/unfold, DeX,
 *   rotation) skips one tick instead of killing the wallpaper.
 */
class WallpaperEngine(
    private val host: Host,
    context: Context
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
        WallpaperPreferencesRepository.get(context)

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var config: WallpaperConfig = WallpaperConfig.DEFAULT

    private var renderer: WallpaperRenderer = RendererFactory.create(config.type)
    private var lastType: WallpaperType = config.type

    private var surfaceW = 0
    private var surfaceH = 0
    private var surfaceValid = false
    private var running = false
    private var startNanos = SystemClock.elapsedRealtimeNanos()
    private var configJob: Job? = null

    private val frameTick = object : Runnable {
        override fun run() {
            drawTick()
            // Re-arm only while drawing is still wanted; otherwise the loop
            // parks with zero CPU until the next visibility/surface callback.
            if (running && host.engineIsVisible && surfaceValid) {
                handler.postDelayed(this, frameDelayMs())
            } else {
                running = false
            }
        }
    }

    // ---- Lifecycle (called from LiveEngine) --------------------------------

    fun onCreate() {
        host.engineSetTouchEventsEnabled(true)
        host.engineSetOffsetNotificationsEnabled(true)
        startNanos = SystemClock.elapsedRealtimeNanos()
        // Live config: keeps running after the settings Activity closes and
        // updates in place when the user changes options.
        configJob = scope.launch {
            prefs.config.collect { next ->
                val typeChanged = next.type != lastType
                config = next
                if (typeChanged) recreateRenderer(next.type)
            }
        }
    }

    fun onSurfaceCreated() {
        surfaceValid = true
        startLoop()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        // Foldables / rotation / DeX can deliver 0 during transitions; ignore.
        if (width > 0 && height > 0) {
            surfaceW = width
            surfaceH = height
            renderer.onSurfaceChanged(width, height)
        }
        startLoop()
    }

    fun onSurfaceDestroyed() {
        surfaceValid = false
        stopLoop()
    }

    fun onVisibilityChanged(visible: Boolean) {
        // visible=false covers: screen off, fullscreen app, lock screen hiding
        // the home wallpaper. All cases must park the loop to save battery.
        if (visible) startLoop() else stopLoop()
    }

    fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        if (config.parallaxEnabled) {
            renderer.onOffsetsChanged(xOffset, yOffset)
        }
    }

    fun onTouchEvent(event: MotionEvent) {
        if (config.touchInteractionEnabled && surfaceW > 0 && surfaceH > 0) {
            renderer.onTouch(event, surfaceW, surfaceH)
        }
    }

    fun onDestroy() {
        stopLoop()
        configJob?.cancel()
        scope.cancel()
        renderer.release()
    }

    // ---- Frame loop --------------------------------------------------------

    private fun frameDelayMs(): Long {
        val fps = if (host.engineIsPreview) {
            minOf(config.effectiveFps, 30) // picker thumbnail stays cheap
        } else {
            config.effectiveFps
        }.coerceIn(15, 60)
        return 1000L / fps
    }

    private fun startLoop() {
        if (running) return
        if (!surfaceValid || !host.engineIsVisible) return
        if (surfaceW <= 0 || surfaceH <= 0) {
            val frame = host.engineHolder.surfaceFrame
            if (frame.width() > 0 && frame.height() > 0) {
                surfaceW = frame.width()
                surfaceH = frame.height()
                renderer.onSurfaceChanged(surfaceW, surfaceH)
            } else {
                return // wait for onSurfaceChanged
            }
        }
        running = true
        handler.removeCallbacks(frameTick)
        handler.post(frameTick)
    }

    private fun stopLoop() {
        running = false
        handler.removeCallbacks(frameTick)
    }

    private fun drawTick() {
        if (!surfaceValid || !host.engineIsVisible) {
            running = false
            return
        }
        val holder = host.engineHolder
        if (holder.surface == null || !holder.surface.isValid) return

        var canvas: android.graphics.Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                val elapsed = SystemClock.elapsedRealtimeNanos() - startNanos
                renderer.drawFrame(canvas, elapsed, config)
            }
        } catch (_: IllegalArgumentException) {
            // Surface destroyed mid-lock (fold/unfold, rapid picker exit).
        } catch (_: IllegalStateException) {
            // Surface not ready; skip this tick.
        } catch (_: Exception) {
            // Never let one bad frame kill the wallpaper (black-screen guard).
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: IllegalArgumentException) {
                    // Surface went away between lock and unlock.
                } catch (_: IllegalStateException) {
                    // Already unlocked / destroyed.
                }
            }
        }
    }

    private fun recreateRenderer(type: WallpaperType) {
        lastType = type
        try {
            renderer.release()
        } catch (_: Exception) {
        }
        renderer = RendererFactory.create(type)
        if (surfaceW > 0 && surfaceH > 0) {
            renderer.onSurfaceChanged(surfaceW, surfaceH)
        }
    }
}
