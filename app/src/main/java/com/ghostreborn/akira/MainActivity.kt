package com.ghostreborn.akira

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.screens.AnimeDetailScreen
import com.ghostreborn.akira.features.home.screens.HomeScreen
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.screens.PlayerScreen
import com.ghostreborn.akira.features.settings.screens.SettingsScreen
import com.ghostreborn.akira.ui.theme.AkiraBackground
import com.ghostreborn.akira.ui.theme.AkiraTheme

// Shared holder for transient navigation arguments like stream sources list
object NavHolder {
    var pendingSources: List<StreamSource> = emptyList()
    var pendingInitialPositionMs: Long = 0L
    var pendingThumbnail: String? = null
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        enableEdgeToEdge()

        setContent {
            AkiraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AkiraBackground
                ) {
                    AkiraAppNavigation()
                }
            }
        }
    }
}

@Composable
fun AkiraAppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val isLoggedIn = remember { TokenManager.isLoggedIn(context) }
    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- 1. LOGIN SCREEN ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // --- 2. HOME SCREEN ---
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { anime ->
                    val encodedTitle = Uri.encode(anime.name)
                    val encodedPoster = Uri.encode(anime.thumbnail ?: "")
                    navController.navigate("detail/${anime.id}?title=$encodedTitle&poster=$encodedPoster")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // --- 3. ANIME DETAIL SCREEN ---
        composable(
            route = "detail/{animeId}?title={title}&poster={poster}",
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("poster") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
            val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) }
            val poster = backStackEntry.arguments?.getString("poster")?.let { Uri.decode(it) }

            AnimeDetailScreen(
                animeId = animeId,
                initialTitle = title,
                initialPoster = poster,
                onNavigateBack = { navController.popBackStack() },
                onPlayEpisode = { epNumber, sources, startMs ->
                    NavHolder.pendingSources = sources
                    NavHolder.pendingInitialPositionMs = startMs
                    NavHolder.pendingThumbnail = poster
                    val encodedTitle = Uri.encode(title ?: "Anime")
                    navController.navigate("player/$animeId/$epNumber?title=$encodedTitle")
                }
            )
        }

        // --- 4. VIDEO PLAYER SCREEN ---
        composable(
            route = "player/{animeId}/{episodeNumber}?title={title}",
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("episodeNumber") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "Anime"
                }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
            val episodeNumber = backStackEntry.arguments?.getString("episodeNumber") ?: "1"
            val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) } ?: "Anime"
            val sources = NavHolder.pendingSources
            val initialMs = NavHolder.pendingInitialPositionMs
            val thumb = NavHolder.pendingThumbnail

            PlayerScreen(
                animeId = animeId,
                animeTitle = title,
                episodeNumber = episodeNumber,
                sources = sources,
                thumbnail = thumb,
                initialPositionMs = initialMs,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- 5. SETTINGS SCREEN ---
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
