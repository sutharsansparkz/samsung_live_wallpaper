# Keep the WallpaperService entry point; it is referenced from AndroidManifest.
-keep public class com.example.livewallpaper.wallpaper.LiveWallpaperService
-keepclassmembers class com.example.livewallpaper.wallpaper.LiveWallpaperService {
    public <methods>;
}
