package com.example.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
 * system "Settings…" button in the live-wallpaper preview opens this screen
 * — even when the launcher icon is hidden.
 */
class MainActivity : ComponentActivity() {

    private val prefs by lazy { WallpaperPreferencesRepository.get(this) }
    private val vm: SettingsViewModel by viewModels { SettingsViewModel.Factory(prefs) }

    /**
     * Storage Access Framework picker. No storage permission needed: the
     * returned URI carries a read grant, which we persist so the wallpaper
     * service can open the video after reboot too.
     */
    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Provider doesn't support persisted grants; the URI still works
            // until the device reboots.
        }
        vm.setVideoUri(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent system bars with theme-aware icons; the Compose layout
        // pads itself below them (see settingsRoot) so nothing collides.
        enableEdgeToEdge()
        setContent {
            LiveWallpaperTheme {
                SettingsScreen(
                    viewModel = vm,
                    onSetWallpaper = ::openLiveWallpaperChooser,
                    onPickVideo = { pickVideo.launch(arrayOf("video/*")) },
                    onHideIcon = ::hideLauncherIcon
                )
            }
        }
    }

    /**
     * Deep-links straight to this wallpaper's preview ("Set wallpaper" flow),
     * the standard ACTION_CHANGE_LIVE_WALLPAPER pattern.
     */
    private fun openLiveWallpaperChooser() {
        if (vm.config.value.videoUri.isNullOrBlank()) {
            Toast.makeText(this, "Pick a video first", Toast.LENGTH_SHORT).show()
            pickVideo.launch(arrayOf("video/*"))
            return
        }
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, LiveWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Open Settings → Wallpaper to select Video Live Wallpaper",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Hides the app icon from the launcher / app drawer.
     *
     * Only the launcher *alias* is disabled — MainActivity itself stays
     * enabled, so the wallpaper preview's Settings button keeps working.
     * There is no in-app "unhide": reopen settings via
     * Wallpaper → Video Live Wallpaper → Settings.
     */
    private fun hideLauncherIcon() {
        try {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, "$packageName.LauncherAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Toast.makeText(
                this,
                "App icon hidden. Reopen settings from the wallpaper preview → Settings.",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Could not hide the icon on this launcher", Toast.LENGTH_SHORT).show()
        }
    }
}
