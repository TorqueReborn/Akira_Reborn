package com.ghostreborn.akira.features.auth.services

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class WatchStatus(val value: String, val label: String) {
    WATCHING("watching", "Watching"),
    PLAN_TO_WATCH("planned", "Plan to Watch"),
    COMPLETED("completed", "Completed"),
    ON_HOLD("held", "On Hold"),
    DROPPED("dropped", "Dropped");

    companion object {
        fun fromValue(v: String?): WatchStatus? = entries.find { it.value.equals(v, ignoreCase = true) }
    }
}

data class WatchStateResult(
    val watchStateId: String,
    val showId: String,
    val userStatus: WatchStatus? = null,
    val watchingCount: Int = 0,
    val plannedCount: Int = 0,
    val completedCount: Int = 0,
    val heldCount: Int = 0,
    val droppedCount: Int = 0
)

object WatchlistSyncManager {
    private const val TAG = "WatchlistSync"

    private val httpClient
        get() = com.ghostreborn.akira.core.di.ServiceLocator.okHttpClient

    private const val WATCH_STATE_QUERY = """
query(
  ${'$'}showId: String!
  ${'$'}isManga: Boolean!
  ${'$'}userId: String
) {
  watchState(
    showId: ${'$'}showId
    isManga: ${'$'}isManga
    userId: ${'$'}userId
  ) {
    _id
    showId
    watchingCount
    watching { userId addedDate }
    plannedCount
    planned { userId addedDate }
    completedCount
    completed { userId addedDate }
    heldCount
    held { userId addedDate }
    droppedCount
    dropped { userId addedDate }
  }
}
"""

    private const val WATCHING_LIST_QUERY = """
query(
  ${'$'}userId: String!
  ${'$'}type: VaildWatchingEnumType!
  ${'$'}cardType: VaildWatchingMediaTypesEnumType!
  ${'$'}pageNo: Int!
  ${'$'}limit: Int!
  ${'$'}sortBy: SortByWatchState
  ${'$'}sortDirection: Int
  ${'$'}search: SearchInput
) {
  watchingList(
    userId: ${'$'}userId
    type: ${'$'}type
    cardType: ${'$'}cardType
    pageNo: ${'$'}pageNo
    limit: ${'$'}limit
    sortBy: ${'$'}sortBy
    sortDirection: ${'$'}sortDirection
    search: ${'$'}search
  ) {
    edges {
      anyCard {
        _id
        name
        englishName
        nativeName
        thumbnail
        type
        season { quarter year }
        score
        availableEpisodes
        availableChapters
        lastEpisodeInfo
        lastChapterInfo
      }
      isManga
    }
    pageInfo {
      total
    }
  }
}
"""

    private const val IMPORT_MY_LIST_MUTATION = """
mutation(${'$'}file: Upload!, ${'$'}listType: VaildSyncTypeEnumType!) {
  importMyList(file: ${'$'}file, listType: ${'$'}listType) {
    success
    error
    jobs {
      _id
      fileName
      listType
      sType
      status
    }
  }
}
"""

    private const val TOGGLE_WATCHING_MUTATION = """
mutation(
  ${'$'}type: VaildWatchingEnumType!
  ${'$'}watchStateId: String!
  ${'$'}userId: String!
) {
  toggleWatching(
    type: ${'$'}type
    watchStateId: ${'$'}watchStateId
    userId: ${'$'}userId
  ) {
    success
  }
}
"""

    data class WatchlistPageResult(
        val shows: List<com.ghostreborn.akira.features.home.models.AnimeShow>,
        val totalCount: Int
    )

    suspend fun fetchWatchlist(
        context: Context,
        status: WatchStatus,
        cardType: String = "ALL", // "ALL", "ANIME", "MANGA"
        page: Int = 1,
        limit: Int = 30,
        sortBy: String? = null, // "UPLOAD_TIME", "SCORE", "TITLE"
        searchQuery: String? = null
    ): WatchlistPageResult = withContext(Dispatchers.IO) {
        val userId = TokenManager.getUserId(context) ?: return@withContext WatchlistPageResult(emptyList(), 0)
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val config = MkissaConfigProvider.getConfig(context)

        val variables = JSONObject().apply {
            put("userId", userId)
            put("type", status.value)
            put("cardType", cardType)
            put("pageNo", page)
            put("limit", limit)
            if (!sortBy.isNullOrBlank()) {
                put("sortBy", sortBy)
                put("sortDirection", -1)
            }
            if (!searchQuery.isNullOrBlank()) {
                put("search", JSONObject().apply {
                    put("query", searchQuery.trim())
                    put("forMe", true)
                })
            }
        }

        try {
            val raw = MkissaGraphQlTransport.executeRawQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = WATCHING_LIST_QUERY.trim(),
                variables = variables,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Referer" to config.endpoints.appReferer,
                    "Origin" to config.endpoints.appOrigin,
                    "x-build-id" to config.buildId
                ),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext WatchlistPageResult(emptyList(), 0)
            val wl = data.optJSONObject("watchingList") ?: return@withContext WatchlistPageResult(emptyList(), 0)
            val edges = wl.optJSONArray("edges") ?: return@withContext WatchlistPageResult(emptyList(), 0)
            val total = wl.optJSONObject("pageInfo")?.optInt("total", 0) ?: edges.length()

            val result = mutableListOf<com.ghostreborn.akira.features.home.models.AnimeShow>()
            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i) ?: continue
                val anyCard = edge.optJSONObject("anyCard") ?: continue
                result.add(com.ghostreborn.akira.features.home.models.AnimeShow.fromJson(anyCard))
            }
            WatchlistPageResult(result, total)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch watchlist for ${status.value}: ${e.message}")
            WatchlistPageResult(emptyList(), 0)
        }
    }

    suspend fun fetchStatusCounts(
        context: Context,
        cardType: String = "ALL"
    ): Map<WatchStatus, Int> = withContext(Dispatchers.IO) {
        val counts = mutableMapOf<WatchStatus, Int>()
        for (status in WatchStatus.entries) {
            val res = fetchWatchlist(
                context = context,
                status = status,
                cardType = cardType,
                page = 1,
                limit = 1
            )
            counts[status] = res.totalCount
        }
        counts
    }

    suspend fun importMyList(
        context: Context,
        fileBytes: ByteArray,
        fileName: String,
        listType: String = "mal" // "mal" or "anilist"
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("You must be logged in to import a watchlist."))
        }
        val config = MkissaConfigProvider.getConfig(context)

        val variables = JSONObject().apply {
            put("listType", listType)
        }

        try {
            val raw = MkissaGraphQlTransport.executeMultipartUpload(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = IMPORT_MY_LIST_MUTATION.trim(),
                variables = variables,
                fileBytes = fileBytes,
                fileName = fileName,
                mimeType = if (fileName.endsWith(".xml", ignoreCase = true)) "application/xml" else "application/json",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Referer" to config.endpoints.appReferer,
                    "Origin" to config.endpoints.appOrigin,
                    "x-build-id" to config.buildId
                ),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data")
            val importResult = data?.optJSONObject("importMyList")
            if (importResult != null && importResult.optBoolean("success", false)) {
                Result.success("Import started successfully! Processing in background.")
            } else {
                val errorMsg = importResult?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: json.optJSONArray("errors")?.optJSONObject(0)?.optString("message")
                    ?: "Import failed on server"
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import list: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun getWatchState(
        context: Context,
        showId: String,
        isManga: Boolean = false
    ): WatchStateResult? = withContext(Dispatchers.IO) {
        val userId = TokenManager.getUserId(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val config = MkissaConfigProvider.getConfig(context)

        val variables = JSONObject().apply {
            put("showId", showId)
            put("isManga", isManga)
            if (!userId.isNullOrBlank()) {
                put("userId", userId)
            }
        }

        try {
            val raw = MkissaGraphQlTransport.executeRawQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = WATCH_STATE_QUERY.trim(),
                variables = variables,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Referer" to config.endpoints.appReferer,
                    "Origin" to config.endpoints.appOrigin,
                    "x-build-id" to config.buildId
                ),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext null
            val ws = data.optJSONObject("watchState") ?: return@withContext null
            val wsId = ws.optString("_id", "")
            if (wsId.isBlank()) return@withContext null

            fun containsUser(arrayName: String): Boolean {
                if (userId.isNullOrBlank()) return false
                val arr = ws.optJSONArray(arrayName) ?: return false
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null && obj.optString("userId") == userId) return true
                }
                return false
            }

            val currentStatus = when {
                containsUser("watching") -> WatchStatus.WATCHING
                containsUser("planned") -> WatchStatus.PLAN_TO_WATCH
                containsUser("completed") -> WatchStatus.COMPLETED
                containsUser("held") -> WatchStatus.ON_HOLD
                containsUser("dropped") -> WatchStatus.DROPPED
                else -> null
            }

            WatchStateResult(
                watchStateId = wsId,
                showId = ws.optString("showId", showId),
                userStatus = currentStatus,
                watchingCount = ws.optInt("watchingCount", 0),
                plannedCount = ws.optInt("plannedCount", 0),
                completedCount = ws.optInt("completedCount", 0),
                heldCount = ws.optInt("heldCount", 0),
                droppedCount = ws.optInt("droppedCount", 0)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch watchState for $showId: ${e.message}")
            null
        }
    }

    suspend fun setWatchStatus(
        context: Context,
        watchStateId: String,
        status: WatchStatus
    ): Boolean = withContext(Dispatchers.IO) {
        val userId = TokenManager.getUserId(context)
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "Cannot toggle watch status: user not logged in")
            return@withContext false
        }
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val config = MkissaConfigProvider.getConfig(context)

        val variables = JSONObject().apply {
            put("type", status.value)
            put("watchStateId", watchStateId)
            put("userId", userId)
        }

        try {
            val raw = MkissaGraphQlTransport.executeRawQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = TOGGLE_WATCHING_MUTATION.trim(),
                variables = variables,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Referer" to config.endpoints.appReferer,
                    "Origin" to config.endpoints.appOrigin,
                    "x-build-id" to config.buildId
                ),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext false
            val toggle = data.optJSONObject("toggleWatching") ?: return@withContext false
            toggle.optBoolean("success", false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle watching to ${status.value}: ${e.message}")
            false
        }
    }
}
