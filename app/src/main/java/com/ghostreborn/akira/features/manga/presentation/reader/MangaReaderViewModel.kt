package com.ghostreborn.akira.features.manga.presentation.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.features.manga.data.MangaRepository
import com.ghostreborn.akira.features.manga.models.MangaPage
import com.ghostreborn.akira.features.manga.models.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MangaReaderUiState {
    data object Loading : MangaReaderUiState
    data class Error(val message: String) : MangaReaderUiState
    data class Success(
        val mangaId: String,
        val chapterString: String,
        val pages: List<MangaPage>,
        val currentPage: Int = 1,
        val readingMode: ReadingMode = ReadingMode.WEBTOON,
        val showControls: Boolean = true
    ) : MangaReaderUiState
}

class MangaReaderViewModel(
    private val mangaRepository: MangaRepository = ServiceLocator.mangaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MangaReaderUiState>(MangaReaderUiState.Loading)
    val uiState: StateFlow<MangaReaderUiState> = _uiState.asStateFlow()

    fun loadChapter(
        context: Context,
        mangaId: String,
        chapterString: String,
        translationType: String = "sub"
    ) {
        _uiState.value = MangaReaderUiState.Loading
        viewModelScope.launch {
            try {
                val pages = mangaRepository.fetchChapterPages(
                    context = context,
                    mangaId = mangaId,
                    chapterString = chapterString,
                    translationType = translationType
                )

                if (pages.isEmpty()) {
                    _uiState.value = MangaReaderUiState.Error("No pages found for this chapter.")
                } else {
                    _uiState.value = MangaReaderUiState.Success(
                        mangaId = mangaId,
                        chapterString = chapterString,
                        pages = pages
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MangaReaderUiState.Error(e.message ?: "Failed to load chapter pages")
            }
        }
    }

    fun toggleControls() {
        val current = _uiState.value
        if (current is MangaReaderUiState.Success) {
            _uiState.update { current.copy(showControls = !current.showControls) }
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        val current = _uiState.value
        if (current is MangaReaderUiState.Success) {
            _uiState.update { current.copy(readingMode = mode) }
        }
    }

    fun onPageChanged(context: Context, page: Int) {
        val current = _uiState.value
        if (current is MangaReaderUiState.Success) {
            _uiState.update { current.copy(currentPage = page) }
            com.ghostreborn.akira.features.manga.data.MangaHistoryManager.saveProgress(
                context = context,
                progress = com.ghostreborn.akira.features.manga.models.MangaReadingProgress(
                    mangaId = current.mangaId,
                    lastChapterString = current.chapterString,
                    lastPageNumber = page
                )
            )
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MangaReaderViewModel() as T
            }
        }
    }
}
