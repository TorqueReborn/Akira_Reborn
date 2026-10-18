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
    val displayTitle: String
        get() = listOfNotNull(englishName, name, nativeName)
            .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Anime"

    companion object {
        private fun clean(s: String?): String? = s?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        fun fromJson(json: JSONObject): AnimeShow {
            val id = json.optString("_id", "")
            val rawName = clean(
                when {
                    json.has("name") -> json.optString("name")
                    json.has("title") -> json.optString("title")
                    else -> null
                }
            )

            val englishName = clean(json.optString("englishName"))
            val nativeName = clean(json.optString("nativeName"))

            // Extract first valid alternative name if available
            val altNamesArr = json.optJSONArray("altNames")
            var firstAlt: String? = null
            if (altNamesArr != null) {
                for (i in 0 until altNamesArr.length()) {
                    val candidate = clean(altNamesArr.optString(i))
                    if (candidate != null) {
                        firstAlt = candidate
                        break
                    }
                }
            }

            val name = rawName ?: englishName ?: firstAlt ?: nativeName ?: "Anime"

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
            val availableChaptersObj = json.optJSONObject("availableChapters")
            val availableEpisodesSub = when {
                availableEpisodesObj != null -> availableEpisodesObj.optInt("sub", 0)
                availableChaptersObj != null -> availableChaptersObj.optInt("sub", availableChaptersObj.optInt("raw", 0))
                json.has("chapterCount") -> json.optInt("chapterCount", 0)
                else -> 0
            }
            val availableEpisodesDub = availableEpisodesObj?.optInt("dub", 0) ?: 0

            val lastEpisodeInfoObj = json.optJSONObject("lastEpisodeInfo")
            val lastChapterInfoObj = json.optJSONObject("lastChapterInfo")
            val subInfo = lastEpisodeInfoObj?.optJSONObject("sub") ?: lastChapterInfoObj?.optJSONObject("sub") ?: lastChapterInfoObj?.optJSONObject("raw")
            val dubInfo = lastEpisodeInfoObj?.optJSONObject("dub")
            val lastEpisodeSub = subInfo?.optString("episodeString")?.takeIf { it.isNotBlank() }
                ?: subInfo?.optString("chapterString")?.takeIf { it.isNotBlank() }
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
            val rawName = clean(
                when {
                    anyCard.has("name") -> anyCard.optString("name")
                    anyCard.has("title") -> anyCard.optString("title")
                    else -> null
                }
            )

            val englishName = clean(anyCard.optString("englishName"))
            val nativeName = clean(anyCard.optString("nativeName"))

            // Extract first valid alternative name if available
            val altNamesArr = anyCard.optJSONArray("altNames")
            var firstAlt: String? = null
            if (altNamesArr != null) {
                for (i in 0 until altNamesArr.length()) {
                    val candidate = clean(altNamesArr.optString(i))
                    if (candidate != null) {
                        firstAlt = candidate
                        break
                    }
                }
            }

            val name = rawName ?: englishName ?: firstAlt ?: nativeName ?: "Anime"

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
