package com.ghostreborn.akira

import android.content.Context
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.models.AnimePageResult
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.home.services.EpisodeFetchResult
import com.ghostreborn.akira.features.home.viewmodels.AnimeDetailViewModel
import com.ghostreborn.akira.features.player.models.StreamSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAnimeRepository : AnimeRepository {
    override var allowAdult: Boolean = false
    override var denyEcchi: Boolean = false
    override var allowUnknown: Boolean = false

    override fun loadContentPreferences(context: Context) {}
    override fun updateContentPreferences(context: Context, newAllowAdult: Boolean?, newDenyEcchi: Boolean?, newAllowUnknown: Boolean?) {}
    override fun clearCache() {}

    override suspend fun fetchAnimeList(page: Int, limit: Int, translationType: String, listProfile: String, searchQuery: String?, sortBy: String?, countryOrigin: String?, season: String?, seasonYear: Int?, context: Context?): AnimePageResult {
        return AnimePageResult(shows = emptyList(), total = 0, page = page)
    }

    override suspend fun fetchTopRankedAnime(dateRange: Int, size: Int, context: Context?): List<AnimeShow> {
        return emptyList()
    }

    override suspend fun fetchAnimeDetail(animeId: String, context: Context?): AnimeDetail {
        return AnimeDetail(
            id = animeId,
            name = "Fake Anime Detail",
            description = "Fake description"
        )
    }

    override suspend fun fetchEpisodeStreams(context: Context, showId: String, episodeString: String, translationType: String, authToken: String?): EpisodeFetchResult {
        return EpisodeFetchResult(
            streams = listOf(StreamSource(sourceName = "Stream 1", url = "https://example.com/1.m3u8")),
            rawResponse = "",
            tobeparsed = "",
            decryptedJson = "",
            bootstrapEpoch = 0L
        )
    }
}

class AnimeDetailViewModelTest {

    private lateinit var viewModel: AnimeDetailViewModel
    private lateinit var fakeRepository: FakeAnimeRepository

    @Before
    fun setUp() {
        fakeRepository = FakeAnimeRepository()
        viewModel = AnimeDetailViewModel(repository = fakeRepository)
    }

    @Test
    fun initialState_isLoadingTrue() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertFalse(state.isFavorite)
    }
}
