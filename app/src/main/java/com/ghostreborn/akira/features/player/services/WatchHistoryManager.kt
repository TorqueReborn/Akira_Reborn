package com.ghostreborn.akira.features.player.services

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class AnimeProgress(
    val animeId: String,
    val animeTitle: String,
    val episodeNumber: String,
    val seekPositionMs: Long,
    val totalDurationMs: Long,
    val thumbnail: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("animeId", animeId)
        put("animeTitle", animeTitle)
        put("episodeNumber", episodeNumber)
        put("seekPositionMs", seekPositionMs)
        put("totalDurationMs", totalDurationMs)
        put("thumbnail", thumbnail)
        put("lastWatchedTimestamp", lastWatchedTimestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): AnimeProgress = AnimeProgress(
            animeId = json.optString("animeId", ""),
            animeTitle = json.optString("animeTitle", ""),
            episodeNumber = json.optString("episodeNumber", ""),
            seekPositionMs = json.optLong("seekPositionMs", 0L),
            totalDurationMs = json.optLong("totalDurationMs", 0L),
            thumbnail = json.optString("thumbnail").takeIf { it.isNotBlank() },
            lastWatchedTimestamp = json.optLong("lastWatchedTimestamp", System.currentTimeMillis())
        )
    }
}

object WatchHistoryManager {
    private const val PREFS_NAME = "akira_watch_history"
    private const val KEY_HISTORY = "anime_watch_progress_list"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) init(context)
        return prefs!!
    }

    fun getProgressList(context: Context): List<AnimeProgress> {
        val raw = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AnimeProgress>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj != null) list.add(AnimeProgress.fromJson(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getProgress(context: Context, animeId: String): AnimeProgress? {
        val list = getProgressList(context)
        return list.firstOrNull { it.animeId == animeId }
    }

    fun saveProgress(
        context: Context,
        animeId: String,
        animeTitle: String,
        episodeNumber: String,
        seekPositionMs: Long,
        totalDurationMs: Long,
        thumbnail: String? = null
    ) {
        if (animeId.isBlank()) return
        val list = getProgressList(context).toMutableList()

        val existingIndex = list.indexOfFirst { it.animeId == animeId }
        val finalThumb = if (thumbnail.isNullOrBlank() && existingIndex >= 0) {
            list[existingIndex].thumbnail
        } else {
            thumbnail
        }

        val newProgress = AnimeProgress(
            animeId = animeId,
            animeTitle = animeTitle,
            episodeNumber = episodeNumber,
            seekPositionMs = seekPositionMs,
            totalDurationMs = totalDurationMs,
            thumbnail = finalThumb,
            lastWatchedTimestamp = System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            list.removeAt(existingIndex)
        }
        list.add(0, newProgress)

        while (list.size > 50) {
            list.removeAt(list.size - 1)
        }

        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }
}
