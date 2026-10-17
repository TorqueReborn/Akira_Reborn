package com.ghostreborn.akira.features.downloads.models

/**
 * Status of an episode download.
 */
enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Represents a downloaded or actively downloading episode.
 */
data class DownloadedEpisode(
    val animeId: String,
    val episodeNumber: String,
    val animeTitle: String,
    val thumbnail: String? = null,
    val videoPath: String, // Absolute path to the downloaded MP4 / media file
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val quality: String = "1080p",
    val error: String? = null,
    val downloadUrl: String = "",
    val completedAt: Long = 0L
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
