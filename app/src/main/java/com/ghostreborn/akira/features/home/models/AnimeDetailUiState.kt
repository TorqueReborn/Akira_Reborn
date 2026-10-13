package com.ghostreborn.akira.features.home.models

import com.ghostreborn.akira.features.player.models.StreamSource

data class AnimeDetailUiState(
    val isLoading: Boolean = true,
    val animeDetail: AnimeDetail? = null,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false,
    val selectedEpisodeNumber: String? = null,
    val isFetchingStreams: Boolean = false,
    val episodeStreams: List<StreamSource> = emptyList()
)
