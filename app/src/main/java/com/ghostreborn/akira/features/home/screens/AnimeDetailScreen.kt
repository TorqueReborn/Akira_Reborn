package com.ghostreborn.akira.features.home.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghostreborn.akira.features.home.viewmodels.AnimeDetailViewModel
import com.ghostreborn.akira.features.home.widgets.AnimeHeader
import com.ghostreborn.akira.features.home.widgets.AnimeSynopsis
import com.ghostreborn.akira.features.home.widgets.EpisodeGrid
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.ui.theme.AkiraPrimary

@Composable
fun AnimeDetailScreen(
    animeId: String,
    titleHint: String? = null,
    posterHint: String? = null,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (String, List<StreamSource>, Long, List<String>, String) -> Unit,
    viewModel: AnimeDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(animeId) {
        viewModel.loadAnimeDetail(context, animeId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12121A))
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = AkiraPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: "Error loading details",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.animeDetail != null -> {
                val detail = uiState.animeDetail!!
                val episodesList = if (detail.episodesDetail.sub.isNotEmpty()) detail.episodesDetail.sub else detail.episodesDetail.dub

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimeHeader(
                        detail = detail,
                        isFavorite = uiState.isFavorite,
                        onToggleFavorite = { viewModel.toggleFavorite(context) }
                    )

                    AnimeSynopsis(
                        synopsis = detail.description,
                        genres = detail.genres
                    )

                    EpisodeGrid(
                        episodes = episodesList,
                        onEpisodeClick = { epNum ->
                            viewModel.fetchEpisodeStreams(
                                context = context,
                                episodeString = epNum,
                                onSuccess = { streams ->
                                    onPlayEpisode(
                                        epNum,
                                        streams,
                                        0L,
                                        episodesList,
                                        "sub"
                                    )
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (uiState.isFetchingStreams) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AkiraPrimary)
            }
        }
    }
}
