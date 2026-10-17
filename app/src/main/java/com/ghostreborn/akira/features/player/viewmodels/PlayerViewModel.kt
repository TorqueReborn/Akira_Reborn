package com.ghostreborn.akira.features.player.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.PlayerUiState
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.AniSkipService
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
                translationType = translationType,
                errorMessage = null,
                isFetchingEpisode = false
            )
        }
    }

    fun selectSourceIndex(index: Int) {
        if (index in _uiState.value.sources.indices) {
            _uiState.update { it.copy(selectedSourceIndex = index, errorMessage = null) }
        }
    }

    fun setSkipIntervals(intervals: List<com.ghostreborn.akira.features.player.models.SkipInterval>) {
        _uiState.update { it.copy(skipIntervals = intervals) }
    }

    fun loadSkipTimes(title: String, episodeNumber: String, explicitMalId: Long? = null, durationMs: Long = 0L) {
        val epNum = episodeNumber.toDoubleOrNull() ?: return
        viewModelScope.launch {
            try {
                android.util.Log.d("PlayerViewModel", "loadSkipTimes for '$title' ep $epNum (explicitMalId: $explicitMalId)")
                val malId = explicitMalId ?: AniSkipService.resolveMalIdFromTitle(title)
                android.util.Log.d("PlayerViewModel", "Resolved MAL ID: $malId for '$title'")
                if (malId != null && malId > 0) {
                    val epLenSec = if (durationMs > 0) durationMs / 1000L else 1440L
                    val intervals = AniSkipService.getSkipTimes(
                        malId = malId,
                        episodeNumber = epNum,
                        episodeLengthSeconds = epLenSec
                    )
                    android.util.Log.d("PlayerViewModel", "Found ${intervals.size} skip intervals from AniSkip")
                    setSkipIntervals(intervals)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "Error loading skip times: ${e.message}")
            }
        }
    }

    fun updatePlaybackProgress(currentPositionMs: Long, durationMs: Long, bufferedPercentage: Int, isPlaying: Boolean) {
        _uiState.update { state ->
            val active = state.skipIntervals.firstOrNull { it.contains(currentPositionMs) }
            state.copy(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPercentage = bufferedPercentage,
                isPlaying = isPlaying,
                activeSkipInterval = active
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

    fun saveWatchProgress(
        context: Context,
        positionMs: Long,
        durationMs: Long,
        showIdOverride: String? = null,
        titleOverride: String? = null,
        episodeNumberOverride: String? = null,
        thumbnailOverride: String? = null
    ) {
        val state = _uiState.value
        val showId = showIdOverride?.takeIf { it.isNotBlank() } ?: state.showId
        val title = titleOverride?.takeIf { it.isNotBlank() } ?: state.title
        val episodeNumber = episodeNumberOverride?.takeIf { it.isNotBlank() } ?: state.episodeNumber
        val thumbnail = thumbnailOverride ?: state.thumbnailUrl

        if (showId.isNotBlank() && episodeNumber.isNotBlank()) {
            WatchHistoryManager.saveProgress(
                context = context,
                animeId = showId,
                animeTitle = title,
                episodeNumber = episodeNumber,
                seekPositionMs = positionMs,
                totalDurationMs = durationMs,
                thumbnail = thumbnail
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
