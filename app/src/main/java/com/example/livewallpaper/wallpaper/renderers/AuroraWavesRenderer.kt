package com.example.livewallpaper.wallpaper.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.example.livewallpaper.wallpaper.WallpaperConfig
import com.example.livewallpaper.wallpaper.WallpaperRenderer
import kotlin.math.sin

/**
 * Layered sine "aurora" ribbons over a dark gradient.
 * Pure vector drawing (paths), resolution-independent — ideal for Fold/DeX
 * where the surface can be very large.
 */
class AuroraWavesRenderer : WallpaperRenderer {

    private var width = 1
    private var height = 1
    private var xOffset = 0f

    private val bgPaint = Paint()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()

    private data class Band(val yFrac: Float, val ampFrac: Float, val color: Int, val speed: Double, val thick: Float)

    private val bands = listOf(
        Band(0.35f, 0.06f, 0xAA22D3EE.toInt(), 0.7, 90f),
        Band(0.5f, 0.08f, 0xAA818CF8.toInt(), 0.5, 130f),
        Band(0.65f, 0.05f, 0xAAF472B6.toInt(), 0.9, 70f)
    )

    override fun onSurfaceChanged(width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        this.xOffset = xOffset.coerceIn(0f, 1f)
    }

    override fun drawFrame(canvas: Canvas, elapsedNanos: Long, config: WallpaperConfig) {
        val bg = if (config.amoledDark) 0xFF000000.toInt() else config.baseColors.firstOrNull()
            ?: 0xFF0B1026.toInt()
        canvas.drawColor(bg)

        val t = elapsedNanos / 1_000_000_000.0 * config.speedMultiplier.toDouble()
        val parallax = if (config.parallaxEnabled) (xOffset - 0.5f) * width * 0.15f else 0f
        val scale = if (config.batterySaver) 24 else 12 // fewer segments in saver mode

        for (band in bands) {
            path.reset()
            val baseY = band.yFrac * height
            val amp = band.ampFrac * height
            var first = true
            var x = -20f
            while (x <= width + 20f) {
                val y = (baseY
                    + amp * sin(x / width * 6.28 + t * band.speed)
                    + amp * 0.4 * sin(x / width * 12.56 - t * band.speed * 0.7)).toFloat()
                if (first) {
                    path.moveTo(x + parallax, y)
                    first = false
                } else {
                    path.lineTo(x + parallax, y)
                }
                x += scale
            }
            wavePaint.color = band.color
            wavePaint.strokeWidth = band.thick * height / 1920f
            wavePaint.alpha = 170
            canvas.drawPath(path, wavePaint)
        }
    }
}
