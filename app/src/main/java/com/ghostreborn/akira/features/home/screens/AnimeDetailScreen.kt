package com.ghostreborn.akira.features.home.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraBackground
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraTextPrimary
import com.ghostreborn.akira.ui.theme.AkiraTextSecondary
import com.ghostreborn.akira.utils.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    animeId: String,
    initialTitle: String? = null,
    initialPoster: String? = null,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (String, List<StreamSource>, Long, List<String>, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedAudioTab by remember { mutableStateOf("SUB") } // "SUB", "DUB", "RAW"
    var loadingEpisodeString by remember { mutableStateOf<String?>(null) }
    var lastProgressMs by remember { mutableStateOf(0L) }
    var lastWatchedEp by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(animeId) {
        // Load last watched progress
        val progress = WatchHistoryManager.getProgress(context, animeId)
        if (progress != null) {
            lastProgressMs = progress.seekPositionMs
            lastWatchedEp = progress.episodeNumber
        }

        try {
            val d = AnimeRepository.fetchAnimeDetail(animeId, context = context)
            detail = d
            if (d.episodesDetail.sub.isNotEmpty()) {
                selectedAudioTab = "SUB"
            } else if (d.episodesDetail.dub.isNotEmpty()) {
                selectedAudioTab = "DUB"
            } else if (d.episodesDetail.raw.isNotEmpty()) {
                selectedAudioTab = "RAW"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load details"
        } finally {
            isLoading = false
        }
    }

    val episodes = remember(detail, selectedAudioTab) {
        val d = detail ?: return@remember emptyList()
        when (selectedAudioTab) {
            "SUB" -> if (d.episodesDetail.sub.isNotEmpty()) d.episodesDetail.sub else (1..d.availableEpisodesSub).map { "$it" }
            "DUB" -> if (d.episodesDetail.dub.isNotEmpty()) d.episodesDetail.dub else (1..d.availableEpisodesDub).map { "$it" }
            else -> d.episodesDetail.raw
        }
    }

    fun playEpisode(epStr: String, startMs: Long = 0L) {
        if (loadingEpisodeString != null) return
        loadingEpisodeString = epStr

        scope.launch {
            try {
                val token = TokenManager.getAccessToken(context)
                val result = AnimeRepository.fetchEpisodeStreams(
                    context = context,
                    showId = animeId,
                    episodeString = epStr,
                    translationType = selectedAudioTab.lowercase(),
                    authToken = token
                )
                onPlayEpisode(epStr, result.streams, startMs, episodes, selectedAudioTab.lowercase())
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load ep $epStr: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loadingEpisodeString = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        if (isLoading && detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AkiraPrimary)
            }
        } else if (errorMessage != null && detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage ?: "Error", color = Color.Red)
            }
        } else {
            val d = detail!!
            val posterUrl = d.thumbnail ?: initialPoster
            val bannerUrl = d.banner ?: d.thumbnail ?: initialPoster
            val title = d.englishName ?: d.name

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

                        // Floating Back Button
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(start = 16.dp, top = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        // Poster thumbnail card + title banner row at the bottom of hero
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Elevated Poster Card
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

                // Info Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Pills Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (d.score != null && d.score > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF3B2D14))
                                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
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
                                            text = String.format("%.1f", d.score),
                                            color = Color(0xFFFDE68A),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (d.type != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = d.type.uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (d.status != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
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

                        // Synopsis
                        if (!d.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = d.description,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Resume button if available
                        if (lastWatchedEp != null) {
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
                                        text = "Resume Episode $lastWatchedEp",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Audio Selector (SUB / DUB / RAW)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EPISODES",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

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
                                        contentAlignment = androidx.compose.ui.BiasAlignment(animatedAudioBias, 0f)
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
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null
                                                ) { selectedAudioTab = tab }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
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

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Episodes Grid/List
                item {
                    if (episodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No episodes available for $selectedAudioTab", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Grid of episode numbers
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            val chunked = episodes.chunked(5)
                            chunked.forEach { rowEps ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowEps.forEach { epStr ->
                                        val isCurrentLoading = loadingEpisodeString == epStr
                                        val isLastWatched = lastWatchedEp == epStr
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .shadow(if (isLastWatched || isCurrentLoading) 3.dp else 1.dp, RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp))
                                                .then(
                                                    when {
                                                        isCurrentLoading -> Modifier.background(AkiraLogoGradientBrush)
                                                        isLastWatched -> Modifier
                                                            .background(MaterialTheme.colorScheme.surface)
                                                            .border(1.5.dp, AkiraLogoGradientBrush, RoundedCornerShape(12.dp))
                                                        else -> Modifier
                                                            .background(MaterialTheme.colorScheme.surface)
                                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                                    }
                                                )
                                                .clickable(enabled = loadingEpisodeString == null) { playEpisode(epStr) },
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
                                            }
                                        }
                                    }
                                    // Filler for incomplete rows
                                    repeat(5 - rowEps.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
