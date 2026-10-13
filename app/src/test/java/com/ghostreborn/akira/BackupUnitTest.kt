package com.ghostreborn.akira

import com.ghostreborn.akira.features.home.services.FavoriteAnime
import com.ghostreborn.akira.features.player.services.AnimeProgress
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUnitTest {

    @Test
    fun testFavoriteSerializationAndDeserialization() {
        val favorite = FavoriteAnime(
            animeId = "anime_123",
            title = "Solo Leveling",
            poster = "https://example.com/poster.jpg",
            timestamp = 1700000000000L
        )

        val json = favorite.toJson()
        assertEquals("anime_123", json.getString("animeId"))
        assertEquals("Solo Leveling", json.getString("title"))
        assertEquals("https://example.com/poster.jpg", json.getString("poster"))

        val restored = FavoriteAnime.fromJson(json)
        assertEquals(favorite.animeId, restored.animeId)
        assertEquals(favorite.title, restored.title)
        assertEquals(favorite.poster, restored.poster)
        assertEquals(favorite.timestamp, restored.timestamp)
    }

    @Test
    fun testAnimeProgressSerializationAndDeserialization() {
        val progress = AnimeProgress(
            animeId = "anime_456",
            animeTitle = "Frieren",
            episodeNumber = "12",
            seekPositionMs = 840000L,
            totalDurationMs = 1420000L,
            thumbnail = "https://example.com/thumb.jpg",
            lastWatchedTimestamp = 1700000050000L
        )

        val json = progress.toJson()
        assertEquals("anime_456", json.getString("animeId"))
        assertEquals("Frieren", json.getString("animeTitle"))
        assertEquals("12", json.getString("episodeNumber"))
        assertEquals(840000L, json.getLong("seekPositionMs"))

        val restored = AnimeProgress.fromJson(json)
        assertEquals(progress.animeId, restored.animeId)
        assertEquals(progress.animeTitle, restored.animeTitle)
        assertEquals(progress.episodeNumber, restored.episodeNumber)
        assertEquals(progress.seekPositionMs, restored.seekPositionMs)
        assertEquals(progress.totalDurationMs, restored.totalDurationMs)
        assertEquals(progress.thumbnail, restored.thumbnail)
    }

    @Test
    fun testFullBackupPayloadStructure() {
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            val favArray = JSONArray().apply {
                put(FavoriteAnime("a1", "One Piece", null).toJson())
            }
            put("favorites", favArray)

            val histArray = JSONArray().apply {
                put(AnimeProgress("a1", "One Piece", "1090", 5000L, 1400000L).toJson())
            }
            put("watchHistory", histArray)

            val prefs = JSONObject().apply {
                put("allowAdult", false)
                put("denyEcchi", true)
                put("allowUnknown", false)
            }
            put("contentPreferences", prefs)

            val auth = JSONObject().apply {
                put("accessToken", "mock_token")
                put("username", "OtakuUser")
            }
            put("authSession", auth)
        }

        assertTrue(root.has("favorites"))
        assertTrue(root.has("watchHistory"))
        assertTrue(root.has("contentPreferences"))
        assertTrue(root.has("authSession"))
        assertEquals(1, root.getInt("version"))
        assertEquals(1, root.getJSONArray("favorites").length())
        assertEquals("One Piece", root.getJSONArray("favorites").getJSONObject(0).getString("title"))
    }
}
