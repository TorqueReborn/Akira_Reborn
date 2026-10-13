package com.ghostreborn.akira.features.player.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Manages an on-disk cache for streaming media segments.
 * Prevents re-downloading watched chunks and allows continuous background buffering
 * without Out-Of-Memory (OOM) risks.
 */
@OptIn(UnstableApi::class)
object VideoCacheManager {
    private const val MAX_CACHE_SIZE_BYTES = 1024L * 1024L * 1024L // 1 GB cache limit
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getInstance(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    @Synchronized
    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}
