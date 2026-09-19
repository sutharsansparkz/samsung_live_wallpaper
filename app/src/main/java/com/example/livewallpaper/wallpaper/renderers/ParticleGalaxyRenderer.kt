package com.example.livewallpaper.wallpaper.renderers

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.MotionEvent
import com.example.livewallpaper.wallpaper.WallpaperConfig
import com.example.livewallpaper.wallpaper.WallpaperRenderer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Starfield / galaxy: N particles drift and twinkle over a dark gradient.
 * Zero allocation in [drawFrame]: fixed-size pool reused across frames.
 */
class ParticleGalaxyRenderer(seed: Long = 42L) : WallpaperRenderer {

    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var radius: Float, var phase: Float, var speed: Float
    )

    private val random = Random(seed)
    private var width = 1
    private var height = 1
    private var xOffset = 0f
    private val ripples = ArrayDeque<Ripple>(8)

    private data class Ripple(var x: Float, var y: Float, var age: Float)

    private var particles = emptyArray<Particle>()
    private val bgPaint = Paint()
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private var gradient: Shader? = null
    private var gradientKey = ""

    override fun onSurfaceChanged(width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        gradient = null
        if (particles.isEmpty()) initParticles(120)
        // Re-project existing particles into the new bounds.
        for (p in particles) {
            p.x = random.nextFloat() * this.width
            p.y = random.nextFloat() * this.height
        }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        this.xOffset = xOffset.coerceIn(0f, 1f)
    }

    override fun onTouch(event: MotionEvent, width: Int, height: Int) {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            if (ripples.size >= 8) ripples.removeFirst()
            ripples.addLast(Ripple(event.x, event.y, 0f))
        }
    }

    override fun drawFrame(canvas: Canvas, elapsedNanos: Long, config: WallpaperConfig) {
        val t = (elapsedNanos / 1_000_000_000.0 * config.speedMultiplier).toFloat()
        ensureGradient(config)
        bgPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        bgPaint.shader = null

        ensureParticleBudget(config.effectiveParticleCount)

        val parallax = if (config.parallaxEnabled) (xOffset - 0.5f) * width * 0.2f else 0f
        val dt = 1f / config.effectiveFps.coerceAtLeast(15)

        for (p in particles) {
            p.x += p.vx * p.speed * config.speedMultiplier * dt * 60f
            p.y += p.vy * p.speed * config.speedMultiplier * dt * 60f
            if (p.x < 0) p.x += width else if (p.x > width) p.x -= width
            if (p.y < 0) p.y += height else if (p.y > height) p.y -= height

            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * 2f + p.phase))
            starPaint.alpha = (255 * twinkle).toInt().coerceIn(0, 255)
            starPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(p.x + parallax * p.speed, p.y, p.radius, starPaint)
        }

        // Touch ripples expand and fade; aged out after 1s.
        val it = ripples.iterator()
        while (it.hasNext()) {
            val r = it.next()
            r.age += dt
            if (r.age > 1f) {
                it.remove()
                continue
            }
            ripplePaint.alpha = (200 * (1f - r.age)).toInt()
            ripplePaint.color = 0xFF67E8F9.toInt()
            canvas.drawCircle(r.x, r.y, 20f + r.age * 260f, ripplePaint)
        }
    }

    private fun ensureGradient(config: WallpaperConfig) {
        val key = config.baseColors.joinToString(",") + "|$width|$height|" + config.amoledDark
        if (gradient == null || key != gradientKey) {
            gradientKey = key
            val top = if (config.amoledDark) 0xFF000000.toInt() else config.baseColors.firstOrNull()
                ?: 0xFF0B1026.toInt()
            val bottom = config.baseColors.lastOrNull() ?: 0xFF0E7490.toInt()
            gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(top, bottom), null, Shader.TileMode.CLAMP
            )
        }
    }

    private fun ensureParticleBudget(wanted: Int) {
        if (particles.size == wanted) return
        val kept = particles.take(wanted)
        particles = if (kept.size >= wanted) {
            kept.toTypedArray()
        } else {
            kept.toMutableList().also { list ->
                repeat(wanted - list.size) { list.add(newParticle()) }
            }.toTypedArray()
        }
    }

    private fun initParticles(n: Int) {
        particles = Array(n) { newParticle() }
    }

    private fun newParticle(): Particle {
        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
        val speed = 0.2f + random.nextFloat() * 0.9f
        return Particle(
            x = random.nextFloat() * width,
            y = random.nextFloat() * height,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            radius = 1f + random.nextFloat() * 2.6f,
            phase = random.nextFloat() * 6.28f,
            speed = speed
        )
    }
}
