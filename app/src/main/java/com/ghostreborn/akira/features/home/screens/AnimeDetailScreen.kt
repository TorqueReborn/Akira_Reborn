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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
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
    onPlayEpisode: (String, List<StreamSource>, Long) -> Unit
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
            val d = AnimeRepository.fetchAnimeDetail(animeId)
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
                onPlayEpisode(epStr, result.streams, startMs)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load ep $epStr: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loadingEpisodeString = null
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AkiraBackground)
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
            val bannerUrl = d.banner ?: d.thumbnail ?: initialPoster
            val title = d.englishName ?: d.name

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Hero Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
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

                        // Gradient fading into background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x66000000),
                                            Color.Transparent,
                                            AkiraBackground
                                        )
                                    )
                                )
                        )

                        // Floating Back Button
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 16.dp)
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
                    }
                }

                // Info Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AkiraTextPrimary
                        )

                        if (!d.nativeName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = d.nativeName,
                                fontSize = 14.sp,
                                color = AkiraTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Pills Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (d.score != null && d.score > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFFBEB))
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
                                            color = Color(0xFF92400E),
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
                                        .background(Color(0xFFF3E8FF))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = d.type.uppercase(),
                                        color = AkiraPrimary,
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
                                        .background(Color(0xFFEDE8F5))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = d.status.uppercase(),
                                        color = AkiraTextPrimary,
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
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 12.sp,
                                            color = AkiraTextSecondary,
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
                                color = AkiraTextPrimary
                            )
                        }

                        // Resume button if available
                        if (lastWatchedEp != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AkiraPrimary)
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
                                color = AkiraTextPrimary
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEDE8F5))
                                    .padding(3.dp)
                            ) {
                                listOf("SUB", "DUB", "RAW").forEach { tab ->
                                    val isSelected = selectedAudioTab == tab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) AkiraPrimary else Color.Transparent)
                                            .clickable { selectedAudioTab = tab }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tab,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else AkiraTextSecondary
                                        )
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
                            Text(text = "No episodes available for $selectedAudioTab", color = AkiraTextSecondary)
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
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .shadow(2.dp, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCurrentLoading) AkiraPrimary else Color.White)
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
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
                                                    color = AkiraTextPrimary,
                                                    fontWeight = FontWeight.Bold,
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
