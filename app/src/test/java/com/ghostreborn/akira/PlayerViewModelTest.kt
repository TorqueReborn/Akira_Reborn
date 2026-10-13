package com.ghostreborn.akira

import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.viewmodels.PlayerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class PlayerViewModelTest {

    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        viewModel = PlayerViewModel()
    }

    @Test
    fun initWithArgs_populatesUiStateCorrectly() {
        val sources = listOf(
            StreamSource(sourceName = "Source 1", url = "https://example.com/1.m3u8"),
            StreamSource(sourceName = "Source 2", url = "https://example.com/2.m3u8")
        )

        viewModel.initWithArgs(
            showId = "anime_123",
            title = "Test Anime",
            episodeNumber = "1",
            sources = sources,
            thumbnailUrl = "https://example.com/thumb.jpg",
            initialPositionMs = 5000L,
            episodes = listOf("1", "2", "3"),
            translationType = "sub"
        )

        val state = viewModel.uiState.value
        assertEquals("anime_123", state.showId)
        assertEquals("Test Anime", state.title)
        assertEquals("1", state.episodeNumber)
        assertEquals(2, state.sources.size)
        assertEquals(0, state.selectedSourceIndex)
        assertNotNull(state.currentSource)
        assertEquals("Source 1", state.currentSource?.sourceName)
    }

    @Test
    fun selectSourceIndex_updatesSelectedSource() {
        val sources = listOf(
            StreamSource(sourceName = "Source 1", url = "https://example.com/1.m3u8"),
            StreamSource(sourceName = "Source 2", url = "https://example.com/2.m3u8")
        )

        viewModel.initWithArgs(
            showId = "anime_123",
            title = "Test Anime",
            episodeNumber = "1",
            sources = sources,
            thumbnailUrl = null,
            initialPositionMs = 0L,
            episodes = emptyList(),
            translationType = "sub"
        )

        viewModel.selectSourceIndex(1)
        assertEquals(1, viewModel.uiState.value.selectedSourceIndex)
        assertEquals("Source 2", viewModel.uiState.value.currentSource?.sourceName)
    }
}
