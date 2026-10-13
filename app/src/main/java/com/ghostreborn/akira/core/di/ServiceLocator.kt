package com.ghostreborn.akira.core.di

import android.content.Context
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.home.services.DefaultAnimeRepository
import com.ghostreborn.akira.features.updater.services.UpdateManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Lightweight Service Locator for managing app-wide singleton dependencies.
 * Facilitates dependency injection for ViewModels and enables clean mocking in unit tests.
 */
object ServiceLocator {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    var animeRepository: AnimeRepository = DefaultAnimeRepository(okHttpClient)

    fun getUpdateManager(context: Context): UpdateManager {
        return UpdateManager.getInstance(context)
    }

    /**
     * Resets dependencies to defaults (useful for unit testing).
     */
    fun reset() {
        animeRepository = DefaultAnimeRepository(okHttpClient)
    }
}
