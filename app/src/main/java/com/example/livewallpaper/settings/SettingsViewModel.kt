package com.example.livewallpaper.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.livewallpaper.wallpaper.WallpaperConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Bridges the Compose settings UI and [WallpaperPreferencesRepository].
 * Every setter writes to DataStore; the running wallpaper observes the same
 * flow and updates live.
 */
class SettingsViewModel(
    private val repo: WallpaperPreferencesRepository
) : ViewModel() {

    val config: StateFlow<WallpaperConfig> = repo.config

    fun setVideoUri(uriString: String?) = viewModelScope.launch { repo.setVideoUri(uriString) }
    fun setVideoMuted(muted: Boolean) = viewModelScope.launch { repo.setVideoMuted(muted) }

    class Factory(
        private val repo: WallpaperPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repo) as T
        }
    }
}
