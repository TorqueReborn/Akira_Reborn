package com.ghostreborn.akira.features.manga.data

import android.content.Context
import android.content.SharedPreferences
import com.ghostreborn.akira.features.manga.models.MangaReadingProgress
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists and retrieves reading progress for manga titles.
 */
object MangaHistoryManager {
    private const val PREFS_NAME = "akira_manga_history"
    private const val KEY_PROGRESS_LIST = "manga_reading_progress_list"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs!!
    }

    fun saveProgress(context: Context, progress: MangaReadingProgress) {
        val list = getProgressList(context).toMutableList()
        list.removeAll { it.mangaId == progress.mangaId }
        list.add(0, progress)

        val trimmed = if (list.size > 50) list.take(50) else list
        val array = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("mangaId", item.mangaId)
                put("lastChapterString", item.lastChapterString)
                put("lastPageNumber", item.lastPageNumber)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_PROGRESS_LIST, array.toString()).apply()
    }

    fun getProgress(context: Context, mangaId: String): MangaReadingProgress? {
        return getProgressList(context).firstOrNull { it.mangaId == mangaId }
    }

    fun getProgressList(context: Context): List<MangaReadingProgress> {
        val raw = getPrefs(context).getString(KEY_PROGRESS_LIST, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<MangaReadingProgress>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(
                    MangaReadingProgress(
                        mangaId = obj.optString("mangaId", ""),
                        lastChapterString = obj.optString("lastChapterString", ""),
                        lastPageNumber = obj.optInt("lastPageNumber", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
