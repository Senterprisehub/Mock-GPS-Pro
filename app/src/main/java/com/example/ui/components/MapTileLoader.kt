package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

enum class MapTileProvider(
    val displayName: String,
    val template: String,
    val iconLabel: String = "🗺️",
    val maxZoom: Int = 20
) {
    GOOGLE_STREETS(
        displayName = "🗺️ Google Maps (Streets)",
        template = "https://mt{s_digit}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        iconLabel = "🗺️",
        maxZoom = 21
    ),
    GOOGLE_HYBRID(
        displayName = "🌐 Google Hybrid (Satellite + Roads)",
        template = "https://mt{s_digit}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
        iconLabel = "🌐",
        maxZoom = 21
    ),
    GOOGLE_SATELLITE(
        displayName = "🛰️ Google Satellite (Pure)",
        template = "https://mt{s_digit}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
        iconLabel = "🛰️",
        maxZoom = 21
    ),
    GOOGLE_TERRAIN(
        displayName = "🏔️ Google Terrain (Topography)",
        template = "https://mt{s_digit}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}",
        iconLabel = "🏔️",
        maxZoom = 18
    )
}

object MapTileLoader {
    private var imageLoaderInstance: ImageLoader? = null
    private val subdomains = listOf("a", "b", "c", "d")

    @Synchronized
    fun getImageLoader(context: Context): ImageLoader {
        imageLoaderInstance?.let { return it }

        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        }

        val headerInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("User-Agent", "MockGpsPro/3.6 (Android; Mobile Map Client)")
                .header("Referer", "https://www.google.com/maps")
                .header("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            chain.proceed(newRequest)
        }

        val cacheDir = File(context.applicationContext.cacheDir, "okhttp_tiles_cache")
        val okHttpCache = Cache(cacheDir, 256L * 1024 * 1024) // 256 MB OkHttp Cache

        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .cache(okHttpCache)
            .addInterceptor(headerInterceptor)
            .retryOnConnectionFailure(true)
            .build()

        val loader = ImageLoader.Builder(context.applicationContext)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context.applicationContext)
                    .maxSizePercent(0.12)
                    .strongReferencesEnabled(false)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.applicationContext.cacheDir.resolve("map_tiles_cache"))
                    .maxSizeBytes(128L * 1024 * 1024)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(false)
            .build()

        imageLoaderInstance = loader
        return loader
    }

    fun buildTileUrl(provider: MapTileProvider, z: Int, x: Int, y: Int): String {
        val effectiveZ = z.coerceIn(0, provider.maxZoom)
        val numTiles = 1 shl effectiveZ
        val wrappedX = ((x % numTiles) + numTiles) % numTiles
        val clampedY = y.coerceIn(0, (numTiles - 1).coerceAtLeast(0))

        val sub = subdomains[Math.abs(wrappedX + clampedY) % subdomains.size]
        val subDigit = Math.abs(wrappedX + clampedY) % 4

        return provider.template
            .replace("{s}", sub)
            .replace("{s_digit}", subDigit.toString())
            .replace("{z}", effectiveZ.toString())
            .replace("{x}", wrappedX.toString())
            .replace("{y}", clampedY.toString())
    }
}

