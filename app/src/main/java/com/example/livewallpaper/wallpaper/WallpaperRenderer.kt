package com.example.livewallpaper.wallpaper

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder

/**
 * Contract every wallpaper visual must implement.
 *
 * Implementations must be cheap to construct, must do all allocation either
 * up-front or in [onSurfaceChanged], and must never block inside [drawFrame]:
 * the engine calls it on a dedicated handler at up to 60fps.
 *
 * Implementations do NOT need to be thread-safe beyond being driven from the
 * single engine thread.
 */
interface WallpaperRenderer {

    /**
     * True when the renderer drives the SurfaceHolder's surface itself (e.g.
     * MediaPlayer video) instead of the engine's canvas loop. The engine then
     * skips lockCanvas/draw and only forwards lifecycle/visibility/config.
     */
    val drivesOwnSurface: Boolean get() = false

    /** Called with a live surface (created or changed). */
    fun onSurfaceAttached(holder: SurfaceHolder) = Unit

    /** Called when the surface is being torn down; release surface-bound resources. */
    fun onSurfaceDetached() = Unit

    /** Mirrors Engine.onVisibilityChanged: pause/resume surface-owned playback. */
    fun onVisibilityChanged(visible: Boolean) = Unit

    /** Latest settings snapshot; called on every DataStore emission. */
    fun onConfigChanged(config: WallpaperConfig) = Unit

    /** Called when the surface size changes (rotation, fold/unfold, DeX). */
    fun onSurfaceChanged(width: Int, height: Int)

    /**
     * Home-screen scroll offset, 0..1. Only used when
     * [WallpaperConfig.parallaxEnabled] is true; One UI/Samsung launchers
     * report this reliably, third-party launchers may always report 0.
     */
    fun onOffsetsChanged(xOffset: Float, yOffset: Float) = Unit

    /** Touch interaction; engine forwards events only when enabled in config. */
    fun onTouch(event: MotionEvent, width: Int, height: Int) = Unit

    /**
     * Render one frame. [elapsedNanos] is time since the engine started and is
     * guaranteed monotonic; use it (scaled by config.speedMultiplier) instead
     * of wall-clock time so frames stay smooth across doze/sleep.
     */
    fun drawFrame(canvas: Canvas, elapsedNanos: Long, config: WallpaperConfig)

    /** Release shaders, bitmaps, pools. Called once in Engine.onDestroy. */
    fun release() = Unit
}
