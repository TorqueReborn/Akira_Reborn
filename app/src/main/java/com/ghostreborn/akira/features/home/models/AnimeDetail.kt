package com.ghostreborn.akira.features.home.models

import com.ghostreborn.akira.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject

data class AnimeCharacter(
    val name: String,
    val role: String? = null,
    val image: String? = null,
    val actorName: String? = null,
    val actorImage: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): AnimeCharacter {
            val role = json.optString("role").takeIf { it.isNotBlank() }
            val charObj = json.optJSONObject("character")
            val name = charObj?.optString("name") ?: json.optString("name", "Character")
            val image = ImageUtils.resolveUrl(charObj?.optString("image") ?: json.optString("image"))

            val actorsArr = json.optJSONArray("actors")
            var actorName: String? = null
            var actorImage: String? = null
            if (actorsArr != null && actorsArr.length() > 0) {
                val firstActor = actorsArr.optJSONObject(0)
                actorName = firstActor?.optString("name")?.takeIf { it.isNotBlank() }
                actorImage = ImageUtils.resolveUrl(firstActor?.optString("image"))
            }

            return AnimeCharacter(
                name = name,
                role = role,
                image = image,
                actorName = actorName,
                actorImage = actorImage
            )
        }
    }
}

data class AnimeEpisodeDetail(
    val sub: List<String> = emptyList(),
    val dub: List<String> = emptyList(),
    val raw: List<String> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject?): AnimeEpisodeDetail {
            if (json == null) return AnimeEpisodeDetail()

            fun parseList(arr: JSONArray?): List<String> {
                if (arr == null) return emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i)
                    if (s.isNotBlank()) list.add(s)
                }
                return list
            }

            return AnimeEpisodeDetail(
                sub = parseList(json.optJSONArray("sub")),
                dub = parseList(json.optJSONArray("dub")),
                raw = parseList(json.optJSONArray("raw"))
            )
        }
    }
}

data class AnimeDetail(
    val id: String,
    val name: String,
    val englishName: String? = null,
    val nativeName: String? = null,
    val thumbnail: String? = null,
    val banner: String? = null,
    val description: String? = null,
    val type: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val score: Double? = null,
    val episodeCount: Int? = null,
    val seasonQuarter: String? = null,
    val seasonYear: Int? = null,
    val studios: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val availableEpisodesSub: Int = 0,
    val availableEpisodesDub: Int = 0,
    val lastEpisodeSub: String? = null,
    val lastEpisodeDub: String? = null,
    val episodesDetail: AnimeEpisodeDetail = AnimeEpisodeDetail(),
    val characters: List<AnimeCharacter> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): AnimeDetail {
            val id = json.optString("_id", "")
            val name = when {
                json.has("name") -> json.optString("name")
                json.has("title") -> json.optString("title")
                else -> "Unknown"
            }
            val englishName = json.optString("englishName").takeIf { it.isNotBlank() }
            val nativeName = json.optString("nativeName").takeIf { it.isNotBlank() }

            val thumbnail = ImageUtils.resolveUrl(
                when {
                    json.has("thumbnail") -> json.optString("thumbnail")
                    json.has("cover") -> json.optString("cover")
                    else -> null
                }
            )

            val banner = ImageUtils.resolveUrl(
                when {
                    json.has("banner") -> json.optString("banner")
                    json.has("bannerImage") -> json.optString("bannerImage")
                    json.has("bannerUrl") -> json.optString("bannerUrl")
                    else -> null
                }
            )

            var desc = json.optString("description", "").trim()
            desc = desc.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&#39;", "'")
            val description = desc.takeIf { it.isNotBlank() }

            val type = json.optString("type").takeIf { it.isNotBlank() }
            val status = json.optString("status").takeIf { it.isNotBlank() }
            val rating = json.optString("rating").takeIf { it.isNotBlank() }
            val score = if (json.has("score")) json.optDouble("score").takeIf { !it.isNaN() && it > 0 } else null
            val episodeCount = json.optInt("episodeCount").takeIf { it > 0 }

            val seasonObj = json.optJSONObject("season")
            val seasonQuarter = seasonObj?.optString("quarter")?.takeIf { it.isNotBlank() }
            val seasonYear = seasonObj?.optInt("year")?.takeIf { it > 0 }

            val studiosList = mutableListOf<String>()
            val studiosArr = json.optJSONArray("studios")
            if (studiosArr != null) {
                for (i in 0 until studiosArr.length()) {
                    val s = studiosArr.optString(i).trim()
                    if (s.isNotBlank()) studiosList.add(s)
                }
            }

            val genresList = mutableListOf<String>()
            val genresArr = json.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    val g = genresArr.optString(i).trim()
                    if (g.isNotBlank()) genresList.add(g)
                }
            }

            val tagsList = mutableListOf<String>()
            val tagsArr = json.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    var t = tagsArr.optString(i).trim()
                    if (t.startsWith("theme:")) t = t.substring(6)
                    if (t.isNotBlank()) tagsList.add(t)
                }
            }

            val availableEpisodesObj = json.optJSONObject("availableEpisodes")
            val availableEpisodesSub = availableEpisodesObj?.optInt("sub", 0) ?: 0
            val availableEpisodesDub = availableEpisodesObj?.optInt("dub", 0) ?: 0

            val lastEpisodeInfoObj = json.optJSONObject("lastEpisodeInfo")
            val subInfo = lastEpisodeInfoObj?.optJSONObject("sub")
            val dubInfo = lastEpisodeInfoObj?.optJSONObject("dub")
            val lastEpisodeSub = subInfo?.optString("episodeString")?.takeIf { it.isNotBlank() }
            val lastEpisodeDub = dubInfo?.optString("episodeString")?.takeIf { it.isNotBlank() }

            val epDetailObj = json.optJSONObject("availableEpisodesDetail")
            val episodesDetail = AnimeEpisodeDetail.fromJson(epDetailObj)

            val charList = mutableListOf<AnimeCharacter>()
            val charArr = json.optJSONArray("characters")
            if (charArr != null) {
                for (i in 0 until charArr.length()) {
                    val c = charArr.optJSONObject(i)
                    if (c != null) charList.add(AnimeCharacter.fromJson(c))
                }
            }

            return AnimeDetail(
                id = id,
                name = name,
                englishName = englishName,
                nativeName = nativeName,
                thumbnail = thumbnail,
                banner = banner,
                description = description,
                type = type,
                status = status,
                rating = rating,
                score = score,
                episodeCount = episodeCount,
                seasonQuarter = seasonQuarter,
                seasonYear = seasonYear,
                studios = studiosList,
                genres = genresList,
                tags = tagsList,
                availableEpisodesSub = availableEpisodesSub,
                availableEpisodesDub = availableEpisodesDub,
                lastEpisodeSub = lastEpisodeSub,
                lastEpisodeDub = lastEpisodeDub,
                episodesDetail = episodesDetail,
                characters = charList
            )
        }
    }
}
