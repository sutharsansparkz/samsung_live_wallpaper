package com.example.livewallpaper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** App theme for the settings Activity only (the wallpaper draws its own colors). */
@Composable
fun LiveWallpaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
