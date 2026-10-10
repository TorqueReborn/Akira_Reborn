package com.ghostreborn.akira.features.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.home.widgets.AnimeCard
import com.ghostreborn.akira.features.home.widgets.FeaturedAnimeBanner
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraBackground
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraTextPrimary
import com.ghostreborn.akira.ui.theme.AkiraTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToDetail: (AnimeShow) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var translationType by remember { mutableStateOf("sub") } // "sub" or "dub"

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
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun fetchShows(page: Int, reset: Boolean = false) {
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
                val result = AnimeRepository.fetchAnimeList(
                    page = page,
                    searchQuery = q,
                    sortBy = sortBy,
                    translationType = translationType
                )

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

    LaunchedEffect(Unit) {
        // Load top-ranked for carousel
        scope.launch {
            try {
                val ranked = AnimeRepository.fetchTopRankedAnime(dateRange = 1, size = 8)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AkiraBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            if (isSearchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            searchJob?.cancel()
                            searchJob = scope.launch {
                                delay(400L)
                                fetchShows(1, reset = true)
                            }
                        },
                        placeholder = { Text("Search anime, characters, studios...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AkiraPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            fetchShows(1, reset = true)
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close Search")
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AKIRA",
                        style = TextStyle(
                            brush = AkiraLogoGradientBrush,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Sub / Dub Pill Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF1EEF8))
                            .padding(3.dp)
                    ) {
                        listOf("sub" to "SUB", "dub" to "DUB").forEach { (type, label) ->
                            val isSelected = translationType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) AkiraPrimary else Color.Transparent)
                                    .clickable {
                                        if (translationType != type) {
                                            translationType = type
                                            fetchShows(1, reset = true)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else AkiraTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = AkiraTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = AkiraTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Categories horizontal tab row
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                categories.forEachIndexed { index, (label, _) ->
                    val isSelected = selectedCategoryIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            if (selectedCategoryIndex != index) {
                                selectedCategoryIndex = index
                                fetchShows(1, reset = true)
                            }
                        },
                        text = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AkiraPrimary else Color(0xFFEDE8F5))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else AkiraTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Anime Grid
            if (isLoading && shows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AkiraPrimary)
                }
            } else if (!errorMessage.isNullOrBlank() && shows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Featured Banner header item
                    if (featuredShows.isNotEmpty() && !isSearchActive) {
                        item(span = { GridItemSpan(2) }) {
                            FeaturedAnimeBanner(
                                featuredShows = featuredShows,
                                onTap = onNavigateToDetail,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    items(shows, key = { it.id }) { show ->
                        AnimeCard(
                            anime = show,
                            onTap = { onNavigateToDetail(show) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                        )
                    }

                    if (isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
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
            }
        }
    }
}
