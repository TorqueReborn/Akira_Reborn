package com.ghostreborn.akira.features.player.services

import com.ghostreborn.akira.features.player.models.SkipInterval
import com.ghostreborn.akira.features.player.models.SkipSegmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service that queries AniSkip API (https://api.aniskip.com/v2/skip-times)
 * to retrieve accurate crowd-sourced Opening (OP) and Ending (ED) timestamps.
 */
object AniSkipService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // In-memory cache for fetched skip intervals by "malId:episode"
    private val skipCache = ConcurrentHashMap<String, List<SkipInterval>>()
    // In-memory cache for resolved MAL IDs by title
    private val titleMalIdCache = ConcurrentHashMap<String, Long>()

    /**
     * Sanitizes anime titles for search by removing video/audio quality tags,
     * release type brackets (e.g. "(Dub)", "[Sub]", "(TV)", "(Uncensored)"),
     * and extraneous whitespace.
     */
    fun sanitizeTitleForSearch(title: String): String {
        val cleaned = title
            .replace(Regex("(?i)\\b(dub|sub|uncensored|batch|tv|4k|hd|dual audio|english dub|multi-sub)\\b"), "")
            .replace(Regex("\\(.*\\)|\\[.*\\]"), "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.length >= 2) cleaned else title.trim()
    }

    /**
     * Resolves a MAL ID from an anime title using the AniList GraphQL search.
     */
    suspend fun resolveMalIdFromTitle(title: String): Long? = withContext(Dispatchers.IO) {
        val rawTitle = title.trim()
        if (rawTitle.isBlank()) return@withContext null
        titleMalIdCache[rawTitle]?.let { return@withContext it }

        val sanitized = sanitizeTitleForSearch(rawTitle)
        titleMalIdCache[sanitized]?.let { return@withContext it }

        val searchTerms = listOfNotNull(
            sanitized,
            if (sanitized != rawTitle) rawTitle.replace(Regex("\\(.*\\)|\\[.*]"), "").trim() else null
        ).distinct()

        val query = """
            query (${'$'}search: String) {
              Media(search: ${'$'}search, type: ANIME) {
                idMal
                id
              }
            }
        """.trimIndent()

        for (searchTerm in searchTerms) {
            if (searchTerm.isBlank()) continue
            try {
                val jsonPayload = JSONObject().apply {
                    put("query", query)
                    put("variables", JSONObject().apply { put("search", searchTerm) })
                }

                val request = Request.Builder()
                    .url("https://graphql.anilist.co")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .header("User-Agent", "Akira/2.0")
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val resObj = JSONObject(body)
                    val media = resObj.optJSONObject("data")?.optJSONObject("Media") ?: return@use
                    val malId = media.optLong("idMal", 0L)
                    if (malId > 0) {
                        titleMalIdCache[rawTitle] = malId
                        titleMalIdCache[sanitized] = malId
                        return@withContext malId
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Attempts to query AniSkip for opening and ending timestamps.
     * Returns a list of [SkipInterval]s or empty list if none found or on error.
     */
    suspend fun getSkipTimes(
        malId: Long,
        episodeNumber: Double,
        episodeLengthSeconds: Long = 0L
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        if (malId <= 0 || episodeNumber <= 0.0) return@withContext emptyList()

        val cacheKey = "$malId:$episodeNumber"
        skipCache[cacheKey]?.let { return@withContext it }

        val episodeParam = if (episodeNumber % 1.0 == 0.0) {
            episodeNumber.toLong().toString()
        } else {
            episodeNumber.toString()
        }

        val lengthParam = if (episodeLengthSeconds > 0) episodeLengthSeconds else 1440L

        val urlBuilder = StringBuilder("https://api.aniskip.com/v2/skip-times/$malId/$episodeParam")
            .append("?types[]=op&types[]=ed&types[]=mixed-op&types[]=mixed-ed&types[]=recap")
            .append("&episodeLength=$lengthParam")

        try {
            android.util.Log.d("AniSkipService", "Fetching skip times: $urlBuilder")
            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", "Akira/2.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.w("AniSkipService", "AniSkip HTTP ${response.code} for $urlBuilder")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)

                if (!json.optBoolean("found", false)) {
                    android.util.Log.d("AniSkipService", "AniSkip: No skip intervals found for $malId ep $episodeParam")
                    return@withContext emptyList()
                }

                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<SkipInterval>()

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val skipType = item.optString("skipType", "").lowercase()
                    val interval = item.optJSONObject("interval") ?: continue

                    val startSec = interval.optDouble("startTime", 0.0)
                    val endSec = interval.optDouble("endTime", 0.0)

                    if (endSec > startSec) {
                        val segmentType = when (skipType) {
                            "op" -> SkipSegmentType.OPENING
                            "ed" -> SkipSegmentType.ENDING
                            "recap" -> SkipSegmentType.RECAP
                            else -> SkipSegmentType.MIXED
                        }

                        val label = when (segmentType) {
                            SkipSegmentType.OPENING -> "Skip Opening"
                            SkipSegmentType.ENDING -> "Skip Outro"
                            SkipSegmentType.RECAP -> "Skip Recap"
                            SkipSegmentType.MIXED -> "Skip Intro"
                        }

                        list.add(
                            SkipInterval(
                                startTimeMs = (startSec * 1000).toLong(),
                                endTimeMs = (endSec * 1000).toLong(),
                                type = segmentType,
                                label = label
                            )
                        )
                    }
                }

                skipCache[cacheKey] = list
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearCache() {
        skipCache.clear()
    }
}
