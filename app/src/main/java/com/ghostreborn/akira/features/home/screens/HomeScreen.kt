package com.ghostreborn.akira.features.home.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.home.services.FavoriteAnime
import com.ghostreborn.akira.features.home.services.FavoritesManager
import com.ghostreborn.akira.features.home.widgets.AnimeCard
import com.ghostreborn.akira.features.home.widgets.FavoritesRowSection
import com.ghostreborn.akira.features.home.widgets.FeaturedAnimeBanner
import com.ghostreborn.akira.features.home.widgets.HomeFilterStrip
import com.ghostreborn.akira.features.home.widgets.HomeTopAppBar
import com.ghostreborn.akira.features.home.widgets.MiniMusicPlayerBar
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    initialSearch: String? = null,
    onNavigateToDetail: (AnimeShow) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWatchlist: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf(initialSearch ?: "") }
    var isSearchActive by remember { mutableStateOf(!initialSearch.isNullOrBlank()) }
    var translationType by remember { mutableStateOf("sub") } // "sub" or "dub"
    var selectedMediaType by remember { mutableStateOf("anime") } // "anime" or "manga"

    val categories = remember {
        listOf(
            "Latest Updates" to "Latest_Update",
            "Top Rated" to "Top",
            "Popular" to "Popular",
            "New Releases" to "Release_Year"
        )
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val shows = remember { mutableStateListOf<AnimeShow>() }
    val featuredShows = remember { mutableStateListOf<AnimeShow>() }
    val favoriteShows = remember { mutableStateListOf<FavoriteAnime>() }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    val targetBias = if (translationType == "sub") -1f else 1f
    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pill_translation_bias"
    )

    val targetMediaBias = if (selectedMediaType == "anime") -1f else 1f
    val animatedMediaBias by animateFloatAsState(
        targetValue = targetMediaBias,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pill_media_bias"
    )

    fun fetchShows(page: Int, reset: Boolean = false) {
        if (page > 1 && (isLoading || isLoadingMore || !hasMore)) return
        if (page == 1 && isLoading && !reset) return

        if (page == 1) {
            isLoading = true
            errorMessage = null
            if (reset) shows.clear()
        } else {
            isLoadingMore = true
        }

        scope.launch {
            try {
                val sortBy = categories[selectedCategoryIndex].second
                val q = searchQuery.trim().ifEmpty { null }
                val result = if (selectedMediaType == "manga") {
                    ServiceLocator.mangaRepository.fetchMangaList(
                        page = page,
                        searchQuery = q,
                        sortBy = sortBy,
                        context = context
                    )
                } else {
                    AnimeRepository.fetchAnimeList(
                        page = page,
                        searchQuery = q,
                        sortBy = sortBy,
                        translationType = translationType,
                        context = context
                    )
                }

                if (page == 1) {
                    shows.clear()
                    shows.addAll(result.shows)
                } else {
                    shows.addAll(result.shows)
                }

                currentPage = page
                hasMore = result.shows.isNotEmpty()
            } catch (e: Exception) {
                if (page == 1) {
                    errorMessage = e.message ?: "Failed to load shows"
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    // Reload favorites when returning to the HomeScreen
    LifecycleResumeEffect(Unit) {
        val currentFavorites = FavoritesManager.getFavorites(context)
        favoriteShows.clear()
        favoriteShows.addAll(currentFavorites)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        // Load top-ranked for carousel
        scope.launch {
            try {
                val ranked = AnimeRepository.fetchTopRankedAnime(dateRange = 1, size = 8, context = context)
                if (ranked.isNotEmpty()) {
                    featuredShows.clear()
                    featuredShows.addAll(ranked)
                }
            } catch (_: Exception) {}
        }
        fetchShows(1, reset = true)
    }

    // Pagination scroll listener
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6 && !isLoading && !isLoadingMore && hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            fetchShows(currentPage + 1)
        }
    }

    val searchFocusRequester = remember { FocusRequester() }

    // Close search mode and reset query when back button is pressed
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        fetchShows(1, reset = true)
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            // Automatically request focus and open keyboard
            delay(100L)
            try {
                searchFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Main Anime Grid including Header and Categories as scrollable items
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Clean & Balanced Top App Bar
            item(
                key = "header_bar",
                contentType = "header",
                span = { GridItemSpan(2) }
            ) {
                HomeTopAppBar(
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    selectedMediaType = selectedMediaType,
                    animatedMediaBias = animatedMediaBias,
                    searchFocusRequester = searchFocusRequester,
                    onSearchQueryChange = { newQuery ->
                        searchQuery = newQuery
                        searchJob?.cancel()
                        searchJob = scope.launch {
                            delay(400L)
                            fetchShows(1, reset = true)
                        }
                    },
                    onCloseSearch = {
                        isSearchActive = false
                        searchQuery = ""
                        fetchShows(1, reset = true)
                    },
                    onOpenSearch = { isSearchActive = true },
                    onSelectMediaType = { type ->
                        if (selectedMediaType != type) {
                            selectedMediaType = type
                            fetchShows(1, reset = true)
                        }
                    },
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            // Unified Filter Strip (SUB/DUB Pill + Category Chips)
            item(
                key = "unified_filter_strip",
                contentType = "filters",
                span = { GridItemSpan(2) }
            ) {
                HomeFilterStrip(
                    selectedMediaType = selectedMediaType,
                    translationType = translationType,
                    animatedBias = animatedBias,
                    categories = categories,
                    selectedCategoryIndex = selectedCategoryIndex,
                    onSelectTranslationType = { type ->
                        if (translationType != type) {
                            translationType = type
                            fetchShows(1, reset = true)
                        }
                    },
                    onSelectCategory = { index ->
                        if (selectedCategoryIndex != index) {
                            selectedCategoryIndex = index
                            fetchShows(1, reset = true)
                        }
                    }
                )
            }

            // Featured Banner header item (shown for anime mode)
            if (featuredShows.isNotEmpty() && !isSearchActive && selectedMediaType == "anime") {
                item(
                    key = "featured_banner",
                    contentType = "banner",
                    span = { GridItemSpan(2) }
                ) {
                    FeaturedAnimeBanner(
                        featuredShows = featuredShows,
                        onTap = onNavigateToDetail,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Favorited Anime Row (Shown before latest updates/shows when not in search)
            if (favoriteShows.isNotEmpty() && !isSearchActive) {
                item(
                    key = "favorites_section",
                    contentType = "favorites",
                    span = { GridItemSpan(2) }
                ) {
                    FavoritesRowSection(
                        favorites = favoriteShows,
                        onTap = onNavigateToDetail,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            // Loading state when initial fetch
            if (isLoading && shows.isEmpty()) {
                item(
                    key = "initial_loading",
                    contentType = "loading",
                    span = { GridItemSpan(2) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AkiraPrimary)
                    }
                }
            } else if (!errorMessage.isNullOrBlank() && shows.isEmpty()) {
                item(
                    key = "error_retry",
                    contentType = "error",
                    span = { GridItemSpan(2) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = errorMessage ?: "Error", color = Color.Red)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to Retry",
                                color = AkiraPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { fetchShows(1, reset = true) }
                            )
                        }
                    }
                }
            }

            items(
                items = shows,
                key = { it.id },
                contentType = { "anime_card" }
            ) { show ->
                AnimeCard(
                    anime = show,
                    onTap = { onNavigateToDetail(show) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                )
            }

            if (isLoadingMore) {
                item(
                    key = "loading_more",
                    contentType = "loading_more",
                    span = { GridItemSpan(2) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AkiraPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Mini Floating Music Player
        MiniMusicPlayerBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}
