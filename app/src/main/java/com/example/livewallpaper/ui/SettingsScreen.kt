package com.example.livewallpaper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.livewallpaper.settings.SettingsViewModel
import com.example.livewallpaper.wallpaper.WallpaperConfig
import com.example.livewallpaper.wallpaper.WallpaperType
import kotlin.math.roundToInt

/**
 * Configuration UI. This is a normal screen for editing DataStore settings —
 * the wallpaper itself keeps rendering in [com.example.livewallpaper.wallpaper.LiveWallpaperService]
 * regardless of whether this screen is open.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSetWallpaper: () -> Unit,
    onOpenPicker: () -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Galaxy Live Wallpaper", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pick a style, tune it, then set it as your wallpaper. " +
                "Changes apply live — even after this screen is closed.",
            style = MaterialTheme.typography.bodyMedium
        )

        SettingSection("Style") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WallpaperType.entries.forEach { type ->
                    FilterChip(
                        selected = config.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.title) }
                    )
                }
            }
        }

        if (config.type == WallpaperType.VIDEO) {
            SettingSection("Video") {
                val videoName = rememberVideoName(config.videoUri)
                OutlinedButton(
                    onClick = onPickVideo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (config.videoUri == null) "Pick a video" else "Change video")
                }
                Text(
                    videoName ?: "No video selected — the wallpaper stays black until you pick one.",
                    style = MaterialTheme.typography.bodySmall
                )
                SwitchRow("Mute video", config.videoMuted, viewModel::setVideoMuted)
            }
        }

        SettingSection("Speed  (${String.format("%.1fx", config.speedMultiplier)})") {
            Slider(
                value = config.speedMultiplier,
                onValueChange = { viewModel.setSpeed(it) },
                valueRange = WallpaperConfig.MIN_SPEED..WallpaperConfig.MAX_SPEED
            )
        }

        SettingSection("Particles  (${config.particleCount})") {
            Slider(
                value = config.particleCount.toFloat(),
                onValueChange = { viewModel.setParticleCount(it.roundToInt()) },
                valueRange = WallpaperConfig.MIN_PARTICLES.toFloat()..
                    WallpaperConfig.MAX_PARTICLES.toFloat(),
                steps = 11
            )
        }

        SettingSection("Frame rate") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WallpaperConfig.FPS_OPTIONS.forEach { fps ->
                    FilterChip(
                        selected = config.fpsLimit == fps,
                        onClick = { viewModel.setFps(fps) },
                        label = { Text("$fps fps") }
                    )
                }
            }
            if (config.batterySaver) {
                Text(
                    "Battery saver caps rendering at 30 fps.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SettingSection("Behaviour") {
            SwitchRow("Parallax on swipe", config.parallaxEnabled, viewModel::setParallax)
            SwitchRow("Touch ripples", config.touchInteractionEnabled, viewModel::setTouch)
            SwitchRow("AMOLED dark blacks", config.amoledDark, viewModel::setAmoledDark)
            SwitchRow("Battery saver", config.batterySaver, viewModel::setBatterySaver)
        }

        Spacer(Modifier.height(4.dp))

        Button(onClick = onSetWallpaper, modifier = Modifier.fillMaxWidth()) {
            Text("Set as wallpaper")
        }
        OutlinedButton(onClick = onOpenPicker, modifier = Modifier.fillMaxWidth()) {
            Text("Open system wallpaper picker")
        }

        Text(
            "Samsung tip: after setting, long-press the home screen → Wallpaper " +
                "to move it between Home and Lock screens where One UI allows it.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Resolves a display name for the picked video URI, or null when unset/unreadable. */
@Composable
private fun rememberVideoName(uriString: String?): String? {
    val context = LocalContext.current
    return remember(uriString) {
        if (uriString.isNullOrBlank()) {
            null
        } else {
            try {
                val uri = android.net.Uri.parse(uriString)
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else uri.lastPathSegment
                } ?: android.net.Uri.parse(uriString).lastPathSegment
            } catch (_: Exception) {
                null
            }
        }
    }
}
