package com.ghostreborn.akira

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Color
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.network.CaptchaChallengeManager
import com.ghostreborn.akira.features.auth.screens.LoginScreen
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.auth.widgets.CloudflareVerificationDialog
import com.ghostreborn.akira.features.downloads.screens.DownloadsScreen
import com.ghostreborn.akira.features.downloads.services.EpisodeDownloadManager
import com.ghostreborn.akira.features.home.screens.AnimeDetailScreen
import com.ghostreborn.akira.features.home.screens.GuideDetailScreen
import com.ghostreborn.akira.features.home.screens.HomeScreen
import com.ghostreborn.akira.features.home.screens.MusicScreen
import com.ghostreborn.akira.features.home.screens.ScheduleScreen
import com.ghostreborn.akira.features.home.screens.WatchlistScreen
import com.ghostreborn.akira.features.home.screens.WorldMapViewerScreen
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.manga.presentation.reader.MangaReaderScreen
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.screens.PlayerScreen
import com.ghostreborn.akira.features.player.services.PlayerSessionManager
import com.ghostreborn.akira.features.settings.screens.SettingsScreen
import com.ghostreborn.akira.features.updater.services.UpdateManager
import com.ghostreborn.akira.features.updater.widgets.UpdateDialog
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        TokenManager.init(this)
        EpisodeDownloadManager.init(this)
        AnimeRepository.loadContentPreferences(this)
        val updateManager = UpdateManager.getInstance(this)
        updateManager.checkForUpdates(isManual = false)

        // Pre-warm dynamic crypto config asynchronously so login and feed loads don't lag
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MkissaConfigProvider.getConfig(applicationContext)
            } catch (_: Exception) {
                // Background warmup failure is non-fatal; fallbacks exist
            }
        }

        setContent {
            AkiraTheme {
                val updateState = updateManager.uiState.collectAsState().value
                val captchaChallenge = CaptchaChallengeManager.challenge.collectAsState().value

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkiraAppNavigation()

                    if (captchaChallenge != null) {
                        CloudflareVerificationDialog(
                            onTokenExtracted = { token ->
                                CaptchaChallengeManager.submitSolution(
                                    token = token,
                                    provider = MkissaConfigProvider.currentEndpoints.captchaProvider
                                )
                            },
                            onDismiss = { CaptchaChallengeManager.cancelChallenge() }
                        )
                    }

                    UpdateDialog(
                        state = updateState,
                        onStartDownload = { release -> updateManager.startDownload(release) },
                        onCancelDownload = { updateManager.cancelDownload() },
                        onInstall = { apkFile -> updateManager.installApk(apkFile) },
                        onDismiss = { updateManager.clearState() },
                        onDismissUpdateVersion = { release -> updateManager.dismissUpdate(release) }
                    )
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
    val startDestination = if (isLoggedIn) "home?query={query}" else "login"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentDestination != null && (
        currentDestination.startsWith("home") ||
        currentDestination == "watchlist" ||
        currentDestination == "schedule" ||
        currentDestination == "downloads"
    )

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val navItems = remember {
                        listOf(
                            Triple("home", "Home", Icons.Rounded.Home),
                            Triple("watchlist", "Watchlist", Icons.Rounded.Bookmark),
                            Triple("schedule", "Schedule", Icons.Rounded.CalendarToday),
                            Triple("downloads", "Downloads", Icons.Rounded.Download)
                        )
                    }

                    val selectedIndex = navItems.indexOfFirst { (route, _, _) ->
                        if (route == "home") currentDestination?.startsWith("home") == true
                        else currentDestination == route
                    }.coerceAtLeast(0)

                    val targetNavBias = -1f + (selectedIndex * (2f / (navItems.size - 1)))
                    val animatedNavBias by animateFloatAsState(
                        targetValue = targetNavBias,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bottom_nav_slider_bias"
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .shadow(12.dp, CircleShape, spotColor = AkiraPrimary.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier.padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Smooth Sliding Gradient Indicator
                            Row(modifier = Modifier.matchParentSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .align(Alignment.CenterVertically),
                                    contentAlignment = BiasAlignment(animatedNavBias, 0f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(1f / navItems.size)
                                            .clip(CircleShape)
                                            .background(AkiraLogoGradientBrush)
                                    )
                                }
                            }

                            // Interactive Navigation Tab Items
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                navItems.forEachIndexed { index, (route, label, icon) ->
                                    val isSelected = index == selectedIndex
                                    val contentColor by animateColorAsState(
                                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "nav_item_content_color"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (!isSelected) {
                                                    val dest = if (route == "home") "home?query={query}" else route
                                                    navController.navigate(dest) {
                                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = label,
                                                    color = contentColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("home?query={query}") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = "home?query={query}",
                    arguments = listOf(
                        navArgument("query") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val queryArg = backStackEntry.arguments?.getString("query")?.let { Uri.decode(it) }
                    HomeScreen(
                        initialSearch = queryArg,
                        onNavigateToDetail = { anime ->
                            val encodedTitle = Uri.encode(anime.displayTitle)
                            val encodedPoster = Uri.encode(anime.thumbnail ?: "")
                            navController.navigate("detail/${anime.id}?title=$encodedTitle&poster=$encodedPoster")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        onNavigateToWatchlist = {
                            navController.navigate("watchlist") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToSchedule = {
                            navController.navigate("schedule") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToMusic = {
                            navController.navigate("music")
                        },
                        onNavigateToDownloads = {
                            navController.navigate("downloads") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable("watchlist") {
                    WatchlistScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetail = { anime ->
                            val encodedTitle = Uri.encode(anime.displayTitle)
                            val encodedPoster = Uri.encode(anime.thumbnail ?: "")
                            navController.navigate("detail/${anime.id}?title=$encodedTitle&poster=$encodedPoster")
                        }
                    )
                }

                composable("schedule") {
                    ScheduleScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetail = { showId ->
                            navController.navigate("detail/$showId")
                        }
                    )
                }

                composable("music") {
                    MusicScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetail = { showId ->
                            navController.navigate("detail/$showId")
                        }
                    )
                }

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
                    val title = backStackEntry.arguments?.getString("title")
                        ?.let { Uri.decode(it) }
                        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val poster = backStackEntry.arguments?.getString("poster")
                        ?.let { Uri.decode(it) }
                        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                    AnimeDetailScreen(
                        animeId = animeId,
                        titleHint = title,
                        posterHint = poster,
                        onNavigateBack = { navController.popBackStack() },
                        onPlayEpisode = { epNumber, sources, startMs, epList, transType, malId ->
                            PlayerSessionManager.setSession(
                                showId = animeId,
                                episodeNumber = epNumber,
                                sources = sources,
                                initialPositionMs = startMs,
                                thumbnail = poster,
                                episodes = epList,
                                translationType = transType,
                                malId = malId
                            )
                            val encodedTitle = Uri.encode(title ?: "Anime")
                            navController.navigate("player/$animeId/$epNumber?title=$encodedTitle")
                        },
                        onReadChapter = { chapterString, transType ->
                            navController.navigate("manga_reader/$animeId/$chapterString?transType=$transType")
                        },
                        onFilterByGenre = { query ->
                            val encodedQuery = Uri.encode(query)
                            navController.navigate("home?query=$encodedQuery") {
                                popUpTo("home?query={query}") { inclusive = true }
                            }
                        },
                        onOpenGuide = { guideId, guideTitle ->
                            val encodedGuideTitle = Uri.encode(guideTitle ?: "Watch Guide")
                            navController.navigate("guide_detail/$guideId?title=$encodedGuideTitle")
                        },
                        onOpenMap = { mapId, mapTitle ->
                            val encodedMapTitle = Uri.encode(mapTitle ?: "World Map")
                            navController.navigate("map_viewer/$mapId?title=$encodedMapTitle")
                        }
                    )
                }

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
                    val sources = PlayerSessionManager.getSources(animeId, episodeNumber)
                    val initialMs = PlayerSessionManager.getInitialPositionMs(animeId, episodeNumber)
                    val thumb = PlayerSessionManager.getThumbnail(animeId, episodeNumber)
                    val episodes = PlayerSessionManager.getEpisodes(animeId, episodeNumber)
                    val translationType = PlayerSessionManager.getTranslationType(animeId, episodeNumber)
                    val malId = PlayerSessionManager.getMalId(animeId, episodeNumber)

                    PlayerScreen(
                        animeId = animeId,
                        animeTitle = title,
                        episodeNumber = episodeNumber,
                        sources = sources,
                        thumbnail = thumb,
                        initialPositionMs = initialMs,
                        episodes = episodes,
                        translationType = translationType,
                        malId = malId,
                        onNavigateBack = { navController.popBackStack() },
                        onChangeEpisode = { nextEp, nextSources ->
                            PlayerSessionManager.setSession(
                                showId = animeId,
                                episodeNumber = nextEp,
                                sources = nextSources,
                                initialPositionMs = 0L,
                                thumbnail = thumb,
                                episodes = episodes,
                                translationType = translationType,
                                malId = malId
                            )
                            val encodedTitle = Uri.encode(title)
                            navController.navigate("player/$animeId/$nextEp?title=$encodedTitle") {
                                popUpTo("player/$animeId/$episodeNumber?title=$encodedTitle") { inclusive = true }
                            }
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLogoutSuccess = {
                            navController.navigate("login") {
                                popUpTo("home?query={query}") { inclusive = true }
                            }
                        }
                    )
                }

                composable("downloads") {
                    DownloadsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onPlayOffline = { downloadedEp ->
                            val localSource = StreamSource(
                                sourceName = "Offline (${downloadedEp.quality})",
                                url = downloadedEp.videoPath,
                                isDirect = true,
                                priority = 100.0,
                                fileExtension = "mp4"
                            )
                            PlayerSessionManager.setSession(
                                showId = downloadedEp.animeId,
                                episodeNumber = downloadedEp.episodeNumber,
                                sources = listOf(localSource),
                                initialPositionMs = 0L,
                                thumbnail = downloadedEp.thumbnail,
                                episodes = listOf(downloadedEp.episodeNumber),
                                translationType = "sub"
                            )
                            val encodedTitle = Uri.encode(downloadedEp.animeTitle)
                            navController.navigate("player/${downloadedEp.animeId}/${downloadedEp.episodeNumber}?title=$encodedTitle")
                        }
                    )
                }

                composable(
                    route = "manga_reader/{mangaId}/{chapterString}?transType={transType}",
                    arguments = listOf(
                        navArgument("mangaId") { type = NavType.StringType },
                        navArgument("chapterString") { type = NavType.StringType },
                        navArgument("transType") {
                            type = NavType.StringType
                            defaultValue = "sub"
                        }
                    )
                ) { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
                    val chapterString = backStackEntry.arguments?.getString("chapterString") ?: "1"
                    val transType = backStackEntry.arguments?.getString("transType") ?: "sub"

                    MangaReaderScreen(
                        mangaId = mangaId,
                        chapterString = chapterString,
                        translationType = transType,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "guide_detail/{guideId}?title={title}",
                    arguments = listOf(
                        navArgument("guideId") { type = NavType.StringType },
                        navArgument("title") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = "Watch Guide"
                        }
                    )
                ) { backStackEntry ->
                    val guideId = backStackEntry.arguments?.getString("guideId") ?: ""
                    val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) } ?: "Watch Guide"

                    GuideDetailScreen(
                        threadId = guideId,
                        titleHint = title,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMedia = { mediaId, mediaTitle ->
                            val encodedMediaTitle = Uri.encode(mediaTitle ?: "Anime")
                            navController.navigate("detail/$mediaId?title=$encodedMediaTitle")
                        }
                    )
                }

                composable(
                    route = "map_viewer/{mapId}?title={title}",
                    arguments = listOf(
                        navArgument("mapId") { type = NavType.StringType },
                        navArgument("title") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = "World Map"
                        }
                    )
                ) { backStackEntry ->
                    val mapId = backStackEntry.arguments?.getString("mapId") ?: ""
                    val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) } ?: "World Map"

                    WorldMapViewerScreen(
                        mapId = mapId,
                        titleHint = title,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
