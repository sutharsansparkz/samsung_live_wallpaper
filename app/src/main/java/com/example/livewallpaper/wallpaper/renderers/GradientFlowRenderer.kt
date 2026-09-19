package com.example.livewallpaper.wallpaper.renderers

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.example.livewallpaper.wallpaper.WallpaperConfig
import com.example.livewallpaper.wallpaper.WallpaperRenderer
import kotlin.math.min

/**
 * Slow drifting multi-stop gradient with drifting soft orbs.
 * Cheapest renderer: 1 gradient + a few radial blobs per frame, no bitmaps.
 */
class GradientFlowRenderer : WallpaperRenderer {

    private var width = 1
    private var height = 1
    private var xOffset = 0f
    private var gradient: LinearGradient? = null
    private var gradientKey = ""

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Orb(
        val fx: Float, val fy: Float, val radiusFrac: Float,
        val color: Int, val dx: Float, val dy: Float, val phase: Float
    )

    // Deterministic orbs; positions derived from index so any surface size works.
    private val orbs = listOf(
        Orb(0.2f, 0.3f, 0.45f, 0x59EC4899.toInt(), 0.05f, 0.03f, 0f),
        Orb(0.8f, 0.25f, 0.38f, 0x5906B6D4.toInt(), -0.04f, 0.05f, 2f),
        Orb(0.6f, 0.8f, 0.5f, 0x597C3AED.toInt(), 0.03f, -0.04f, 4f)
    )

    override fun onSurfaceChanged(width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        gradient = null // rebuilt lazily with current colors
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        this.xOffset = xOffset.coerceIn(0f, 1f)
    }

    override fun drawFrame(canvas: Canvas, elapsedNanos: Long, config: WallpaperConfig) {
        val t = (elapsedNanos / 1_000_000_000.0 * config.speedMultiplier.toDouble()).toFloat()
        ensureGradient(config)

        gradient?.let { bgPaint.shader = it }
        bgPaint.alpha = 255
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        bgPaint.shader = null

        if (config.amoledDark) {
            // Deepen blacks with a translucent black wash for AMOLED savings.
            canvas.drawColor(0x40000000)
        }

        val parallax = if (config.parallaxEnabled) (xOffset - 0.5f) * width * 0.12f else 0f
        val minDim = min(width, height).toFloat()
        for (orb in orbs) {
            val ox = (orb.fx + orb.dx * kotlin.math.sin(t * 0.25f + orb.phase)) * width + parallax
            val oy = (orb.fy + orb.dy * kotlin.math.cos(t * 0.2f + orb.phase)) * height
            val r = orb.radiusFrac * minDim
            orbPaint.color = orb.color
            canvas.drawCircle(ox, oy, r, orbPaint)
        }
    }

    private fun ensureGradient(config: WallpaperConfig) {
        val key = config.baseColors.joinToString(",") + "|$width|$height"
        if (gradient == null || key != gradientKey) {
            gradientKey = key
            val colors = config.baseColors.toIntArray()
            gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
        }
    }
}
