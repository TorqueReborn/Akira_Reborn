package com.ghostreborn.akira.features.home.models

import com.ghostreborn.akira.utils.ImageUtils
import org.json.JSONObject

data class AnimeShow(
    val id: String,
    val name: String,
    val englishName: String? = null,
    val nativeName: String? = null,
    val thumbnail: String? = null,
    val banner: String? = null,
    val type: String? = null,
    val seasonQuarter: String? = null,
    val seasonYear: Int? = null,
    val score: Double? = null,
    val availableEpisodesSub: Int = 0,
    val availableEpisodesDub: Int = 0,
    val lastEpisodeSub: String? = null,
    val lastEpisodeDub: String? = null,
    val views: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): AnimeShow {
            val id = json.optString("_id", "")
            val name = when {
                json.has("name") -> json.optString("name")
                json.has("title") -> json.optString("title")
                else -> "Unknown"
            }

            val englishName = json.optString("englishName").takeIf { it.isNotBlank() }
            val nativeName = json.optString("nativeName").takeIf { it.isNotBlank() }

            val thumbCandidate = when {
                json.has("thumbnail") -> json.optString("thumbnail")
                json.has("cover") -> json.optString("cover")
                else -> null
            }
            val thumbnail = ImageUtils.resolveUrl(thumbCandidate)

            val bannerCandidate = when {
                json.has("banner") -> json.optString("banner")
                json.has("bannerImage") -> json.optString("bannerImage")
                json.has("bannerUrl") -> json.optString("bannerUrl")
                else -> null
            }
            val banner = ImageUtils.resolveUrl(bannerCandidate)

            val type = when {
                json.has("type") -> json.optString("type")
                json.has("format") -> json.optString("format")
                else -> null
            }?.takeIf { it.isNotBlank() }

            val seasonObj = json.optJSONObject("season")
            val seasonQuarter = seasonObj?.optString("quarter")?.takeIf { it.isNotBlank() }
            val seasonYear = seasonObj?.optInt("year")?.takeIf { it > 0 }

            val score = if (json.has("score")) json.optDouble("score").takeIf { !it.isNaN() && it > 0 } else null

            val availableEpisodesObj = json.optJSONObject("availableEpisodes")
            val availableEpisodesSub = availableEpisodesObj?.optInt("sub", 0) ?: 0
            val availableEpisodesDub = availableEpisodesObj?.optInt("dub", 0) ?: 0

            val lastEpisodeInfoObj = json.optJSONObject("lastEpisodeInfo")
            val subInfo = lastEpisodeInfoObj?.optJSONObject("sub")
            val dubInfo = lastEpisodeInfoObj?.optJSONObject("dub")
            val lastEpisodeSub = subInfo?.optString("episodeString")?.takeIf { it.isNotBlank() }
            val lastEpisodeDub = dubInfo?.optString("episodeString")?.takeIf { it.isNotBlank() }

            val pageStatusObj = json.optJSONObject("pageStatus")
            val views = when {
                pageStatusObj?.has("rangeViews") == true -> pageStatusObj.optString("rangeViews")
                pageStatusObj?.has("views") == true -> pageStatusObj.optString("views")
                json.has("views") -> json.optString("views")
                else -> null
            }

            return AnimeShow(
                id = id,
                name = name,
                englishName = englishName,
                nativeName = nativeName,
                thumbnail = thumbnail,
                banner = banner,
                type = type,
                seasonQuarter = seasonQuarter,
                seasonYear = seasonYear,
                score = score,
                availableEpisodesSub = availableEpisodesSub,
                availableEpisodesDub = availableEpisodesDub,
                lastEpisodeSub = lastEpisodeSub,
                lastEpisodeDub = lastEpisodeDub,
                views = views
            )
        }

        fun fromRankedCard(json: JSONObject): AnimeShow {
            val anyCard = json.optJSONObject("anyCard") ?: json
            val pageStatus = json.optJSONObject("pageStatus")

            val id = anyCard.optString("_id", "")
            val name = when {
                anyCard.has("name") -> anyCard.optString("name")
                anyCard.has("title") -> anyCard.optString("title")
                else -> "Unknown"
            }

            val englishName = anyCard.optString("englishName").takeIf { it.isNotBlank() }
            val nativeName = anyCard.optString("nativeName").takeIf { it.isNotBlank() }

            val thumbCandidate = when {
                anyCard.has("thumbnail") -> anyCard.optString("thumbnail")
                anyCard.has("cover") -> anyCard.optString("cover")
                else -> null
            }
            val thumbnail = ImageUtils.resolveUrl(thumbCandidate)

            val bannerCandidate = when {
                anyCard.has("banner") -> anyCard.optString("banner")
                anyCard.has("bannerImage") -> anyCard.optString("bannerImage")
                anyCard.has("bannerUrl") -> anyCard.optString("bannerUrl")
                else -> null
            }
            val banner = ImageUtils.resolveUrl(bannerCandidate)

            val score = if (anyCard.has("score")) anyCard.optDouble("score").takeIf { !it.isNaN() && it > 0 } else null

            val availableEpisodesObj = anyCard.optJSONObject("availableEpisodes")
            val availableEpisodesSub = availableEpisodesObj?.optInt("sub", 0) ?: 0
            val availableEpisodesDub = availableEpisodesObj?.optInt("dub", 0) ?: 0

            val airedStart = anyCard.optJSONObject("airedStart")
            val seasonYear = airedStart?.optInt("year")?.takeIf { it > 0 }

            val views = when {
                pageStatus?.has("rangeViews") == true -> pageStatus.optString("rangeViews")
                pageStatus?.has("views") == true -> pageStatus.optString("views")
                else -> null
            }

            return AnimeShow(
                id = id,
                name = name,
                englishName = englishName,
                nativeName = nativeName,
                thumbnail = thumbnail,
                banner = banner,
                type = "TV",
                seasonQuarter = null,
                seasonYear = seasonYear,
                score = score,
                availableEpisodesSub = availableEpisodesSub,
                availableEpisodesDub = availableEpisodesDub,
                views = views
            )
        }
    }
}

data class AnimePageResult(
    val shows: List<AnimeShow>,
    val total: Int,
    val page: Int
)
