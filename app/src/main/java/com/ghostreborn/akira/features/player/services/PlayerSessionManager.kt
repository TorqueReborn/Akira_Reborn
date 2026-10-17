package com.ghostreborn.akira.features.player.services

import com.ghostreborn.akira.features.player.models.StreamSource
import java.util.concurrent.ConcurrentHashMap

/**
 * Scoped session cache for passing streaming metadata, sources, and state
 * between screens without using global static mutable state on NavHost.
 */
object PlayerSessionManager {

    private data class SessionData(
        val sources: List<StreamSource>,
        val initialPositionMs: Long,
        val thumbnail: String?,
        val episodes: List<String>,
        val translationType: String,
        val malId: Long? = null,
        val aniListId: Long? = null
    )

    private val sessions = ConcurrentHashMap<String, SessionData>()

    private fun sessionKey(showId: String, episodeNumber: String): String = "$showId:$episodeNumber"

    fun setSession(
        showId: String,
        episodeNumber: String,
        sources: List<StreamSource>,
        initialPositionMs: Long = 0L,
        thumbnail: String? = null,
        episodes: List<String> = emptyList(),
        translationType: String = "sub",
        malId: Long? = null,
        aniListId: Long? = null
    ) {
        sessions[sessionKey(showId, episodeNumber)] = SessionData(
            sources = sources,
            initialPositionMs = initialPositionMs,
            thumbnail = thumbnail,
            episodes = episodes,
            translationType = translationType,
            malId = malId,
            aniListId = aniListId
        )
    }

    fun getSources(showId: String, episodeNumber: String): List<StreamSource> {
        return sessions[sessionKey(showId, episodeNumber)]?.sources ?: emptyList()
    }

    fun getInitialPositionMs(showId: String, episodeNumber: String): Long {
        return sessions[sessionKey(showId, episodeNumber)]?.initialPositionMs ?: 0L
    }

    fun getThumbnail(showId: String, episodeNumber: String): String? {
        return sessions[sessionKey(showId, episodeNumber)]?.thumbnail
    }

    fun getEpisodes(showId: String, episodeNumber: String): List<String> {
        return sessions[sessionKey(showId, episodeNumber)]?.episodes ?: emptyList()
    }

    fun getTranslationType(showId: String, episodeNumber: String): String {
        return sessions[sessionKey(showId, episodeNumber)]?.translationType ?: "sub"
    }

    fun getMalId(showId: String, episodeNumber: String): Long? {
        return sessions[sessionKey(showId, episodeNumber)]?.malId
    }

    fun getAniListId(showId: String, episodeNumber: String): Long? {
        return sessions[sessionKey(showId, episodeNumber)]?.aniListId
    }

    fun clearSession(showId: String, episodeNumber: String) {
        sessions.remove(sessionKey(showId, episodeNumber))
    }
}
