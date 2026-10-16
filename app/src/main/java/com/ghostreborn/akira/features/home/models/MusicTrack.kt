package com.ghostreborn.akira.features.home.models

import com.ghostreborn.akira.utils.ImageUtils
import org.json.JSONObject

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val coverUrl: String?,
    val durationSeconds: Int,
    val audioUrl: String?,
    val type: String?, // e.g. "Opening", "Ending", "OST", "Insert Song"
    val showId: String?,
    val showTitle: String?
) {
    companion object {
        fun fromJson(json: JSONObject): MusicTrack {
            val id = json.optString("_id", "")
            val title = when {
                json.has("musicTitle") -> json.optString("musicTitle")
                json.has("title") -> json.optString("title")
                else -> "Unknown Track"
            }
            val artist = json.optString("artist").takeIf { it.isNotBlank() }
            val album = json.optString("album").takeIf { it.isNotBlank() }
            val rawCover = json.optString("cover").takeIf { it.isNotBlank() }
                ?: json.optString("thumbnail").takeIf { it.isNotBlank() }
            val coverUrl = ImageUtils.resolveUrl(rawCover)
            val durationSeconds = json.optInt("duration", 0)
            val type = json.optString("type").takeIf { it.isNotBlank() }

            // Extract audio streaming URL
            var audioUrl: String? = null
            val urlsArr = json.optJSONArray("musicUrls")
            if (urlsArr != null && urlsArr.length() > 0) {
                val first = urlsArr.opt(0)
                if (first is JSONObject) {
                    audioUrl = first.optString("url").takeIf { it.isNotBlank() }
                } else if (first is String && first.isNotBlank()) {
                    audioUrl = first
                }
            }
            if (audioUrl == null) {
                audioUrl = json.optString("musicUrl").takeIf { it.isNotBlank() }
                    ?: json.optString("src").takeIf { it.isNotBlank() }
                    ?: json.optString("url").takeIf { it.isNotBlank() }
            }
            if (!audioUrl.isNullOrBlank()) {
                audioUrl = ImageUtils.resolveUrl(audioUrl)
            }

            val showObj = json.optJSONObject("show")
            val showId = showObj?.optString("showId")?.takeIf { it.isNotBlank() }
                ?: showObj?.optString("_id")?.takeIf { it.isNotBlank() }
            val showTitle = showObj?.optString("name")?.takeIf { it.isNotBlank() }

            return MusicTrack(
                id = id,
                title = title,
                artist = artist,
                album = album,
                coverUrl = coverUrl,
                durationSeconds = durationSeconds,
                audioUrl = audioUrl,
                type = type,
                showId = showId,
                showTitle = showTitle
            )
        }
    }
}
