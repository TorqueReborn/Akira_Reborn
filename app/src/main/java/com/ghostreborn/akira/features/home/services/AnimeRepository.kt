package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaClientConfig
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.config.OperationRegistry
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
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

interface AnimeRepository {
    var allowAdult: Boolean
    var denyEcchi: Boolean
    var allowUnknown: Boolean

    fun loadContentPreferences(context: Context)
    fun updateContentPreferences(
        context: Context,
        newAllowAdult: Boolean? = null,
        newDenyEcchi: Boolean? = null,
        newAllowUnknown: Boolean? = null
    )
    fun clearCache()

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
    ): AnimePageResult

    suspend fun fetchTopRankedAnime(
        dateRange: Int = 1,
        size: Int = 15,
        context: Context? = null
    ): List<AnimeShow>

    suspend fun fetchAnimeDetail(
        animeId: String,
        context: Context? = null
    ): AnimeDetail

    suspend fun fetchEpisodeStreams(
        context: Context,
        showId: String,
        episodeString: String,
        translationType: String = "sub",
        authToken: String? = null
    ): EpisodeFetchResult

    companion object : AnimeRepository {
        private val repo: AnimeRepository get() = ServiceLocator.animeRepository

        override var allowAdult: Boolean
            get() = repo.allowAdult
            set(value) { repo.allowAdult = value }

        override var denyEcchi: Boolean
            get() = repo.denyEcchi
            set(value) { repo.denyEcchi = value }

        override var allowUnknown: Boolean
            get() = repo.allowUnknown
            set(value) { repo.allowUnknown = value }

        override fun loadContentPreferences(context: Context) = repo.loadContentPreferences(context)
        override fun updateContentPreferences(context: Context, newAllowAdult: Boolean?, newDenyEcchi: Boolean?, newAllowUnknown: Boolean?) = repo.updateContentPreferences(context, newAllowAdult, newDenyEcchi, newAllowUnknown)
        override fun clearCache() = repo.clearCache()

        override suspend fun fetchAnimeList(page: Int, limit: Int, translationType: String, listProfile: String, searchQuery: String?, sortBy: String?, countryOrigin: String?, season: String?, seasonYear: Int?, context: Context?) =
            repo.fetchAnimeList(page, limit, translationType, listProfile, searchQuery, sortBy, countryOrigin, season, seasonYear, context)

        override suspend fun fetchTopRankedAnime(dateRange: Int, size: Int, context: Context?) =
            repo.fetchTopRankedAnime(dateRange, size, context)

        override suspend fun fetchAnimeDetail(animeId: String, context: Context?) =
            repo.fetchAnimeDetail(animeId, context)

        override suspend fun fetchEpisodeStreams(context: Context, showId: String, episodeString: String, translationType: String, authToken: String?) =
            repo.fetchEpisodeStreams(context, showId, episodeString, translationType, authToken)
    }
}

class DefaultAnimeRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : AnimeRepository {

    private object InternalConstants {
        const val TAG = "DefaultAnimeRepository"
        const val PREFS_NAME = "akira_content_prefs"
        const val PREF_ALLOW_ADULT = "pref_allow_adult"
        const val PREF_DENY_ECCHI = "pref_deny_ecchi"
        const val PREF_ALLOW_UNKNOWN = "pref_allow_unknown"
    }

    @Volatile
    private var preferencesLoaded: Boolean = false

    override var allowAdult: Boolean = false
    override var denyEcchi: Boolean = false
    override var allowUnknown: Boolean = false

    private val pageCache = ConcurrentHashMap<String, AnimePageResult>()
    private val detailCache = ConcurrentHashMap<String, AnimeDetail>()
    @Volatile
    private var topRankedCache: List<AnimeShow>? = null

    private val baseUrl: String
        get() = MkissaConfigProvider.currentEndpoints.apiBaseUrl

    override fun loadContentPreferences(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(InternalConstants.PREFS_NAME, Context.MODE_PRIVATE)
        allowAdult = prefs.getBoolean(InternalConstants.PREF_ALLOW_ADULT, false)
        denyEcchi = prefs.getBoolean(InternalConstants.PREF_DENY_ECCHI, false)
        allowUnknown = prefs.getBoolean(InternalConstants.PREF_ALLOW_UNKNOWN, false)
        preferencesLoaded = true
    }

    override fun updateContentPreferences(
        context: Context,
        newAllowAdult: Boolean?,
        newDenyEcchi: Boolean?,
        newAllowUnknown: Boolean?
    ) {
        val prefs = context.applicationContext.getSharedPreferences(InternalConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        newAllowAdult?.let {
            allowAdult = it
            editor.putBoolean(InternalConstants.PREF_ALLOW_ADULT, it)
        }
        newDenyEcchi?.let {
            denyEcchi = it
            editor.putBoolean(InternalConstants.PREF_DENY_ECCHI, it)
        }
        newAllowUnknown?.let {
            allowUnknown = it
            editor.putBoolean(InternalConstants.PREF_ALLOW_UNKNOWN, it)
        }
        editor.apply()
        clearCache()
    }

    override fun clearCache() {
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

    override suspend fun fetchAnimeList(
        page: Int,
        limit: Int,
        translationType: String,
        listProfile: String,
        searchQuery: String?,
        sortBy: String?,
        countryOrigin: String?,
        season: String?,
        seasonYear: Int?,
        context: Context?
    ): AnimePageResult = withContext(Dispatchers.IO) {
        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val buildId = config.buildId
        val queryHash = config.getOperationHash(OperationRegistry.OP_BROWSE)

        if (!preferencesLoaded && context != null) {
            loadContentPreferences(context)
        }

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

        val cacheKey = "$baseUrl?variables=${Uri.encode(variablesObj.toString())}&extensions=${Uri.encode(extensionsObj.toString())}"
        if (q.isNullOrEmpty()) {
            pageCache[cacheKey]?.let { return@withContext it }
        }

        val body = MkissaGraphQlTransport.executePersistedQuery(
            client = httpClient,
            apiBaseUrl = config.endpoints.apiBaseUrl,
            variables = variablesObj,
            extensions = extensionsObj,
            headers = getHeaders(buildId),
            authToken = AuthRepository.getValidAccessTokenOrNull(context)
        )
        val result = parseAnimeResponse(body, page)
        if (q.isNullOrEmpty()) pageCache[cacheKey] = result
        result
    }

    override suspend fun fetchTopRankedAnime(
        dateRange: Int,
        size: Int,
        context: Context?
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

        val body = MkissaGraphQlTransport.executePersistedQuery(
            client = httpClient,
            apiBaseUrl = config.endpoints.apiBaseUrl,
            variables = variablesObj,
            extensions = extensionsObj,
            headers = getHeaders(buildId),
            authToken = AuthRepository.getValidAccessTokenOrNull(context)
        )
        val json = JSONObject(body)
        throwIfGraphQlErrors(json, "Failed to load top-ranked anime")
        val data = json.optJSONObject("data")
        val top10 = data?.optJSONObject("top10")
        val edges = top10?.optJSONArray("edges")

        val list = mutableListOf<AnimeShow>()
        if (edges != null) {
            for (i in 0 until edges.length()) {
                val item = edges.optJSONObject(i)
                if (item != null) list.add(AnimeShow.fromRankedCard(item))
            }
        }
        topRankedCache = list
        list
    }

    override suspend fun fetchAnimeDetail(
        animeId: String,
        context: Context?
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

        val body = MkissaGraphQlTransport.executePersistedQuery(
            client = httpClient,
            apiBaseUrl = config.endpoints.apiBaseUrl,
            variables = variablesObj,
            extensions = extensionsObj,
            headers = getHeaders(buildId),
            authToken = AuthRepository.getValidAccessTokenOrNull(context)
        )
        val json = JSONObject(body)
        throwIfGraphQlErrors(json, "Failed to load anime details")
        val data = json.optJSONObject("data")
        val showMap = data?.optJSONObject("show") ?: data?.optJSONObject("anime")
            ?: throw IllegalStateException("Anime details not found.")

        val detail = AnimeDetail.fromJson(showMap)
        detailCache[animeId] = detail
        detail
    }

    private fun throwIfGraphQlErrors(json: JSONObject, fallbackMessage: String) {
        val errors = json.optJSONArray("errors") ?: return
        if (errors.length() == 0) return
        val first = errors.optJSONObject(0)
        val message = first?.optString("message")?.takeIf { it.isNotBlank() } ?: fallbackMessage
        throw IllegalStateException(message)
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

    override suspend fun fetchEpisodeStreams(
        context: Context,
        showId: String,
        episodeString: String,
        translationType: String,
        authToken: String?
    ): EpisodeFetchResult = withContext(Dispatchers.IO) {
        require(showId.isNotBlank()) { "Show ID cannot be empty." }
        require(episodeString.isNotBlank()) { "Episode string cannot be empty." }

        var config = MkissaConfigProvider.getConfig(context)
        val effectiveLane = OperationRegistry.resolveLaneForQuery(OperationRegistry.OP_EPISODE)
            ?: throw IllegalStateException("Unsupported or unknown content lane for episode operation")

        val requestAuthToken = try {
            AuthRepository.getValidAccessTokenOrNull(context)
        } catch (e: Exception) {
            Log.w(InternalConstants.TAG, "Could not refresh the saved session: ${e.message}")
            null
        } ?: if (!TokenManager.isAccessTokenExpired(context)) authToken else null

        suspend fun executeEpisodeQuery(
            bootstrap: CryptoBootstrap,
            cfg: MkissaClientConfig,
            explicitAaReq: String? = null
        ): String {
            val activeQueryHash = cfg.getOperationHash(OperationRegistry.OP_EPISODE)
            val effectiveAaReq = explicitAaReq ?: Decryptor.generateAaReq(
                queryHash = activeQueryHash,
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
                    put("sha256Hash", activeQueryHash)
                })
                put("k", effectiveLane)
                put("aaReq", effectiveAaReq)
            }

            return MkissaGraphQlTransport.executePersistedQuery(
                client = httpClient,
                apiBaseUrl = cfg.endpoints.apiBaseUrl,
                variables = variablesObj,
                extensions = extensionsObj,
                headers = getHeaders(cfg.buildId),
                authToken = requestAuthToken
            )
        }

        var bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = requestAuthToken, config = config)
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
                    u.contains("AACRYPTO EXPIRED") || u.contains("AACRYPTO_EXPIRED") ||
                    u.contains("INVALID_BOOT_TOKEN")
        }

        if (checkBuildIdError(responseStr) || responseStr.contains("invalid_boot_token")) {
            Log.d(InternalConstants.TAG, "Unknown build id or invalid boot token encountered. Invalidation retry...")
            MkissaConfigProvider.invalidate(context)
            Decryptor.invalidateBootstrapCacheForLane(effectiveLane)

            try {
                config = MkissaConfigProvider.getConfig(context, forceRefresh = true)
                bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = requestAuthToken, forceRefresh = true, config = config)
                val freshAaReq = Decryptor.generateAaReq(
                    queryHash = config.getOperationHash(OperationRegistry.OP_EPISODE),
                    bootstrap = bootstrap,
                    buildId = config.buildId,
                    config = config
                )
                responseStr = executeEpisodeQuery(bootstrap, config, freshAaReq)
            } catch (e: Exception) {
                Log.e(InternalConstants.TAG, "Build ID / boot recovery retry failed: $e")
            }

            if (checkBuildIdError(responseStr)) {
                throw IllegalStateException("Mkissa API rejected build ID: $responseStr")
            }
        }

        if (checkCryptoError(responseStr)) {
            Log.d(InternalConstants.TAG, "AA_CRYPTO error. Retrying with fresh bootstrap...")
            Decryptor.invalidateBootstrapCacheForLane(effectiveLane)

            try {
                bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = requestAuthToken, forceRefresh = true, config = config)
                val freshAaReq = Decryptor.generateAaReq(
                    queryHash = config.getOperationHash(OperationRegistry.OP_EPISODE),
                    bootstrap = bootstrap,
                    buildId = config.buildId,
                    config = config
                )
                responseStr = executeEpisodeQuery(bootstrap, config, freshAaReq)
            } catch (e: Exception) {
                Log.e(InternalConstants.TAG, "Crypto retry failed: $e")
            }
        }

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
