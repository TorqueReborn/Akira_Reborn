package com.ghostreborn.akira.features.player.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.PlayerUiState
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: AnimeRepository = ServiceLocator.animeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initWithArgs(
        showId: String,
        title: String,
        episodeNumber: String,
        sources: List<StreamSource>,
        thumbnailUrl: String?,
        initialPositionMs: Long,
        episodes: List<String>,
        translationType: String
    ) {
        _uiState.update {
            it.copy(
                showId = showId,
                title = title,
                episodeNumber = episodeNumber,
                sources = sources,
                selectedSourceIndex = 0,
                thumbnailUrl = thumbnailUrl,
                initialPositionMs = initialPositionMs,
                episodes = episodes,
                translationType = translationType
            )
        }
    }

    fun selectSourceIndex(index: Int) {
        if (index in _uiState.value.sources.indices) {
            _uiState.update { it.copy(selectedSourceIndex = index, errorMessage = null) }
        }
    }

    fun updatePlaybackProgress(currentPositionMs: Long, durationMs: Long, bufferedPercentage: Int, isPlaying: Boolean) {
        _uiState.update {
            it.copy(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPercentage = bufferedPercentage,
                isPlaying = isPlaying
            )
        }
    }

    fun toggleControlsVisibility(visible: Boolean? = null) {
        _uiState.update {
            it.copy(isControlsVisible = visible ?: !it.isControlsVisible)
        }
    }

    fun setQualityPickerOpen(open: Boolean) {
        _uiState.update { it.copy(isQualityPickerOpen = open) }
    }

    fun setEpisodeDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(isEpisodeDrawerOpen = open) }
    }

    fun setError(message: String?) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun saveWatchProgress(context: Context, positionMs: Long, durationMs: Long) {
        val state = _uiState.value
        if (state.showId.isNotBlank() && state.episodeNumber.isNotBlank()) {
            WatchHistoryManager.saveProgress(
                context = context,
                animeId = state.showId,
                animeTitle = state.title,
                episodeNumber = state.episodeNumber,
                seekPositionMs = positionMs,
                totalDurationMs = durationMs,
                thumbnail = state.thumbnailUrl
            )
        }
    }

    fun fetchNextEpisodeStreams(
        context: Context,
        newEpisodeString: String,
        onSuccess: (List<StreamSource>) -> Unit
    ) {
        val state = _uiState.value
        _uiState.update { it.copy(isFetchingEpisode = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val token = TokenManager.getAccessToken(context)
                val result = repository.fetchEpisodeStreams(
                    context = context,
                    showId = state.showId,
                    episodeString = newEpisodeString,
                    translationType = state.translationType,
                    authToken = token
                )
                _uiState.update {
                    it.copy(
                        episodeNumber = newEpisodeString,
                        sources = result.streams,
                        selectedSourceIndex = 0,
                        isFetchingEpisode = false
                    )
                }
                onSuccess(result.streams)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingEpisode = false,
                        errorMessage = "Failed to load episode $newEpisodeString: ${e.message}"
                    )
                }
            }
        }
    }
}
