package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user's private remarks and personal notes for individual anime / manga.
 * Notes are persisted locally and tied to each animeId.
 */
object UserRemarksManager {
    private const val PREFS_NAME = "akira_user_remarks"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
        return prefs!!
    }

    fun getRemarks(context: Context, animeId: String): String {
        if (animeId.isBlank()) return ""
        return getPrefs(context).getString("remark_$animeId", "") ?: ""
    }

    fun saveRemarks(context: Context, animeId: String, remarks: String) {
        if (animeId.isBlank()) return
        val trimmed = remarks.trim()
        if (trimmed.isEmpty()) {
            getPrefs(context).edit().remove("remark_$animeId").apply()
        } else {
            getPrefs(context).edit().putString("remark_$animeId", trimmed).apply()
        }
    }

    fun getAllRemarks(context: Context): Map<String, String> {
        val all = getPrefs(context).all
        val result = mutableMapOf<String, String>()
        for ((key, value) in all) {
            if (key.startsWith("remark_") && value is String && value.isNotBlank()) {
                val animeId = key.removePrefix("remark_")
                result[animeId] = value
            }
        }
        return result
    }

    fun restoreRemarks(context: Context, remarksMap: Map<String, String>) {
        val editor = getPrefs(context).edit()
        for ((animeId, note) in remarksMap) {
            if (animeId.isNotBlank() && note.isNotBlank()) {
                editor.putString("remark_$animeId", note.trim())
            }
        }
        editor.apply()
    }
}
