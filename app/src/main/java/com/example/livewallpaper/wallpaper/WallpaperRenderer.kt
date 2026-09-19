package com.example.livewallpaper.wallpaper

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder

/**
 * Contract for wallpaper visuals.
 *
 * The bundled [com.example.livewallpaper.wallpaper.renderers.VideoRenderer]
 * drives the surface directly via MediaPlayer ([drivesOwnSurface]); the
 * canvas methods below exist for renderers that draw frame-by-frame instead.
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

    /** Home-screen scroll offset, 0..1. */
    fun onOffsetsChanged(xOffset: Float, yOffset: Float) = Unit

    /** Touch interaction. */
    fun onTouch(event: MotionEvent, width: Int, height: Int) = Unit

    /** Render one frame (only used when [drivesOwnSurface] is false). */
    fun drawFrame(canvas: Canvas, elapsedNanos: Long, config: WallpaperConfig)

    /** Release player, shaders, bitmaps or pools. Called in Engine.onDestroy. */
    fun release() = Unit
}
