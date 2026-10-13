package com.ghostreborn.akira.features.home.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.home.services.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val topRanked: List<AnimeShow> = emptyList(),
    val browseList: List<AnimeShow> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: AnimeRepository = ServiceLocator.animeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData(context: Context) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val top = repository.fetchTopRankedAnime(context = context)
                val browse = repository.fetchAnimeList(page = 1, context = context)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        topRanked = top,
                        browseList = browse.shows
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load home feed: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(context: Context, query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            try {
                val result = repository.fetchAnimeList(
                    page = 1,
                    searchQuery = query.ifBlank { null },
                    context = context
                )
                _uiState.update { it.copy(browseList = result.shows) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
