package com.ghostreborn.akira.features.home.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.auth.services.WatchStatus
import com.ghostreborn.akira.features.auth.services.WatchlistSyncManager
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.home.widgets.AnimeCard
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.launch

enum class WatchlistSort(val label: String, val queryValue: String?) {
    RECENT("Recent Activity", "UPLOAD_TIME"),
    SCORE("Top Score", "SCORE"),
    TITLE("Title (A-Z)", "TITLE")
}

@Composable
fun WatchlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (AnimeShow) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedStatus by remember { mutableStateOf(WatchStatus.WATCHING) }
    var selectedMediaType by remember { mutableStateOf("ALL") } // "ALL", "ANIME", "MANGA"
    var selectedSort by remember { mutableStateOf(WatchlistSort.RECENT) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSortDropdownOpen by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val watchlistItems = remember { mutableStateListOf<AnimeShow>() }
    val statusCounts = remember { mutableStateMapOf<WatchStatus, Int>() }

    // History map for new episode indicators
    val watchHistoryMap = remember {
        mutableStateMapOf<String, String>().apply {
            WatchHistoryManager.getProgressList(context).forEach {
                put(it.animeId, it.episodeNumber)
            }
        }
    }

    fun loadCounts() {
        scope.launch {
            val counts = WatchlistSyncManager.fetchStatusCounts(context, selectedMediaType)
            statusCounts.putAll(counts)
        }
    }

    fun loadWatchlist() {
        isLoading = true
        scope.launch {
            try {
                val pageResult = WatchlistSyncManager.fetchWatchlist(
                    context = context,
                    status = selectedStatus,
                    cardType = selectedMediaType,
                    sortBy = selectedSort.queryValue,
                    searchQuery = searchQuery.takeIf { it.isNotBlank() }
                )
                watchlistItems.clear()

                // In case backend sorting is basic, also apply client-side sorting if needed
                val sorted = when (selectedSort) {
                    WatchlistSort.SCORE -> pageResult.shows.sortedByDescending { it.score ?: 0.0 }
                    WatchlistSort.TITLE -> pageResult.shows.sortedBy { (it.englishName ?: it.name).lowercase() }
                    WatchlistSort.RECENT -> pageResult.shows
                }

                // If searchQuery is present and backend didn't filter, filter in memory
                val filtered = if (searchQuery.isNotBlank()) {
                    sorted.filter {
                        (it.englishName ?: "").contains(searchQuery, ignoreCase = true) ||
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    sorted
                }

                watchlistItems.addAll(filtered)
                statusCounts[selectedStatus] = pageResult.totalCount
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedMediaType) {
        loadCounts()
    }

    LaunchedEffect(selectedStatus, selectedMediaType, selectedSort, searchQuery) {
        loadWatchlist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar: Back, Title, Actions (Search, Sort)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "MY WATCHLIST",
                style = TextStyle(
                    brush = AkiraLogoGradientBrush,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Search Icon Toggle
            IconButton(
                onClick = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) searchQuery = ""
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSearchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = "Search Watchlist",
                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sort Dropdown Button
            Box {
                IconButton(
                    onClick = { isSortDropdownOpen = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Sort Watchlist",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = isSortDropdownOpen,
                    onDismissRequest = { isSortDropdownOpen = false }
                ) {
                    WatchlistSort.entries.forEach { sortOption ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sortOption.label,
                                    fontWeight = if (selectedSort == sortOption) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSort == sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedSort = sortOption
                                isSortDropdownOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Tier 2: Media Type Filter (ALL / ANIME / MANGA) Pill Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("ALL" to "ALL", "ANIME" to "ANIME", "MANGA" to "MANGA").forEach { (type, label) ->
                        val isSelected = selectedMediaType == type
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "media_filter_text"
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                    else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedMediaType = type
                                }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (selectedSort != WatchlistSort.RECENT) {
                Text(
                    text = selectedSort.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AkiraPrimary
                )
            }
        }

        // Live Search Bar (expandable)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter titles in ${selectedStatus.label}...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = AkiraPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AkiraPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }

        // Status Filter Tabs with Pill Counters: e.g. "Watching (14)"
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(WatchStatus.entries) { status ->
                val isSelected = selectedStatus == status
                val count = statusCounts[status]
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "status_tab_text"
                )
                val badgeText = if (count != null && count > 0) "${status.label} ($count)" else status.label

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                            else Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                        .clickable { selectedStatus = status }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }

        // Active Sort indicator banner
        if (selectedSort != WatchlistSort.RECENT || searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sorted by: ${selectedSort.label}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = " • Filter: \"$searchQuery\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AkiraPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AkiraPrimary)
            }
        } else if (watchlistItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No results matching \"$searchQuery\"" else "No shows in \"${selectedStatus.label}\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try refining your search keyword." else "Titles you add to your watchlist will appear here synced with your account.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = watchlistItems,
                    key = { it.id }
                ) { show ->
                    // Determine if new episode is available
                    val lastWatchedEp = watchHistoryMap[show.id]?.toDoubleOrNull()
                    val availableEp = (show.lastEpisodeSub?.toDoubleOrNull() ?: show.availableEpisodesSub.toDouble())
                    val hasNewEp = selectedStatus == WatchStatus.WATCHING &&
                            lastWatchedEp != null &&
                            availableEp > lastWatchedEp

                    AnimeCard(
                        anime = show,
                        hasNewEpisode = hasNewEp,
                        onTap = { onNavigateToDetail(show) }
                    )
                }
            }
        }
    }
}

