package com.example.livewallpaper.wallpaper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * All wallpaper styles the service can render.
 *
 * To add a new wallpaper type:
 * 1. Add an enum entry here.
 * 2. Add a [WallpaperRenderer] implementation.
 * 3. Register it in `RendererFactory.create()`.
 * No changes to [LiveWallpaperService] or [WallpaperEngine] are required.
 */
enum class WallpaperType(val title: String) {
    GRADIENT_FLOW("Gradient flow"),
    PARTICLE_GALAXY("Particle galaxy"),
    AURORA_WAVES("Aurora waves"),
    VIDEO("Video");

    companion object {
        fun fromName(name: String?): WallpaperType =
            entries.firstOrNull { it.name == name } ?: GRADIENT_FLOW
    }
}

/**
 * Immutable snapshot of every user-tunable wallpaper option.
 * Persisted in DataStore (see WallpaperPreferencesRepository) and delivered
 * to the engine as a StateFlow so the running wallpaper updates live without
 * restarting the service.
 */
data class WallpaperConfig(
    val type: WallpaperType = WallpaperType.GRADIENT_FLOW,
    /** ARGB colors forming the background gradient. */
    val baseColors: List<Int> = listOf(
        Color(0xFF0B1026).toArgb(),
        Color(0xFF312E81).toArgb(),
        Color(0xFF0E7490).toArgb()
    ),
    val particleCount: Int = 90,
    /** 0.1 (calm) .. 3.0 (energetic). */
    val speedMultiplier: Float = 1.0f,
    /** Parallax shift driven by home-screen scroll (One UI sends xOffset). */
    val parallaxEnabled: Boolean = true,
    /** Ripple/burst on double-tap (touch events are opt-in). */
    val touchInteractionEnabled: Boolean = true,
    /** 15 / 30 / 60. Lower = less battery. */
    val fpsLimit: Int = 60,
    /** Caps FPS at 30 and halves particles on low-end / power-save devices. */
    val batterySaver: Boolean = false,
    /** Pure-black background regions for AMOLED power savings. */
    val amoledDark: Boolean = true,
    /**
     * Content URI (as String) of the user-picked video for [WallpaperType.VIDEO].
     * Obtained via Storage Access Framework with a persisted read grant, so the
     * wallpaper service can open it even after reboot without any permission.
     */
    val videoUri: String? = null,
    /** Wallpapers pause when hidden, but when visible a VIDEO wallpaper can opt into sound. */
    val videoMuted: Boolean = true
) {
    /** Effective frame budget after battery-saver adjustments. */
    val effectiveFps: Int get() = if (batterySaver) minOf(fpsLimit, 30) else fpsLimit

    /** Effective particle budget after battery-saver adjustments. */
    val effectiveParticleCount: Int get() = if (batterySaver) particleCount / 2 else particleCount

    companion object {
        const val MIN_PARTICLES = 0
        const val MAX_PARTICLES = 300
        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 3.0f
        val FPS_OPTIONS = listOf(15, 30, 60)

        val DEFAULT = WallpaperConfig()
    }
}
