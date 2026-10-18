package com.ghostreborn.akira.features.home.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ghostreborn.akira.features.home.viewmodels.AnimeDetailViewModel
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import com.ghostreborn.akira.features.downloads.models.DownloadStatus
import com.ghostreborn.akira.features.downloads.services.EpisodeDownloadManager
import com.ghostreborn.akira.features.home.models.AnimeCharacter
import com.ghostreborn.akira.features.home.models.CommunityReview
import com.ghostreborn.akira.features.home.models.MusicTrack
import com.ghostreborn.akira.features.home.services.MusicRepository
import com.ghostreborn.akira.features.home.services.ReviewRepository
import com.ghostreborn.akira.features.home.services.UserRemarksManager
import com.ghostreborn.akira.features.home.widgets.MiniMusicPlayerBar
import com.ghostreborn.akira.features.home.widgets.detail.AnimeCharactersSection
import com.ghostreborn.akira.features.home.widgets.detail.AnimeDetailHeroHeader
import com.ghostreborn.akira.features.home.widgets.detail.AnimeMetadataSection
import com.ghostreborn.akira.features.home.widgets.detail.CharacterDetailDialog
import com.ghostreborn.akira.features.home.widgets.detail.CommunityReviewsSection
import com.ghostreborn.akira.features.home.widgets.detail.EpisodeControlsRow
import com.ghostreborn.akira.features.home.widgets.detail.SoundtrackSection
import com.ghostreborn.akira.features.home.widgets.detail.WatchOrderGuidesSection
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AnimeDetailScreen(
    animeId: String,
    titleHint: String? = null,
    posterHint: String? = null,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (String, List<StreamSource>, Long, List<String>, String, Long?) -> Unit,
    onReadChapter: ((String, String) -> Unit)? = null,
    onFilterByGenre: ((String) -> Unit)? = null,
    onOpenGuide: ((String, String?) -> Unit)? = null,
    onOpenMap: ((String, String?) -> Unit)? = null,
    viewModel: AnimeDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedAudioTab by remember { mutableStateOf("SUB") }
    var isAscending by remember { mutableStateOf(true) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var isSearchEpisodeOpen by remember { mutableStateOf(false) }
    var episodeSearchQuery by remember { mutableStateOf("") }

    var lastProgressMs by remember { mutableLongStateOf(0L) }
    var lastWatchedEp by remember { mutableStateOf<String?>(null) }
    var userRemarks by remember { mutableStateOf("") }
    var isRemarksEditing by remember { mutableStateOf(false) }

    val soundtracks = remember { androidx.compose.runtime.mutableStateListOf<MusicTrack>() }
    val communityReviews = remember { androidx.compose.runtime.mutableStateListOf<CommunityReview>() }
    var selectedCharacterForDialog by remember { mutableStateOf<AnimeCharacter?>(null) }

    val downloadsMap by EpisodeDownloadManager.downloads.collectAsState()
    var episodeToDownloadDialog by remember { mutableStateOf<String?>(null) }
    var downloadStreamsCandidate by remember { mutableStateOf<List<StreamSource>>(emptyList()) }
    var isFetchingForDownload by remember { mutableStateOf(false) }

    LaunchedEffect(animeId) {
        viewModel.loadAnimeDetail(context, animeId)
        userRemarks = com.ghostreborn.akira.features.home.services.UserRemarksManager.getRemarks(context, animeId)
        val mangaProgress = com.ghostreborn.akira.features.manga.data.MangaHistoryManager.getProgress(context, animeId)
        if (mangaProgress != null) {
            lastWatchedEp = mangaProgress.lastChapterString
        } else {
            val progress = WatchHistoryManager.getProgress(context, animeId)
            if (progress != null) {
                lastProgressMs = progress.seekPositionMs
                lastWatchedEp = progress.episodeNumber
            }
        }

        // Fetch OST & theme songs for this show
        launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tracks = MusicRepository.fetchMusics(context, showId = animeId, limit = 15)
                soundtracks.clear()
                soundtracks.addAll(tracks)
            } catch (_: Exception) {}
        }

        // Fetch Community Reviews & Ratings for this show
        launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val reviews = ReviewRepository.fetchReviews(context, showId = animeId, limit = 10)
                communityReviews.clear()
                communityReviews.addAll(reviews)
            } catch (_: Exception) {}
        }
    }

    // Auto-select audio tab when detail is first received
    LaunchedEffect(uiState.animeDetail) {
        val d = uiState.animeDetail ?: return@LaunchedEffect
        if (d.episodesDetail.sub.isNotEmpty()) {
            selectedAudioTab = "SUB"
        } else if (d.episodesDetail.dub.isNotEmpty()) {
            selectedAudioTab = "DUB"
        } else if (d.episodesDetail.raw.isNotEmpty()) {
            selectedAudioTab = "RAW"
        }
    }

    val episodes = remember(uiState.animeDetail, selectedAudioTab, isAscending) {
        val d = uiState.animeDetail ?: return@remember emptyList()
        val rawList = when (selectedAudioTab) {
            "SUB" -> if (d.episodesDetail.sub.isNotEmpty()) d.episodesDetail.sub else (1..d.availableEpisodesSub).map { "$it" }
            "DUB" -> if (d.episodesDetail.dub.isNotEmpty()) d.episodesDetail.dub else (1..d.availableEpisodesDub).map { "$it" }
            else -> d.episodesDetail.raw
        }
        val comparator = Comparator<String> { a, b ->
            val numA = a.toDoubleOrNull()
            val numB = b.toDoubleOrNull()
            val cmp = if (numA != null && numB != null) numA.compareTo(numB)
            else a.compareTo(b, ignoreCase = true)
            if (isAscending) cmp else -cmp
        }
        rawList.sortedWith(comparator)
    }

    val episodeChunks = remember(episodes) {
        if (episodes.size > 100) episodes.chunked(100) else emptyList()
    }

    val episodeRanges = remember(episodeChunks) {
        episodeChunks.map { subList ->
            val first = subList.firstOrNull() ?: ""
            val last = subList.lastOrNull() ?: ""
            "$first - $last"
        }
    }

    var selectedRangeIndex by remember(episodes) {
        val initialIdx = if (episodes.size > 100 && lastWatchedEp != null) {
            val epIdx = episodes.indexOf(lastWatchedEp)
            if (epIdx >= 0) epIdx / 100 else 0
        } else 0
        mutableIntStateOf(initialIdx)
    }

    val filteredEpisodes = remember(episodes, selectedRangeIndex, episodeChunks, episodeSearchQuery) {
        val query = episodeSearchQuery.trim()
        if (query.isNotEmpty()) {
            episodes.filter { it.contains(query, ignoreCase = true) }
        } else if (episodeChunks.isNotEmpty()) {
            val safeIdx = selectedRangeIndex.coerceIn(0, episodeChunks.lastIndex)
            episodeChunks[safeIdx]
        } else {
            episodes
        }
    }

    val chunkedEpisodes = remember(filteredEpisodes) {
        filteredEpisodes.chunked(5)
    }

    fun playEpisode(epStr: String, startMs: Long = 0L) {
        val detail = uiState.animeDetail
        val isManga = detail?.type?.equals("manga", ignoreCase = true) == true ||
                detail?.type?.contains("manga", ignoreCase = true) == true
        if (isManga && onReadChapter != null) {
            onReadChapter(epStr, selectedAudioTab.lowercase())
            return
        }

        // Check if episode has already been downloaded for offline playback
        val downloaded = EpisodeDownloadManager.getDownloadedEpisode(animeId, epStr)
        if (downloaded != null) {
            val localSource = StreamSource(
                sourceName = "Offline (${downloaded.quality})",
                url = downloaded.videoPath,
                isDirect = true,
                priority = 100.0,
                fileExtension = "mp4"
            )
            onPlayEpisode(epStr, listOf(localSource), startMs, episodes, selectedAudioTab.lowercase(), uiState.animeDetail?.malId)
            return
        }

        if (uiState.isFetchingStreams) return
        val currentAudioTab = selectedAudioTab.lowercase()
        viewModel.fetchEpisodeStreams(
            context = context,
            episodeString = epStr,
            translationType = currentAudioTab,
            onSuccess = { streams ->
                onPlayEpisode(epStr, streams, startMs, episodes, currentAudioTab, uiState.animeDetail?.malId)
            },
            onError = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        when {
            uiState.isLoading && uiState.animeDetail == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AkiraPrimary)
                }
            }
            uiState.errorMessage != null && uiState.animeDetail == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.errorMessage ?: "Error", color = Color.Red)
                }
            }
            uiState.animeDetail != null -> {
                val d = uiState.animeDetail!!
                val posterUrl = d.thumbnail ?: posterHint
                val bannerUrl = d.banner ?: d.thumbnail ?: posterHint
                val safeTitleHint = titleHint?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val title = listOfNotNull(d.englishName, d.name, safeTitleHint, d.nativeName)
                    .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Anime"
                val safeNativeName = d.nativeName?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals(title, ignoreCase = true) }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Hero Banner with Poster Card
                    item {
                        AnimeDetailHeroHeader(
                            animeId = animeId,
                            title = title,
                            safeNativeName = safeNativeName,
                            posterUrl = posterUrl,
                            bannerUrl = bannerUrl,
                            isFavorite = uiState.isFavorite,
                            onNavigateBack = onNavigateBack,
                            onToggleFavorite = {
                                viewModel.toggleFavorite(context)
                                Toast.makeText(
                                    context,
                                    if (!uiState.isFavorite) "Added to Favorites" else "Removed from Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    // Metadata, Genres, Synopsis, Notes, Characters, Soundtracks, Guides
                    item {
                        AnimeMetadataSection(
                            animeDetail = d,
                            watchStateId = uiState.watchStateId,
                            watchStatus = uiState.watchStatus,
                            guides = uiState.guides,
                            worldMaps = uiState.worldMaps,
                            userRemarks = userRemarks,
                            isRemarksEditing = isRemarksEditing,
                            isSynopsisExpanded = isSynopsisExpanded,
                            onToggleSynopsis = { isSynopsisExpanded = !isSynopsisExpanded },
                            onRemarksChange = { userRemarks = it },
                            onSaveRemarks = {
                                UserRemarksManager.saveRemarks(context, animeId, userRemarks)
                                Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
                            },
                            onToggleRemarksEditing = { isRemarksEditing = !isRemarksEditing },
                            onSetWatchStatus = { status ->
                                viewModel.setWatchStatus(context, status)
                                Toast.makeText(context, "Set to ${status.label}", Toast.LENGTH_SHORT).show()
                            },
                            onFilterByGenre = onFilterByGenre,
                            onOpenGuide = onOpenGuide,
                            onOpenMap = onOpenMap
                        )

                        // Characters & Cast Carousel
                        AnimeCharactersSection(
                            characters = d.characters,
                            onSelectCharacter = { selectedCharacterForDialog = it },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        // Soundtracks (OPs / EDs / OSTs)
                        if (soundtracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            SoundtrackSection(
                                tracks = soundtracks,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        // Watch Order Maps & Guides
                        Spacer(modifier = Modifier.height(20.dp))
                        WatchOrderGuidesSection(
                            guides = uiState.guides,
                            worldMaps = uiState.worldMaps,
                            isLoading = uiState.isLoadingGuides,
                            onOpenGuide = { guide ->
                                onOpenGuide?.invoke(guide.id, guide.title)
                            },
                            onOpenMap = { map ->
                                onOpenMap?.invoke(map.id, map.title)
                            },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        // Resume button if available
                        if (lastWatchedEp != null) {
                            val isManga = d.type?.equals("manga", ignoreCase = true) == true
                            val resumeLabel = if (isManga) "Resume Chapter $lastWatchedEp" else "Resume Episode $lastWatchedEp"
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AkiraLogoGradientBrush)
                                    .clickable { playEpisode(lastWatchedEp!!, lastProgressMs) }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = resumeLabel,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Audio Selector & Controls Row (Sort order, Episode Search, and SUB/DUB/RAW)
                        val isManga = d.type?.equals("manga", ignoreCase = true) == true
                        EpisodeControlsRow(
                            isManga = isManga,
                            isAscending = isAscending,
                            isSearchOpen = isSearchEpisodeOpen,
                            selectedAudioTab = selectedAudioTab,
                            onToggleAscending = { isAscending = !isAscending },
                            onToggleSearch = {
                                isSearchEpisodeOpen = !isSearchEpisodeOpen
                                if (!isSearchEpisodeOpen) episodeSearchQuery = ""
                            },
                            onSelectAudioTab = { selectedAudioTab = it },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        // Collapsible Episode Search Bar
                        AnimatedVisibility(
                            visible = isSearchEpisodeOpen,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = episodeSearchQuery,
                                    onValueChange = { episodeSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            text = "Jump to episode number (e.g. 1042)...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    singleLine = true,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = AkiraPrimary
                                    )
                                )
                            }
                        }

                        // Range selector chips (1-100, 101-200, ...) for anime with many episodes
                        if (episodeRanges.isNotEmpty() && episodeSearchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                itemsIndexed(episodeRanges) { index, rangeLabel ->
                                    val isSelected = selectedRangeIndex == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .then(
                                                if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                            .clickable { selectedRangeIndex = index }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = rangeLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Episodes Grid/List
                    if (episodes.isEmpty()) {
                        item(key = "episodes_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val isManga = d.type?.equals("manga", ignoreCase = true) == true
                                Text(
                                    text = if (isManga) "No chapters available for $selectedAudioTab" else "No episodes available for $selectedAudioTab",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = chunkedEpisodes,
                            key = { row -> "ep_row_${row.firstOrNull() ?: ""}" },
                            contentType = { "episode_row" }
                        ) { rowEps ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEps.forEach { epStr ->
                                    val isCurrentLoading = uiState.isFetchingStreams && uiState.selectedEpisodeNumber == epStr
                                    val isLastWatched = lastWatchedEp == epStr
                                    val dlKey = "$animeId:$epStr"
                                    val dlRecord = downloadsMap[dlKey]
                                    val isDownloaded = dlRecord?.status == DownloadStatus.COMPLETED
                                    val isDownloading = dlRecord?.status == DownloadStatus.DOWNLOADING

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .then(
                                                when {
                                                    isCurrentLoading -> Modifier.background(AkiraLogoGradientBrush)
                                                    isLastWatched -> Modifier
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .border(1.5.dp, AkiraLogoGradientBrush, RoundedCornerShape(12.dp))
                                                    isDownloaded -> Modifier
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .border(1.2.dp, AkiraPrimary, RoundedCornerShape(12.dp))
                                                    else -> Modifier
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                                }
                                            )
                                            .combinedClickable(
                                                enabled = !uiState.isFetchingStreams,
                                                onClick = { playEpisode(epStr) },
                                                onLongClick = {
                                                    if (!isDownloaded && !isDownloading) {
                                                        episodeToDownloadDialog = epStr
                                                        isFetchingForDownload = true
                                                        viewModel.fetchEpisodeStreams(
                                                            context = context,
                                                            episodeString = epStr,
                                                            translationType = selectedAudioTab.lowercase(),
                                                            onSuccess = { s ->
                                                                downloadStreamsCandidate = s
                                                                isFetchingForDownload = false
                                                            },
                                                            onError = { msg ->
                                                                isFetchingForDownload = false
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrentLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = epStr,
                                                color = if (isLastWatched) AkiraPrimary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isLastWatched) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (isDownloaded) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(AkiraPrimary)
                                                )
                                            } else if (isDownloading) {
                                                CircularProgressIndicator(
                                                    progress = { dlRecord?.progress ?: 0f },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(8.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = AkiraPrimary
                                                )
                                            }
                                            if (isLastWatched) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth(0.6f)
                                                        .height(2.5.dp)
                                                        .clip(CircleShape)
                                                        .background(AkiraLogoGradientBrush)
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(5 - rowEps.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (communityReviews.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            CommunityReviewsSection(
                                reviews = communityReviews,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }

        // Mini Music Player Bar
        MiniMusicPlayerBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // Character Voice Actor Detail Dialog
        if (selectedCharacterForDialog != null) {
            CharacterDetailDialog(
                character = selectedCharacterForDialog!!,
                onDismiss = { selectedCharacterForDialog = null }
            )
        }

        // Episode Download Dialog
        if (episodeToDownloadDialog != null) {
            com.ghostreborn.akira.features.downloads.widgets.EpisodeDownloadDialog(
                episodeNumber = episodeToDownloadDialog!!,
                animeTitle = uiState.animeDetail?.name ?: "Anime",
                streams = downloadStreamsCandidate,
                isLoading = isFetchingForDownload,
                onDismiss = {
                    episodeToDownloadDialog = null
                    downloadStreamsCandidate = emptyList()
                },
                onSelectStream = { source, quality ->
                    EpisodeDownloadManager.enqueueDownload(
                        context = context,
                        animeId = animeId,
                        episodeNumber = episodeToDownloadDialog!!,
                        animeTitle = uiState.animeDetail?.name ?: "Anime",
                        downloadUrl = source.url,
                        thumbnail = uiState.animeDetail?.thumbnail,
                        quality = quality
                    )
                    Toast.makeText(context, "Download started for Episode ${episodeToDownloadDialog!!}", Toast.LENGTH_SHORT).show()
                    episodeToDownloadDialog = null
                    downloadStreamsCandidate = emptyList()
                }
            )
        }
    }
}
