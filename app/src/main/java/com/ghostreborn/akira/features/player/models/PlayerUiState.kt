package com.ghostreborn.akira.features.player.models

data class PlayerUiState(
    val showId: String = "",
    val title: String = "",
    val episodeNumber: String = "",
    val sources: List<StreamSource> = emptyList(),
    val selectedSourceIndex: Int = 0,
    val thumbnailUrl: String? = null,
    val initialPositionMs: Long = 0L,
    val episodes: List<String> = emptyList(),
    val translationType: String = "sub",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackStateText: String? = null,
    val errorMessage: String? = null,
    val isControlsVisible: Boolean = true,
    val isQualityPickerOpen: Boolean = false,
    val isEpisodeDrawerOpen: Boolean = false,
    val isFetchingEpisode: Boolean = false,
    val skipIntervals: List<SkipInterval> = emptyList(),
    val activeSkipInterval: SkipInterval? = null
) {
    val currentSource: StreamSource?
        get() = sources.getOrNull(selectedSourceIndex)
}
