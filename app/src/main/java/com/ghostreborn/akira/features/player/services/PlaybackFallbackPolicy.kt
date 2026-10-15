package com.ghostreborn.akira.features.player.services

import com.ghostreborn.akira.features.player.models.StreamSource

/**
 * Chooses the next source to attempt after a playback failure.
 * Embedded webpage URLs are intentionally excluded from automatic fallback because
 * ExoPlayer needs a directly playable media URL (for example MP4 or HLS).
 */
internal object PlaybackFallbackPolicy {
    fun nextUntriedDirectSourceIndex(
        sources: List<StreamSource>,
        failedSourceIndices: Set<Int>
    ): Int? = sources.indices.firstOrNull { index ->
        val source = sources[index]
        val looksLikeDirectMedia = source.isDirect ||
            source.isHls ||
            source.fileExtension.equals("mp4", ignoreCase = true) ||
            source.fileExtension.equals("m3u8", ignoreCase = true)

        index !in failedSourceIndices && looksLikeDirectMedia
    }
}
