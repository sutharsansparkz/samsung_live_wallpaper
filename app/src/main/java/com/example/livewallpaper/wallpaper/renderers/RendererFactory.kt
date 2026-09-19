package com.example.livewallpaper.wallpaper.renderers

import com.example.livewallpaper.wallpaper.WallpaperRenderer
import com.example.livewallpaper.wallpaper.WallpaperType

/**
 * Central registry mapping [WallpaperType] -> [WallpaperRenderer].
 *
 * Extension point: to add a new wallpaper, implement [WallpaperRenderer],
 * add an enum entry, and add one line here. The engine and service
 * automatically pick it up, including live-switching via recreateRenderer().
 */
object RendererFactory {
    fun create(type: WallpaperType): WallpaperRenderer = when (type) {
        WallpaperType.GRADIENT_FLOW -> GradientFlowRenderer()
        WallpaperType.PARTICLE_GALAXY -> ParticleGalaxyRenderer()
        WallpaperType.AURORA_WAVES -> AuroraWavesRenderer()
    }
}
