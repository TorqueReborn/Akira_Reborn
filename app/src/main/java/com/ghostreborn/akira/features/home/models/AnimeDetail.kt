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

            fun extractName(targetObj: JSONObject?, fallbackStr: String?): String {
                if (targetObj == null) {
                    if (fallbackStr.isNullOrBlank()) return "Character"
                    if (fallbackStr.startsWith("{")) {
                        try {
                            val parsed = JSONObject(fallbackStr)
                            val f = parsed.optString("full").takeIf { it.isNotBlank() }
                                ?: parsed.optString("userPreferred").takeIf { it.isNotBlank() }
                            if (f != null) return f
                        } catch (_: Exception) {}
                    }
                    return fallbackStr
                }

                // Check if targetObj has a nested "name" object
                val nameObj = targetObj.optJSONObject("name")
                if (nameObj != null) {
                    val full = nameObj.optString("full").takeIf { it.isNotBlank() }
                    val userPreferred = nameObj.optString("userPreferred").takeIf { it.isNotBlank() }
                    val first = nameObj.optString("first").takeIf { it.isNotBlank() }
                    val last = nameObj.optString("last").takeIf { it.isNotBlank() }
                    val combined = if (first != null && last != null) "$first $last" else first ?: last
                    if (full != null) return full
                    if (userPreferred != null) return userPreferred
                    if (combined != null) return combined
                }

                // Or targetObj itself has "full" / "userPreferred"
                val directFull = targetObj.optString("full").takeIf { it.isNotBlank() }
                    ?: targetObj.optString("userPreferred").takeIf { it.isNotBlank() }
                if (directFull != null) return directFull

                val rawName = targetObj.optString("name").takeIf { it.isNotBlank() }
                if (rawName != null) {
                    if (rawName.startsWith("{")) {
                        try {
                            val parsed = JSONObject(rawName)
                            val f = parsed.optString("full").takeIf { it.isNotBlank() }
                                ?: parsed.optString("userPreferred").takeIf { it.isNotBlank() }
                            if (f != null) return f
                        } catch (_: Exception) {}
                    }
                    return rawName
                }
                return fallbackStr ?: "Character"
            }

            fun extractImage(targetObj: JSONObject?, fallbackStr: String?): String? {
                if (targetObj == null) {
                    if (fallbackStr.isNullOrBlank()) return null
                    if (fallbackStr.startsWith("{")) {
                        try {
                            val parsed = JSONObject(fallbackStr)
                            val l = parsed.optString("large").takeIf { it.isNotBlank() }
                                ?: parsed.optString("medium").takeIf { it.isNotBlank() }
                                ?: parsed.optString("default").takeIf { it.isNotBlank() }
                            if (l != null) return ImageUtils.resolveUrl(l)
                        } catch (_: Exception) {}
                    }
                    return ImageUtils.resolveUrl(fallbackStr)
                }

                // Check if targetObj has a nested "image" object
                val imgObj = targetObj.optJSONObject("image")
                if (imgObj != null) {
                    val large = imgObj.optString("large").takeIf { it.isNotBlank() }
                    val medium = imgObj.optString("medium").takeIf { it.isNotBlank() }
                    val defaultImg = imgObj.optString("default").takeIf { it.isNotBlank() }
                    val resolved = large ?: medium ?: defaultImg
                    if (resolved != null) return ImageUtils.resolveUrl(resolved)
                }

                // Or targetObj itself has "large" / "medium"
                val directLarge = targetObj.optString("large").takeIf { it.isNotBlank() }
                    ?: targetObj.optString("medium").takeIf { it.isNotBlank() }
                    ?: targetObj.optString("default").takeIf { it.isNotBlank() }
                if (directLarge != null) return ImageUtils.resolveUrl(directLarge)

                val direct = targetObj.optString("image").takeIf { it.isNotBlank() }
                if (direct != null && direct.startsWith("{")) {
                    try {
                        val parsed = JSONObject(direct)
                        val l = parsed.optString("large").takeIf { it.isNotBlank() }
                            ?: parsed.optString("medium").takeIf { it.isNotBlank() }
                            ?: parsed.optString("default").takeIf { it.isNotBlank() }
                        if (l != null) return ImageUtils.resolveUrl(l)
                    } catch (_: Exception) {}
                }
                return ImageUtils.resolveUrl(direct ?: fallbackStr)
            }

            // Extract character name and image:
            // 1) First check charObj ("character" object)
            // 2) If not present, check json directly ("name" object or string, "image" object or string)
            val name = if (charObj != null) {
                extractName(charObj, null)
            } else if (json.optJSONObject("name") != null) {
                extractName(json.optJSONObject("name"), null)
            } else {
                extractName(null, json.optString("name"))
            }

            val image = if (charObj != null) {
                extractImage(charObj, null)
            } else if (json.optJSONObject("image") != null) {
                extractImage(json.optJSONObject("image"), null)
            } else {
                extractImage(null, json.optString("image"))
            }

            val actorsArr = json.optJSONArray("actors") ?: json.optJSONArray("voiceActors")
            var actorName: String? = null
            var actorImage: String? = null
            if (actorsArr != null && actorsArr.length() > 0) {
                val firstActor = actorsArr.optJSONObject(0)
                if (firstActor != null) {
                    val actorObj = firstActor.optJSONObject("voiceActor") ?: firstActor.optJSONObject("actor") ?: firstActor
                    actorName = if (actorObj.optJSONObject("name") != null) {
                        extractName(actorObj.optJSONObject("name"), null)
                    } else {
                        extractName(actorObj, actorObj.optString("name"))
                    }
                    actorImage = if (actorObj.optJSONObject("image") != null) {
                        extractImage(actorObj.optJSONObject("image"), null)
                    } else {
                        extractImage(actorObj, actorObj.optString("image"))
                    }
                }
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
    val characters: List<AnimeCharacter> = emptyList(),
    val malId: Long? = null,
    val aniListId: Long? = null,
    val relatedShows: List<AnimeRelation> = emptyList(),
    val relatedMangas: List<AnimeRelation> = emptyList()
) {
    val displayTitle: String
        get() = listOfNotNull(englishName, name, nativeName)
            .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Anime"

    companion object {
        fun fromJson(json: JSONObject): AnimeDetail {
            fun clean(s: String?): String? = s?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

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

            // Name must never be blank or literal "null"
            val name = rawName ?: englishName ?: firstAlt ?: nativeName ?: "Anime"

            fun extractId(vararg keys: String): Long? {
                val objects = listOfNotNull(
                    json,
                    json.optJSONObject("linkedProviderIds"),
                    json.optJSONObject("externalIds"),
                    json.optJSONObject("mappings"),
                    json.optJSONObject("providerIds")
                )
                for (obj in objects) {
                    for (k in keys) {
                        if (obj.has(k) && !obj.isNull(k)) {
                            val num = obj.optLong(k, 0L)
                            if (num > 0) return num
                            val str = obj.optString(k, "").trim()
                            val parsed = str.toLongOrNull()
                            if (parsed != null && parsed > 0) return parsed
                        }
                    }
                }
                return null
            }

            val parsedMalId = extractId("malId", "mal", "myanimelist", "idMal")
            val parsedAniListId = extractId("aniListId", "anilistId", "anilist", "idAniList")

            val thumbnail = ImageUtils.resolveUrl(
                when {
                    json.has("thumbnail") -> json.optString("thumbnail")
                    json.has("cover") -> json.optString("cover")
                    json.has("poster") -> json.optString("poster")
                    json.has("image") -> json.optString("image")
                    json.has("coverImage") -> json.optString("coverImage")
                    else -> null
                }
            )

            val banner = ImageUtils.resolveUrl(
                when {
                    json.has("banner") -> json.optString("banner")
                    json.has("bannerImage") -> json.optString("bannerImage")
                    json.has("bannerUrl") -> json.optString("bannerUrl")
                    json.has("cover") -> json.optString("cover")
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

            val epDetailObj = json.optJSONObject("availableEpisodesDetail")
                ?: json.optJSONObject("availableChaptersDetail")
            val episodesDetail = AnimeEpisodeDetail.fromJson(epDetailObj)

            val charList = mutableListOf<AnimeCharacter>()
            val charArr = json.optJSONArray("characters")
            if (charArr != null) {
                for (i in 0 until charArr.length()) {
                    val c = charArr.optJSONObject(i)
                    if (c != null) charList.add(AnimeCharacter.fromJson(c))
                }
            }

            val relatedShowsList = mutableListOf<AnimeRelation>()
            val relatedShowsArr = json.optJSONArray("relatedShows")
            if (relatedShowsArr != null) {
                for (i in 0 until relatedShowsArr.length()) {
                    val rObj = relatedShowsArr.optJSONObject(i)
                    if (rObj != null) relatedShowsList.add(AnimeRelation.fromJson(rObj))
                }
            }

            val relatedMangasList = mutableListOf<AnimeRelation>()
            val relatedMangasArr = json.optJSONArray("relatedMangas")
            if (relatedMangasArr != null) {
                for (i in 0 until relatedMangasArr.length()) {
                    val rObj = relatedMangasArr.optJSONObject(i)
                    if (rObj != null) relatedMangasList.add(AnimeRelation.fromJson(rObj))
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
                characters = charList,
                malId = parsedMalId,
                aniListId = parsedAniListId,
                relatedShows = relatedShowsList,
                relatedMangas = relatedMangasList
            )
        }
    }
}

data class AnimeRelation(
    val relation: String,
    val showId: String? = null,
    val mangaId: String? = null
) {
    val targetId: String get() = showId ?: mangaId ?: ""

    companion object {
        fun fromJson(json: JSONObject): AnimeRelation {
            return AnimeRelation(
                relation = json.optString("relation", "related"),
                showId = json.optString("showId").takeIf { it.isNotBlank() },
                mangaId = json.optString("mangaId").takeIf { it.isNotBlank() }
            )
        }
    }
}

