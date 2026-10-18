package com.ghostreborn.akira.features.home.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.models.AnimeDetailUiState
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.home.services.FavoritesManager
import com.ghostreborn.akira.features.player.models.StreamSource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnimeDetailViewModel(
    private val repository: AnimeRepository = ServiceLocator.animeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailUiState())
    val uiState: StateFlow<AnimeDetailUiState> = _uiState.asStateFlow()

    fun loadAnimeDetail(context: Context, animeId: String) {
        if (animeId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val detail = repository.fetchAnimeDetail(animeId, context)
                val fav = FavoritesManager.isFavorite(context, animeId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        animeDetail = detail,
                        isFavorite = fav
                    )
                }

                // Load cloud watch status asynchronously
                if (TokenManager.isLoggedIn(context)) {
                    val isManga = detail.type?.equals("manga", ignoreCase = true) == true
                    val ws = com.ghostreborn.akira.features.auth.services.WatchlistSyncManager.getWatchState(
                        context = context,
                        showId = animeId,
                        isManga = isManga
                    )
                    if (ws != null) {
                        _uiState.update {
                            it.copy(
                                watchStatus = ws.userStatus,
                                watchStateId = ws.watchStateId
                            )
                        }
                    }
                }

                // Load guides and world maps asynchronously
                val isManga = detail.type?.equals("manga", ignoreCase = true) == true
                launch {
                    val guidesDeferred = async {
                        try {
                            com.ghostreborn.akira.features.home.services.GuideRepository.fetchGuidesForMedia(
                                context = context,
                                showId = if (!isManga) animeId else null,
                                mangaId = if (isManga) animeId else null
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }

                    val mapsDeferred = async {
                        try {
                            com.ghostreborn.akira.features.home.services.GuideRepository.fetchWorldMaps(
                                context = context,
                                showId = animeId
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }

                    val guides = guidesDeferred.await()
                    val maps = mapsDeferred.await()

                    // If backend community guides are empty, auto-generate official franchise watch order guide from relations!
                    val effectiveGuides = if (guides.isNotEmpty()) {
                        guides
                    } else {
                        try {
                            listOf(com.ghostreborn.akira.features.home.services.GuideRepository.buildFranchiseWatchGuide(context, detail))
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }

                    _uiState.update {
                        it.copy(
                            guides = effectiveGuides,
                            worldMaps = maps,
                            isLoadingGuides = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load details: ${e.message}"
                    )
                }
            }
        }
    }

    fun setWatchStatus(context: Context, newStatus: com.ghostreborn.akira.features.auth.services.WatchStatus) {
        val stateId = _uiState.value.watchStateId ?: return
        _uiState.update { it.copy(isUpdatingWatchStatus = true) }

        viewModelScope.launch {
            val success = com.ghostreborn.akira.features.auth.services.WatchlistSyncManager.setWatchStatus(
                context = context,
                watchStateId = stateId,
                status = newStatus
            )
            _uiState.update {
                it.copy(
                    isUpdatingWatchStatus = false,
                    watchStatus = if (success) newStatus else it.watchStatus
                )
            }
        }
    }

    fun toggleFavorite(context: Context) {
        val detail = _uiState.value.animeDetail ?: return
        val newFavState = FavoritesManager.toggleFavorite(
            context = context,
            animeId = detail.id,
            title = detail.name,
            poster = detail.thumbnail
        )
        _uiState.update { it.copy(isFavorite = newFavState) }
    }

    fun fetchEpisodeStreams(
        context: Context,
        episodeString: String,
        translationType: String = "sub",
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (String) -> Unit
    ) {
        val detail = _uiState.value.animeDetail ?: return
        _uiState.update { it.copy(isFetchingStreams = true, selectedEpisodeNumber = episodeString) }

        viewModelScope.launch {
            try {
                val token = TokenManager.getAccessToken(context)
                val result = repository.fetchEpisodeStreams(
                    context = context,
                    showId = detail.id,
                    episodeString = episodeString,
                    translationType = translationType,
                    authToken = token
                )
                _uiState.update {
                    it.copy(
                        isFetchingStreams = false,
                        episodeStreams = result.streams
                    )
                }
                onSuccess(result.streams)
            } catch (e: Exception) {
                val msg = "Error loading episode $episodeString: ${e.message}"
                _uiState.update { it.copy(isFetchingStreams = false) }
                onError(msg)
            }
        }
    }
}
