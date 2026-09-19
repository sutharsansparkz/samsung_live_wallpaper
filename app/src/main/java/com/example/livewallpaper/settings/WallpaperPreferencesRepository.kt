package com.example.livewallpaper.settings

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.livewallpaper.wallpaper.WallpaperConfig
import com.example.livewallpaper.wallpaper.WallpaperType
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
 * collect [config]; the engine therefore picks up changes live, even while it
 * keeps running after the Activity is closed.
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

    suspend fun setType(type: WallpaperType) {
        appContext.wallpaperDataStore.edit { it[Keys.TYPE] = type.name }
    }

    suspend fun setParticleCount(count: Int) {
        appContext.wallpaperDataStore.edit {
            it[Keys.PARTICLE_COUNT] = count.coerceIn(
                WallpaperConfig.MIN_PARTICLES,
                WallpaperConfig.MAX_PARTICLES
            )
        }
    }

    suspend fun setSpeed(speed: Float) {
        appContext.wallpaperDataStore.edit {
            it[Keys.SPEED] = speed.coerceIn(WallpaperConfig.MIN_SPEED, WallpaperConfig.MAX_SPEED)
        }
    }

    suspend fun setParallaxEnabled(enabled: Boolean) {
        appContext.wallpaperDataStore.edit { it[Keys.PARALLAX] = enabled }
    }

    suspend fun setTouchEnabled(enabled: Boolean) {
        appContext.wallpaperDataStore.edit { it[Keys.TOUCH] = enabled }
    }

    suspend fun setFpsLimit(fps: Int) {
        appContext.wallpaperDataStore.edit {
            it[Keys.FPS] = if (fps in WallpaperConfig.FPS_OPTIONS) fps else 60
        }
    }

    suspend fun setBatterySaver(enabled: Boolean) {
        appContext.wallpaperDataStore.edit { it[Keys.BATTERY_SAVER] = enabled }
    }

    suspend fun setAmoledDark(enabled: Boolean) {
        appContext.wallpaperDataStore.edit { it[Keys.AMOLED_DARK] = enabled }
    }

    suspend fun setBaseColors(argb: List<Int>) {
        appContext.wallpaperDataStore.edit {
            it[Keys.COLORS] = argb.take(4).joinToString(",")
        }
    }

    private object Keys {
        val TYPE = stringPreferencesKey("type")
        val PARTICLE_COUNT = intPreferencesKey("particle_count")
        val SPEED = floatPreferencesKey("speed")
        val PARALLAX = booleanPreferencesKey("parallax")
        val TOUCH = booleanPreferencesKey("touch")
        val FPS = intPreferencesKey("fps")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val AMOLED_DARK = booleanPreferencesKey("amoled_dark")
        val COLORS = stringPreferencesKey("colors_csv")
    }

    private fun androidx.datastore.preferences.core.Preferences.toConfig(): WallpaperConfig {
        val defaults = WallpaperConfig.DEFAULT
        val colors = get(Keys.COLORS)
            ?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size >= 2 }
            ?: defaults.baseColors
        return WallpaperConfig(
            type = WallpaperType.fromName(get(Keys.TYPE)),
            particleCount = get(Keys.PARTICLE_COUNT) ?: defaults.particleCount,
            speedMultiplier = get(Keys.SPEED) ?: defaults.speedMultiplier,
            parallaxEnabled = get(Keys.PARALLAX) ?: defaults.parallaxEnabled,
            touchInteractionEnabled = get(Keys.TOUCH) ?: defaults.touchInteractionEnabled,
            fpsLimit = get(Keys.FPS) ?: defaults.fpsLimit,
            batterySaver = get(Keys.BATTERY_SAVER) ?: defaults.batterySaver,
            amoledDark = get(Keys.AMOLED_DARK) ?: defaults.amoledDark,
            baseColors = colors
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
