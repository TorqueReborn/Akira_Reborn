package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.config.OperationRegistry
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.models.AnimePageResult
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.CryptoBootstrap
import com.ghostreborn.akira.features.player.services.Decryptor
import com.ghostreborn.akira.features.player.services.StreamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class EpisodeFetchResult(
    val streams: List<StreamSource>,
    val rawResponse: String,
    val tobeparsed: String,
    val decryptedJson: String,
    val bootstrapEpoch: Long
)

object AnimeRepository {
    private const val TAG = "AnimeRepository"

    val BASE_URL: String
        get() = MkissaConfigProvider.currentEndpoints.apiBaseUrl

    // Preferences
    var allowAdult: Boolean = false
    var denyEcchi: Boolean = false
    var allowUnknown: Boolean = false

    private const val PREFS_NAME = "akira_content_prefs"
    private const val PREF_ALLOW_ADULT = "pref_allow_adult"
    private const val PREF_DENY_ECCHI = "pref_deny_ecchi"
    private const val PREF_ALLOW_UNKNOWN = "pref_allow_unknown"

    fun loadContentPreferences(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        allowAdult = prefs.getBoolean(PREF_ALLOW_ADULT, false)
        denyEcchi = prefs.getBoolean(PREF_DENY_ECCHI, false)
        allowUnknown = prefs.getBoolean(PREF_ALLOW_UNKNOWN, false)
    }

    fun updateContentPreferences(
        context: Context,
        newAllowAdult: Boolean? = null,
        newDenyEcchi: Boolean? = null,
        newAllowUnknown: Boolean? = null
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        newAllowAdult?.let {
            allowAdult = it
            editor.putBoolean(PREF_ALLOW_ADULT, it)
        }
        newDenyEcchi?.let {
            denyEcchi = it
            editor.putBoolean(PREF_DENY_ECCHI, it)
        }
        newAllowUnknown?.let {
            allowUnknown = it
            editor.putBoolean(PREF_ALLOW_UNKNOWN, it)
        }
        editor.apply()
        clearCache()
    }

    // Dynamic persisted query hash getters with fallback
    val BROWSE_PERSISTED_QUERY_HASH: String
        get() = MkissaConfigProvider.currentConfig.getOperationHash(OperationRegistry.OP_BROWSE)

    val TOP_RANKED_PERSISTED_QUERY_HASH: String
        get() = MkissaConfigProvider.currentConfig.getOperationHash(OperationRegistry.OP_TOP_RANKED)

    val DETAIL_PERSISTED_QUERY_HASH: String
        get() = MkissaConfigProvider.currentConfig.getOperationHash(OperationRegistry.OP_DETAIL)

    val EPISODE_PERSISTED_QUERY_HASH: String
        get() = MkissaConfigProvider.currentConfig.getOperationHash(OperationRegistry.OP_EPISODE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val pageCache = ConcurrentHashMap<String, AnimePageResult>()
    private val detailCache = ConcurrentHashMap<String, AnimeDetail>()
    @Volatile
    private var topRankedCache: List<AnimeShow>? = null

    fun clearCache() {
        pageCache.clear()
        detailCache.clear()
        topRankedCache = null
    }

    private fun getHeaders(buildId: String = MkissaConfigProvider.currentBuildId): Map<String, String> {
        val endpoints = MkissaConfigProvider.currentEndpoints
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Referer" to endpoints.appReferer,
            "Origin" to endpoints.appOrigin,
            "x-build-id" to buildId,
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site"
        )
    }

    suspend fun fetchAnimeList(
        page: Int = 1,
        limit: Int = 26,
        translationType: String = "sub",
        listProfile: String = "browse",
        searchQuery: String? = null,
        sortBy: String? = null,
        countryOrigin: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        context: Context? = null
    ): AnimePageResult = withContext(Dispatchers.IO) {
        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val buildId = config.buildId
        val queryHash = config.getOperationHash(OperationRegistry.OP_BROWSE)

        val searchObj = JSONObject().apply {
            put("listProfile", listProfile)
            put("allowAdult", allowAdult)
            put("allowUnknown", allowUnknown)
            put("denyEcchi", denyEcchi)
        }

        val q = searchQuery?.trim()
        if (!q.isNullOrEmpty()) {
            searchObj.put("query", q)
            searchObj.put("sortBy", sortBy ?: "Top")
        } else {
            if (!sortBy.isNullOrEmpty()) searchObj.put("sortBy", sortBy)
            if (!season.isNullOrEmpty()) searchObj.put("season", season)
            if (seasonYear != null && seasonYear > 0) searchObj.put("year", seasonYear)
        }

        val variablesObj = JSONObject().apply {
            put("search", searchObj)
            put("limit", limit)
            put("page", page)
            put("translationType", translationType)
            if (!countryOrigin.isNullOrEmpty() && countryOrigin != "ALL") {
                put("countryOrigin", countryOrigin)
            }
        }

        val extensionsObj = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        val encodedVars = Uri.encode(variablesObj.toString())
        val encodedExts = Uri.encode(extensionsObj.toString())
        val fullUrl = "$BASE_URL?variables=$encodedVars&extensions=$encodedExts"

        if (page == 1 && q.isNullOrEmpty()) {
            pageCache[fullUrl]?.let { return@withContext it }
        }

        val reqBuilder = Request.Builder().url(fullUrl)
        getHeaders(buildId).forEach { (k, v) -> reqBuilder.header(k, v) }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IllegalStateException("Server error: HTTP ${resp.code}")

            val result = parseAnimeResponse(body, page)
            if (page == 1 && q.isNullOrEmpty()) {
                pageCache[fullUrl] = result
            }
            result
        }
    }

    suspend fun fetchTopRankedAnime(
        dateRange: Int = 1,
        size: Int = 15,
        context: Context? = null
    ): List<AnimeShow> = withContext(Dispatchers.IO) {
        topRankedCache?.takeIf { it.isNotEmpty() }?.let { return@withContext it }

        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val buildId = config.buildId
        val queryHash = config.getOperationHash(OperationRegistry.OP_TOP_RANKED)

        val variablesObj = JSONObject().apply {
            put("type", "anime")
            put("size", size)
            put("dateRange", dateRange)
            put("page", 1)
            put("allowAdult", allowAdult)
            put("allowUnknown", allowUnknown)
        }

        val extensionsObj = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        val encodedVars = Uri.encode(variablesObj.toString())
        val encodedExts = Uri.encode(extensionsObj.toString())
        val fullUrl = "$BASE_URL?variables=$encodedVars&extensions=$encodedExts"

        val reqBuilder = Request.Builder().url(fullUrl)
        getHeaders(buildId).forEach { (k, v) -> reqBuilder.header(k, v) }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IllegalStateException("Server error: HTTP ${resp.code}")

            val json = JSONObject(body)
            val data = json.optJSONObject("data")
            val top10 = data?.optJSONObject("top10")
            val edges = top10?.optJSONArray("edges")

            val list = mutableListOf<AnimeShow>()
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val item = edges.optJSONObject(i)
                    if (item != null) {
                        list.add(AnimeShow.fromRankedCard(item))
                    }
                }
            }
            topRankedCache = list
            list
        }
    }

    suspend fun fetchAnimeDetail(
        animeId: String,
        context: Context? = null
    ): AnimeDetail = withContext(Dispatchers.IO) {
        detailCache[animeId]?.let { return@withContext it }

        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val buildId = config.buildId
        val queryHash = config.getOperationHash(OperationRegistry.OP_DETAIL)

        val variablesObj = JSONObject().apply {
            put("_id", animeId)
            put("search", JSONObject().apply {
                put("allowAdult", true)
                put("allowUnknown", true)
                put("denyEcchi", false)
                put("lite", false)
                put("forMe", false)
            })
        }

        val extensionsObj = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        val encodedVars = Uri.encode(variablesObj.toString())
        val encodedExts = Uri.encode(extensionsObj.toString())
        val fullUrl = "$BASE_URL?variables=$encodedVars&extensions=$encodedExts"

        val reqBuilder = Request.Builder().url(fullUrl)
        getHeaders(buildId).forEach { (k, v) -> reqBuilder.header(k, v) }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IllegalStateException("Server error: HTTP ${resp.code}")

            val json = JSONObject(body)
            if (json.has("errors")) {
                val errors = json.optJSONArray("errors")
                val first = errors?.optJSONObject(0)
                val msg = first?.optString("message") ?: "Failed to load anime details"
                throw IllegalStateException(msg)
            }

            val data = json.optJSONObject("data")
            val showMap = data?.optJSONObject("show") ?: data?.optJSONObject("anime")
                ?: throw IllegalStateException("Anime details not found.")

            val detail = AnimeDetail.fromJson(showMap)
            detailCache[animeId] = detail
            detail
        }
    }

    private fun parseAnimeResponse(rawResponse: String, page: Int): AnimePageResult {
        val json = JSONObject(rawResponse)
        if (json.has("errors")) {
            val errors = json.optJSONArray("errors")
            val first = errors?.optJSONObject(0)
            throw IllegalStateException(first?.optString("message") ?: "API query error")
        }

        val data = json.optJSONObject("data") ?: throw IllegalStateException("Response missing 'data' object.")
        val shows = data.optJSONObject("shows") ?: throw IllegalStateException("Response missing 'shows' object.")

        val pageInfo = shows.optJSONObject("pageInfo")
        val total = pageInfo?.optInt("total", 0) ?: 0

        val edges = shows.optJSONArray("edges")
        val list = mutableListOf<AnimeShow>()
        if (edges != null) {
            for (i in 0 until edges.length()) {
                val item = edges.optJSONObject(i)
                if (item != null) list.add(AnimeShow.fromJson(item))
            }
        }

        return AnimePageResult(shows = list, total = total, page = page)
    }

    suspend fun fetchEpisodeStreams(
        context: Context,
        showId: String,
        episodeString: String,
        translationType: String = "sub",
        authToken: String? = null
    ): EpisodeFetchResult = withContext(Dispatchers.IO) {
        require(showId.isNotBlank()) { "Show ID cannot be empty." }
        require(episodeString.isNotBlank()) { "Episode string cannot be empty." }

        // Initialize/resolve configuration first
        var config = MkissaConfigProvider.getConfig(context)
        val queryHash = config.getOperationHash(OperationRegistry.OP_EPISODE)
        val effectiveLane = OperationRegistry.resolveLaneForQuery(OperationRegistry.OP_EPISODE)
            ?: throw IllegalStateException("Unsupported or unknown content lane for episode operation")

        fun executeEpisodeQuery(
            bootstrap: CryptoBootstrap,
            cfg: com.ghostreborn.akira.core.config.MkissaClientConfig,
            explicitAaReq: String? = null
        ): String {
            val effectiveAaReq = explicitAaReq ?: Decryptor.generateAaReq(
                queryHash = queryHash,
                bootstrap = bootstrap,
                buildId = cfg.buildId,
                config = cfg
            )

            val variablesObj = JSONObject().apply {
                put("showId", showId)
                put("translationType", translationType.lowercase())
                put("episodeString", episodeString)
            }

            val extensionsObj = JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", queryHash)
                })
                put("k", effectiveLane)
                put("aaReq", effectiveAaReq)
            }

            val fullUrl = "$BASE_URL?variables=${Uri.encode(variablesObj.toString())}&extensions=${Uri.encode(extensionsObj.toString())}"
            val reqBuilder = Request.Builder().url(fullUrl)
            getHeaders(cfg.buildId).forEach { (k, v) -> reqBuilder.header(k, v) }

            if (!authToken.isNullOrBlank()) {
                val authHeader = if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken"
                reqBuilder.header("authorization", authHeader)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("API error HTTP ${response.code}: $body")
                }
                return body
            }
        }

        // Step 1: Obtain bootstrap for lane and resolved buildId
        var bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = authToken, config = config)
        var responseStr = executeEpisodeQuery(bootstrap, config)

        fun checkBuildIdError(resp: String): Boolean {
            val u = resp.uppercase()
            return u.contains("UNKNOWN BUILD ID") || u.contains("UNKNOWN_BUILD_ID") ||
                    u.contains("AA_CRYPTO_MISSING_BUILD") || u.contains("AA_CRYPTO_BUILD_MISMATCH")
        }

        fun checkCryptoError(resp: String): Boolean {
            val u = resp.uppercase()
            return u.contains("AA_CRYPTO_STALE") || u.contains("AA_CRYPTO_EXPIRED") ||
                    u.contains("AA_CRYPTO_MISSING") || u.contains("AA_CRYPTO_MISSING_BUILD") ||
                    u.contains("AA_CRYPTO_MISSING_LANE") || u.contains("AA_CRYPTO_LANE_MISMATCH") ||
                    u.contains("AA_CRYPTO_QUERY_MISMATCH") || u.contains("AA_CRYPTO_BUILD_MISMATCH") ||
                    u.contains("AACRYPTO EXPIRED") || u.contains("AACRYPTO_EXPIRED")
        }

        // Recovery: Unknown build ID (max 1 retry)
        if (checkBuildIdError(responseStr)) {
            Log.d(TAG, "Unknown build id encountered. Invalidation retry...")
            MkissaConfigProvider.invalidate(context)
            Decryptor.invalidateBootstrapCacheForLane(effectiveLane)

            try {
                config = MkissaConfigProvider.getConfig(context, forceRefresh = true)
                bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = authToken, forceRefresh = true, config = config)
                val freshAaReq = Decryptor.generateAaReq(
                    queryHash = config.getOperationHash(OperationRegistry.OP_EPISODE),
                    bootstrap = bootstrap,
                    buildId = config.buildId,
                    config = config
                )
                responseStr = executeEpisodeQuery(bootstrap, config, freshAaReq)
            } catch (e: Exception) {
                Log.e(TAG, "Build ID recovery retry failed: $e")
            }

            if (checkBuildIdError(responseStr)) {
                throw IllegalStateException("Mkissa API rejected build ID: $responseStr")
            }
        }

        // Recovery: Crypto expired / stale (max 1 retry)
        if (checkCryptoError(responseStr)) {
            Log.d(TAG, "AA_CRYPTO error. Retrying with fresh bootstrap...")
            Decryptor.invalidateBootstrapCacheForLane(effectiveLane)

            try {
                bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = authToken, forceRefresh = true, config = config)
                val freshAaReq = Decryptor.generateAaReq(
                    queryHash = config.getOperationHash(OperationRegistry.OP_EPISODE),
                    bootstrap = bootstrap,
                    buildId = config.buildId,
                    config = config
                )
                responseStr = executeEpisodeQuery(bootstrap, config, freshAaReq)
            } catch (e: Exception) {
                Log.e(TAG, "Crypto retry failed: $e")
            }
        }

        // Parse and Decrypt
        val root = JSONObject(responseStr)
        if (root.has("errors")) {
            val errors = root.optJSONArray("errors")
            val first = errors?.optJSONObject(0)
            throw IllegalStateException(first?.optString("message") ?: "Episode query error")
        }

        val dataObj = root.optJSONObject("data")
        val epObj = dataObj?.optJSONObject("episode")

        val rawTobe = dataObj?.optString("tobeparsed")
        val epTobe = epObj?.optString("tobeparsed")
        val epEps = epObj?.optString("episodes")

        val tobeparsed = when {
            !rawTobe.isNullOrBlank() -> rawTobe
            !epTobe.isNullOrBlank() -> epTobe
            !epEps.isNullOrBlank() -> epEps
            else -> throw IllegalStateException("No stream data found in episode response.")
        }

        val plainText = Decryptor.decrypt(
            payload = tobeparsed,
            partBBase64 = bootstrap.partB,
            buildId = config.buildId,
            config = config
        )

        val streams = StreamParser.parseStreams(plainText)
        if (streams.isEmpty()) {
            throw IllegalStateException("No playable video stream sources could be extracted.")
        }

        EpisodeFetchResult(
            streams = streams,
            rawResponse = responseStr,
            tobeparsed = tobeparsed,
            decryptedJson = plainText,
            bootstrapEpoch = bootstrap.epoch
        )
    }
}
