package com.example.livewallpaper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/**
 * Configuration UI. This is a normal screen for picking the video and
 * editing settings — the wallpaper itself keeps rendering in
 * [com.example.livewallpaper.wallpaper.LiveWallpaperService]
 * regardless of whether this screen is open.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSetWallpaper: () -> Unit,
    onPickVideo: () -> Unit,
    onHideIcon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .settingsRoot()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Video Live Wallpaper",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Pick a video and set it as your home-screen wallpaper. " +
                "It loops automatically and pauses when hidden.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            "Your video",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedButton(
            onClick = onPickVideo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (config.videoUri == null) "Pick a video" else "Change video")
        }
        Text(
            rememberVideoName(config.videoUri)
                ?: "No video selected — the wallpaper stays black until you pick one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mute video",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(checked = config.videoMuted, onCheckedChange = viewModel::setVideoMuted)
        }

        Spacer(Modifier.height(4.dp))

        Button(onClick = onSetWallpaper, modifier = Modifier.fillMaxWidth()) {
            Text("Set as wallpaper")
        }
        OutlinedButton(onClick = onHideIcon, modifier = Modifier.fillMaxWidth()) {
            Text("Hide app icon")
        }
        Text(
            "Hiding removes the icon from the launcher. You can still open " +
                "these settings from the live-wallpaper preview screen " +
                "(Wallpaper → Video Live Wallpaper → Settings).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Samsung tip: after setting, long-press the home screen → Wallpaper " +
                "to move it between Home and Lock screens where One UI allows it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
