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
import com.ghostreborn.akira.features.home.widgets.MiniMusicPlayerBar
import com.ghostreborn.akira.features.home.widgets.detail.CharacterDetailDialog
import com.ghostreborn.akira.features.home.widgets.detail.CommunityReviewsSection
import com.ghostreborn.akira.features.home.widgets.detail.SoundtrackSection
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
                val title = d.englishName ?: d.name.takeIf { it.isNotBlank() } ?: titleHint ?: "Anime"

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Hero Banner with Poster Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            // Backdrop Banner
                            if (!bannerUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(bannerUrl)
                                        .apply { ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) } }
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }

                            // Gradient fading into background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color(0x99000000),
                                            0.4f to Color(0x33000000),
                                            0.8f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            1.0f to MaterialTheme.colorScheme.background
                                        )
                                    )
                            )

                            // Top Floating Action Bar (Back, Share, Bookmark/Favorite)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x80000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Share Button
                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Check out $title on Akira!\nhttps://akira.moe/anime/$animeId"
                                            )
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share $title")
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x80000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Favorite / Bookmark Button
                                IconButton(
                                    onClick = {
                                        viewModel.toggleFavorite(context)
                                        Toast.makeText(
                                            context,
                                            if (!uiState.isFavorite) "Added to Favorites" else "Removed from Favorites",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isFavorite) AkiraLogoGradientBrush
                                            else Brush.linearGradient(listOf(Color(0x80000000), Color(0x80000000)))
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Poster thumbnail card + title banner row at the bottom of hero
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val cardShape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .size(width = 96.dp, height = 138.dp)
                                        .shadow(8.dp, cardShape)
                                        .clip(cardShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), cardShape)
                                ) {
                                    if (!posterUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(posterUrl)
                                                .apply { ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) } }
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!d.nativeName.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = d.nativeName,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Metadata, Genres, Synopsis, Cast, Episodes
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Metadata Pills Row
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (d.score != null && d.score > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF3B2D14))
                                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val malUrl = if (d.malId != null && d.malId > 0) {
                                                    "https://myanimelist.net/anime/${d.malId}"
                                                } else {
                                                    "https://myanimelist.net/anime.php?q=${android.net.Uri.encode(d.name)}"
                                                }
                                                try {
                                                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(malUrl))
                                                    context.startActivity(browserIntent)
                                                } catch (_: Exception) {}
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = String.format(java.util.Locale.US, "%.1f", d.score),
                                                color = Color(0xFFFDE68A),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (d.type != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { onFilterByGenre?.invoke(d.type) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = d.type.uppercase(),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (d.status != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onFilterByGenre?.invoke(d.status) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = d.status.uppercase(),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Cloud Watchlist Status Selector Pill
                                if (uiState.watchStateId != null) {
                                    var isDropdownOpen by remember { mutableStateOf(false) }
                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (uiState.watchStatus != null) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                )
                                                .clickable { isDropdownOpen = true }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = uiState.watchStatus?.label ?: "Add to Watchlist",
                                                    color = if (uiState.watchStatus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = if (uiState.watchStatus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = isDropdownOpen,
                                            onDismissRequest = { isDropdownOpen = false }
                                        ) {
                                            com.ghostreborn.akira.features.auth.services.WatchStatus.entries.forEach { statusOption ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = statusOption.label,
                                                            fontWeight = if (uiState.watchStatus == statusOption) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (uiState.watchStatus == statusOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        isDropdownOpen = false
                                                        viewModel.setWatchStatus(context, statusOption)
                                                        Toast.makeText(context, "Set to ${statusOption.label}", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Genres
                            if (d.genres.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    d.genres.forEach { genre ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                                .clickable { onFilterByGenre?.invoke(genre) }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = genre,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Synopsis with Expand / Collapse
                            if (!d.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column {
                                    Text(
                                        text = d.description,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AkiraPrimary,
                                        modifier = Modifier
                                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }

                            // Personal Notes / Remarks Section
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "PERSONAL NOTES / REMARKS",
                                            style = TextStyle(
                                                brush = AkiraLogoGradientBrush,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.2.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = if (isRemarksEditing) "Done" else if (userRemarks.isBlank()) "+ Add Note" else "Edit",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AkiraPrimary,
                                            modifier = Modifier
                                                .clickable {
                                                    if (isRemarksEditing) {
                                                        com.ghostreborn.akira.features.home.services.UserRemarksManager.saveRemarks(context, animeId, userRemarks)
                                                        Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isRemarksEditing = !isRemarksEditing
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (isRemarksEditing) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = userRemarks,
                                            onValueChange = { userRemarks = it },
                                            placeholder = { Text("Add private remarks or thoughts on this title...", fontSize = 12.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AkiraPrimary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else if (userRemarks.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = userRemarks,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No private remarks yet. Tap '+ Add Note' to write personal notes.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // Characters & Cast Carousel
                            if (d.characters.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "CHARACTERS & CAST",
                                    style = TextStyle(
                                        brush = AkiraLogoGradientBrush,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(d.characters) { character ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(72.dp)
                                                .clickable { selectedCharacterForDialog = character }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!character.image.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(character.image)
                                                            .apply {
                                                                ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) }
                                                            }
                                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = character.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = character.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!character.actorName.isNullOrBlank()) {
                                                Text(
                                                    text = character.actorName,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Soundtracks (OPs / EDs / OSTs)
                            if (soundtracks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                SoundtrackSection(tracks = soundtracks)
                            }

                            // Resume button if available
                            if (lastWatchedEp != null) {
                                val isManga = d.type?.equals("manga", ignoreCase = true) == true
                                val resumeLabel = if (isManga) "Resume Chapter $lastWatchedEp" else "Resume Episode $lastWatchedEp"
                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isManga = d.type?.equals("manga", ignoreCase = true) == true
                                Text(
                                    text = if (isManga) "CHAPTERS" else "EPISODES",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Sort Order Toggle (1 -> N vs N -> 1)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { isAscending = !isAscending }
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapVert,
                                            contentDescription = "Sort order",
                                            tint = AkiraPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isAscending) "Oldest" else "Newest",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Episode Quick Search Toggle
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSearchEpisodeOpen) AkiraLogoGradientBrush
                                            else Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            )
                                        )
                                        .clickable {
                                            isSearchEpisodeOpen = !isSearchEpisodeOpen
                                            if (!isSearchEpisodeOpen) episodeSearchQuery = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Find episode",
                                        tint = if (isSearchEpisodeOpen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                val audioTabs = listOf("SUB", "DUB", "RAW")
                                val tabIndex = audioTabs.indexOf(selectedAudioTab).coerceAtLeast(0)
                                val animatedAudioBias by animateFloatAsState(
                                    targetValue = when (tabIndex) {
                                        0 -> -1f
                                        1 -> 0f
                                        else -> 1f
                                    },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "audio_tab_bias"
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Sliding Gradient Background Indicator
                                    Row(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(1f)
                                                .align(Alignment.CenterVertically),
                                            contentAlignment = BiasAlignment(animatedAudioBias, 0f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(0.333f)
                                                .clip(CircleShape)
                                                .background(AkiraLogoGradientBrush)
                                            )
                                        }
                                    }

                                    // Labels Row
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        audioTabs.forEach { tab ->
                                            val isSelected = selectedAudioTab == tab
                                            val textColor by animateColorAsState(
                                                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                label = "audio_tab_text"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { selectedAudioTab = tab }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tab,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Collapsible Episode Search Bar
                            AnimatedVisibility(
                                visible = isSearchEpisodeOpen,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
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

                            Spacer(modifier = Modifier.height(14.dp))

                            // Range selector chips (1-100, 101-200, ...) for anime with many episodes
                            if (episodeRanges.isNotEmpty() && episodeSearchQuery.isBlank()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
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
                                Spacer(modifier = Modifier.height(12.dp))
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
