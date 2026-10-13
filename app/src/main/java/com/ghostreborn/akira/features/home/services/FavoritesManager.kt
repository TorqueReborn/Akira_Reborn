package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteAnime(
    val animeId: String,
    val title: String,
    val poster: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("animeId", animeId)
        put("title", title)
        put("poster", poster ?: "")
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): FavoriteAnime = FavoriteAnime(
            animeId = json.optString("animeId", ""),
            title = json.optString("title", ""),
            poster = json.optString("poster").takeIf { it.isNotBlank() },
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

object FavoritesManager {
    private const val PREFS_NAME = "akira_favorites"
    private const val KEY_FAVORITES = "favorite_anime_list"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs!!
    }

    fun isFavorite(context: Context, animeId: String): Boolean {
        if (animeId.isBlank()) return false
        val list = getFavorites(context)
        return list.any { it.animeId == animeId }
    }

    fun getFavorites(context: Context): List<FavoriteAnime> {
        val raw = getPrefs(context).getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<FavoriteAnime>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) list.add(FavoriteAnime.fromJson(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toggleFavorite(context: Context, animeId: String, title: String, poster: String?): Boolean {
        if (animeId.isBlank()) return false
        val list = getFavorites(context).toMutableList()
        val index = list.indexOfFirst { it.animeId == animeId }
        val nowFavorite = if (index >= 0) {
            list.removeAt(index)
            false
        } else {
            list.add(0, FavoriteAnime(animeId, title, poster))
            true
        }

        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_FAVORITES, jsonArray.toString()).apply()
        return nowFavorite
    }
}
