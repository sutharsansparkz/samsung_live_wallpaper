package com.example.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.livewallpaper.settings.SettingsViewModel
import com.example.livewallpaper.settings.WallpaperPreferencesRepository
import com.example.livewallpaper.ui.LiveWallpaperTheme
import com.example.livewallpaper.ui.SettingsScreen
import com.example.livewallpaper.wallpaper.LiveWallpaperService

/**
 * Configuration Activity.
 *
 * This screen only edits persisted settings and fires the system wallpaper
 * intent — it is NOT the wallpaper. Rendering happens in
 * [LiveWallpaperService], which keeps running after this Activity closes,
 * after reboot (system re-binds it), and while the screen state changes.
 *
 * Also registered as `settingsActivity` in res/xml/wallpaper.xml, so the
 * system "Settings…" button in the live-wallpaper preview opens this screen.
 */
class MainActivity : ComponentActivity() {

    private val prefs by lazy { WallpaperPreferencesRepository.get(this) }
    private val vm: SettingsViewModel by viewModels { SettingsViewModel.Factory(prefs) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveWallpaperTheme {
                SettingsScreen(
                    viewModel = vm,
                    onSetWallpaper = ::openLiveWallpaperChooser,
                    onOpenPicker = ::openSystemPicker
                )
            }
        }
    }

    /**
     * Deep-links straight to this wallpaper's preview ("Set wallpaper" flow),
     * the standard ACTION_CHANGE_LIVE_WALLPAPER pattern. Falls back to the
     * generic picker on devices that ignore the component extra (some
     * heavily skinned builds).
     */
    private fun openLiveWallpaperChooser() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, LiveWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Opening wallpaper picker…", Toast.LENGTH_SHORT).show()
            openSystemPicker()
        }
    }

    private fun openSystemPicker() {
        try {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Open Settings → Wallpaper to select Galaxy Live Wallpaper",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
