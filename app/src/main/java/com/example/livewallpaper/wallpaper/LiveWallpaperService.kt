package com.example.livewallpaper.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

/**
 * Native live-wallpaper entry point declared in AndroidManifest with the
 * BIND_WALLPAPER permission. The system binds to this service (not to any
 * Activity) when the user picks the wallpaper, including after reboot —
 * no BOOT_COMPLETED receiver or foreground service is needed or used.
 *
 * [LiveEngine] is intentionally thin: all frame-loop, visibility, battery
 * and renderer logic lives in [WallpaperEngine], which is independently
 * testable and extensible via RendererFactory.
 */
class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = LiveEngine()

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // No caches to drop today (renderers are allocation-free in steady
        // state); hook retained so future bitmap/shader caches can trim here.
    }

    inner class LiveEngine : Engine(), WallpaperEngine.Host {

        private val controller: WallpaperEngine by lazy {
            WallpaperEngine(host = this, context = this@LiveWallpaperService)
        }

        // ---- WallpaperEngine.Host -----------------------------------------
        override val engineHolder: SurfaceHolder
            get() = super.getSurfaceHolder()
        override val engineIsVisible: Boolean
            get() = super.isVisible()
        override val engineIsPreview: Boolean
            get() = super.isPreview()
        override fun engineSetTouchEventsEnabled(enabled: Boolean) {
            super.setTouchEventsEnabled(enabled)
        }
        override fun engineSetOffsetNotificationsEnabled(enabled: Boolean) {
            super.setOffsetNotificationsEnabled(enabled)
        }

        // ---- Engine callbacks (thin forwards) ------------------------------
        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            controller.onCreate()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            controller.onSurfaceCreated()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            controller.onSurfaceChanged(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            controller.onSurfaceDestroyed()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            controller.onVisibilityChanged(visible)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset
            )
            controller.onOffsetsChanged(xOffset, yOffset)
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            controller.onTouchEvent(event)
        }

        override fun onDestroy() {
            controller.onDestroy()
            super.onDestroy()
        }
    }
}
