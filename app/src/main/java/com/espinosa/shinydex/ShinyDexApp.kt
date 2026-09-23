package com.espinosa.shinydex

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.espinosa.shinydex.data.remote.ApiClient

class ShinyDexApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Sprites are fetched through the same hardened OkHttp client as the API. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { ApiClient.okHttpClient }))
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("sprite_cache"))
                    .maxSizeBytes(SPRITE_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        const val SPRITE_CACHE_BYTES = 64L * 1024 * 1024
    }
}
