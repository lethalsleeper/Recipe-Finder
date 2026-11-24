package com.example.recipefinder

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.recipefinder.di.AppContainer

class RecipeFinderApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Coil's app-wide image loader.
     *
     * Images are kept in a persistent disk cache so recipe photos that were shown once still
     * load offline. `respectCacheHeaders(false)` matters here: it tells Coil to serve the cached
     * file even if the server's HTTP headers say it has expired, which is what makes images
     * available without a connection. The cache lives in the app's cache directory, so no
     * storage permission is needed.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { container.okHttpClient }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("recipe_images"))
                    .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()

    private companion object {
        const val IMAGE_DISK_CACHE_BYTES = 150L * 1024 * 1024 // 150 MB
    }
}
