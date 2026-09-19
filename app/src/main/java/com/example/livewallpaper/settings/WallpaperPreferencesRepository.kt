package com.example.livewallpaper.settings

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.livewallpaper.wallpaper.WallpaperConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.wallpaperDataStore by preferencesDataStore(
    name = "wallpaper_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/**
 * Single source of truth for wallpaper settings, backed by DataStore.
 *
 * Both the settings Activity and [com.example.livewallpaper.wallpaper.WallpaperEngine]
 * collect [config]; the engine therefore picks up a newly chosen video live,
 * even while it keeps running after the Activity is closed.
 */
class WallpaperPreferencesRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val config: StateFlow<WallpaperConfig> =
        appContext.wallpaperDataStore.data
            .catch { e ->
                // Corrupt or unreadable prefs: fall back to defaults instead of crashing
                // the wallpaper (which would leave the user with a black screen).
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs -> prefs.toConfig() }
            .stateIn(scope, SharingStarted.Eagerly, WallpaperConfig.DEFAULT)

    suspend fun setVideoUri(uriString: String?) {
        appContext.wallpaperDataStore.edit {
            if (uriString.isNullOrBlank()) it.remove(Keys.VIDEO_URI)
            else it[Keys.VIDEO_URI] = uriString
        }
    }

    suspend fun setVideoMuted(muted: Boolean) {
        appContext.wallpaperDataStore.edit { it[Keys.VIDEO_MUTED] = muted }
    }

    private object Keys {
        val VIDEO_URI = stringPreferencesKey("video_uri")
        val VIDEO_MUTED = booleanPreferencesKey("video_muted")
    }

    private fun androidx.datastore.preferences.core.Preferences.toConfig(): WallpaperConfig {
        val defaults = WallpaperConfig.DEFAULT
        return WallpaperConfig(
            videoUri = get(Keys.VIDEO_URI),
            videoMuted = get(Keys.VIDEO_MUTED) ?: defaults.videoMuted
        )
    }

    companion object {
        @Volatile
        private var instance: WallpaperPreferencesRepository? = null

        fun get(context: Context): WallpaperPreferencesRepository =
            instance ?: synchronized(this) {
                instance ?: WallpaperPreferencesRepository(context).also { instance = it }
            }
    }
}
