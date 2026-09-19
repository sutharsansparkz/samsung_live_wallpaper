package com.example.livewallpaper

import android.app.Application
import com.example.livewallpaper.settings.WallpaperPreferencesRepository

/**
 * Initializes the DataStore-backed settings singleton early so both the
 * settings UI and the wallpaper service share one StateFlow source of truth.
 */
class LiveWallpaperApplication : Application() {
    val prefs: WallpaperPreferencesRepository by lazy {
        WallpaperPreferencesRepository.get(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Touch the lazy repo so Eagerly-shared config starts collecting even
        // if the service (not the Activity) is the first component created,
        // e.g. right after reboot when the system re-binds the wallpaper.
        prefs.config.value
    }
}
