package com.ghostreborn.akira.core.backup

import android.content.Context
import android.net.Uri
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.FavoriteAnime
import com.ghostreborn.akira.features.home.services.FavoritesManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.services.AnimeProgress
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Handles exporting and importing user data:
 * - Favorites
 * - Watch History
 * - Content Preferences (Adult, Ecchi, Unrated filters)
 * - User Authentication Session (optional/included)
 */
object BackupManager {

    private const val BACKUP_VERSION = 1

    /**
     * Creates a JSON string of all current user data.
     */
    suspend fun createBackupJson(context: Context, includeAuth: Boolean = true): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Favorites
        val favorites = FavoritesManager.getFavorites(context)
        val favArray = JSONArray()
        favorites.forEach { favArray.put(it.toJson()) }
        root.put("favorites", favArray)

        // 2. Watch History
        val history = WatchHistoryManager.getProgressList(context)
        val histArray = JSONArray()
        history.forEach { histArray.put(it.toJson()) }
        root.put("watchHistory", histArray)

        // 3. Content Preferences
        AnimeRepository.loadContentPreferences(context)
        val prefsObj = JSONObject().apply {
            put("allowAdult", AnimeRepository.allowAdult)
            put("denyEcchi", AnimeRepository.denyEcchi)
            put("allowUnknown", AnimeRepository.allowUnknown)
        }
        root.put("contentPreferences", prefsObj)

        // 4. Auth Session
        if (includeAuth && TokenManager.isLoggedIn(context)) {
            val authObj = JSONObject().apply {
                put("accessToken", TokenManager.getAccessToken(context) ?: "")
                put("refreshToken", TokenManager.getRefreshToken(context) ?: "")
                put("sessionId", TokenManager.getSessionId(context) ?: "")
                put("username", TokenManager.getUsername(context) ?: "")
                put("displayName", TokenManager.getDisplayName(context) ?: "")
                put("picture", TokenManager.getPicture(context) ?: "")
                put("userId", TokenManager.getUserId(context) ?: "")
                put("email", TokenManager.getEmail(context) ?: "")
                put("isEmailVerified", TokenManager.isEmailVerified(context))
            }
            root.put("authSession", authObj)
        }

        root.toString(2)
    }

    /**
     * Writes backup JSON to the given URI.
     */
    suspend fun exportToFile(context: Context, uri: Uri, includeAuth: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = createBackupJson(context, includeAuth)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Unable to open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads and restores data from the given backup URI.
     * Returns true if user authentication was restored (so UI can navigate to home if in login).
     */
    suspend fun importFromFile(context: Context, uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Unable to open input stream"))

            val root = JSONObject(content.toString())

            // 1. Restore Favorites
            if (root.has("favorites")) {
                val favArray = root.getJSONArray("favorites")
                val favoritesList = mutableListOf<FavoriteAnime>()
                for (i in 0 until favArray.length()) {
                    val obj = favArray.optJSONObject(i)
                    if (obj != null) {
                        favoritesList.add(FavoriteAnime.fromJson(obj))
                    }
                }
                // Save directly to favorites prefs
                val favPrefs = context.applicationContext.getSharedPreferences("akira_favorites", Context.MODE_PRIVATE)
                val newJsonArray = JSONArray()
                favoritesList.forEach { newJsonArray.put(it.toJson()) }
                favPrefs.edit().putString("favorite_anime_list", newJsonArray.toString()).apply()
            }

            // 2. Restore Watch History
            if (root.has("watchHistory")) {
                val histArray = root.getJSONArray("watchHistory")
                val histList = mutableListOf<AnimeProgress>()
                for (i in 0 until histArray.length()) {
                    val obj = histArray.optJSONObject(i)
                    if (obj != null) {
                        histList.add(AnimeProgress.fromJson(obj))
                    }
                }
                val histPrefs = context.applicationContext.getSharedPreferences("akira_watch_history", Context.MODE_PRIVATE)
                val newJsonArray = JSONArray()
                histList.forEach { newJsonArray.put(it.toJson()) }
                histPrefs.edit().putString("anime_watch_progress_list", newJsonArray.toString()).apply()
            }

            // 3. Restore Content Preferences
            if (root.has("contentPreferences")) {
                val prefsObj = root.getJSONObject("contentPreferences")
                val allowAdult = prefsObj.optBoolean("allowAdult", false)
                val denyEcchi = prefsObj.optBoolean("denyEcchi", false)
                val allowUnknown = prefsObj.optBoolean("allowUnknown", false)

                AnimeRepository.updateContentPreferences(
                    context = context,
                    newAllowAdult = allowAdult,
                    newDenyEcchi = denyEcchi,
                    newAllowUnknown = allowUnknown
                )
            }

            // 4. Restore Auth Session if present
            var authRestored = false
            if (root.has("authSession")) {
                val authObj = root.getJSONObject("authSession")
                val accessToken = authObj.optString("accessToken", "")
                if (accessToken.isNotBlank()) {
                    TokenManager.saveAuthData(
                        context = context,
                        accessToken = accessToken,
                        refreshToken = authObj.optString("refreshToken", ""),
                        sessionId = authObj.optString("sessionId", ""),
                        username = authObj.optString("username", "Otaku Member"),
                        displayName = authObj.optString("displayName").takeIf { it.isNotBlank() },
                        picture = authObj.optString("picture").takeIf { it.isNotBlank() },
                        userId = authObj.optString("userId").takeIf { it.isNotBlank() },
                        email = authObj.optString("email").takeIf { it.isNotBlank() },
                        isEmailVerified = authObj.optBoolean("isEmailVerified", false)
                    )
                    authRestored = true
                }
            }

            Result.success(authRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
