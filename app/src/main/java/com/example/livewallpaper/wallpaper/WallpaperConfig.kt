package com.example.livewallpaper.wallpaper

/**
 * Immutable snapshot of the wallpaper settings.
 * Persisted in DataStore (see WallpaperPreferencesRepository) and delivered
 * to the engine as a StateFlow so the running wallpaper updates live without
 * restarting the service.
 */
data class WallpaperConfig(
    /**
     * Content URI (as String) of the user-picked video.
     * Obtained via Storage Access Framework with a persisted read grant, so the
     * wallpaper service can open it even after reboot without any permission.
     */
    val videoUri: String? = null,
    /** Wallpapers pause when hidden, but while visible sound is optional. */
    val videoMuted: Boolean = true
) {
    companion object {
        val DEFAULT = WallpaperConfig()
    }
}
