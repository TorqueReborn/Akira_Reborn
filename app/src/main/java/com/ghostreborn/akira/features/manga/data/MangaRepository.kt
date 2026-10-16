package com.ghostreborn.akira.features.manga.data

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaClientConfig
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.config.OperationRegistry
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.models.AnimePageResult
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.manga.models.MangaPage
import com.ghostreborn.akira.features.player.services.CryptoBootstrap
import com.ghostreborn.akira.features.player.services.Decryptor
import com.ghostreborn.akira.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface MangaRepository {
    suspend fun fetchMangaList(
        page: Int = 1,
        limit: Int = 26,
        searchQuery: String? = null,
        sortBy: String? = null,
        countryOrigin: String? = null,
        context: Context? = null
    ): AnimePageResult

    suspend fun fetchMangaDetail(
        mangaId: String,
        context: Context? = null
    ): AnimeDetail

    suspend fun fetchChapterPages(
        context: Context,
        mangaId: String,
        chapterString: String,
        translationType: String = "sub"
    ): List<MangaPage>
}

class DefaultMangaRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val animeRepository: AnimeRepository = AnimeRepository
) : MangaRepository {

    private val chapterPagesCache = ConcurrentHashMap<String, List<MangaPage>>()

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

    override suspend fun fetchMangaList(
        page: Int,
        limit: Int,
        searchQuery: String?,
        sortBy: String?,
        countryOrigin: String?,
        context: Context?
    ): AnimePageResult = withContext(Dispatchers.IO) {
        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val buildId = config.buildId
        val queryHash = config.getOperationHash(OperationRegistry.OP_MANGA_BROWSE)

        if (context != null) {
            animeRepository.loadContentPreferences(context)
        }

        val searchObj = JSONObject().apply {
            put("allowAdult", animeRepository.allowAdult)
            put("allowUnknown", animeRepository.allowUnknown)
            put("denyEcchi", animeRepository.denyEcchi)
        }

        val q = searchQuery?.trim()
        if (!q.isNullOrEmpty()) {
            searchObj.put("query", q)
            searchObj.put("sortBy", sortBy ?: "Top")
        } else {
            if (!sortBy.isNullOrEmpty()) searchObj.put("sortBy", sortBy)
        }

        val variablesObj = JSONObject().apply {
            put("search", searchObj)
            put("limit", limit)
            put("page", page)
            put("translationType", "sub")
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

        val body = MkissaGraphQlTransport.executePersistedQuery(
            client = httpClient,
            apiBaseUrl = config.endpoints.apiBaseUrl,
            variables = variablesObj,
            extensions = extensionsObj,
            headers = getHeaders(buildId),
            authToken = AuthRepository.getValidAccessTokenOrNull(context)
        )

        parseMangaResponse(body, page)
    }

    private fun parseMangaResponse(rawResponse: String, page: Int): AnimePageResult {
        val json = JSONObject(rawResponse)
        if (json.has("errors")) {
            val errors = json.optJSONArray("errors")
            val first = errors?.optJSONObject(0)
            throw IllegalStateException(first?.optString("message") ?: "API query error")
        }

        val data = json.optJSONObject("data") ?: throw IllegalStateException("Response missing 'data' object.")
        val mangas = data.optJSONObject("mangas") ?: throw IllegalStateException("Response missing 'mangas' object.")

        val pageInfo = mangas.optJSONObject("pageInfo")
        val total = pageInfo?.optInt("total", 0) ?: 0

        val edges = mangas.optJSONArray("edges")
        val list = mutableListOf<com.ghostreborn.akira.features.home.models.AnimeShow>()
        if (edges != null) {
            for (i in 0 until edges.length()) {
                val item = edges.optJSONObject(i)
                if (item != null) {
                    val show = com.ghostreborn.akira.features.home.models.AnimeShow.fromJson(item)
                    list.add(show.copy(type = show.type ?: "manga"))
                }
            }
        }

        return AnimePageResult(shows = list, total = total, page = page)
    }

    override suspend fun fetchMangaDetail(
        mangaId: String,
        context: Context?
    ): AnimeDetail = withContext(Dispatchers.IO) {
        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val buildId = config.buildId

        val query = """
            query(${'$'}_id: String!, ${'$'}search: SearchInput) {
              manga(_id: ${'$'}_id, search: ${'$'}search) {
                _id
                name
                englishName
                nativeName
                thumbnail
                description
                genres
                status
                type
                season
                score
                availableChaptersDetail
                availableChapters
                lastChapterInfo
              }
            }
        """.trimIndent()

        val variablesObj = JSONObject().apply {
            put("_id", mangaId)
            put("search", JSONObject().apply {
                put("allowAdult", animeRepository.allowAdult)
                put("allowUnknown", animeRepository.allowUnknown)
                put("denyEcchi", animeRepository.denyEcchi)
            })
        }

        val body = MkissaGraphQlTransport.executeRawQuery(
            client = httpClient,
            apiBaseUrl = config.endpoints.apiBaseUrl,
            query = query,
            variables = variablesObj,
            headers = getHeaders(buildId),
            authToken = AuthRepository.getValidAccessTokenOrNull(context)
        )

        val json = JSONObject(body)
        if (json.has("errors")) {
            val errors = json.optJSONArray("errors")
            val first = errors?.optJSONObject(0)
            throw IllegalStateException(first?.optString("message") ?: "Failed to load manga details")
        }

        val data = json.optJSONObject("data") ?: throw IllegalStateException("Response missing 'data' object.")
        val mangaObj = data.optJSONObject("manga") ?: throw IllegalStateException("Manga details not found.")

        val detail = AnimeDetail.fromJson(mangaObj)
        detail.copy(type = "manga")
    }

    override suspend fun fetchChapterPages(
        context: Context,
        mangaId: String,
        chapterString: String,
        translationType: String
    ): List<MangaPage> = withContext(Dispatchers.IO) {
        val cacheKey = "$mangaId:$chapterString:$translationType"
        chapterPagesCache[cacheKey]?.let { return@withContext it }

        var config = MkissaConfigProvider.getConfig(context)
        val effectiveLane = OperationRegistry.LANE_CHAPTER_PAGES

        val requestAuthToken = try {
            AuthRepository.getValidAccessTokenOrNull(context)
        } catch (_: Exception) {
            null
        } ?: if (!TokenManager.isAccessTokenExpired(context)) TokenManager.getAccessToken(context) else null

        suspend fun executePagesQuery(
            bootstrap: CryptoBootstrap,
            cfg: MkissaClientConfig,
            explicitAaReq: String? = null
        ): String {
            val activeQueryHash = cfg.operationHashes[OperationRegistry.OP_CHAPTER_PAGES]
                ?: cfg.getOperationHash(OperationRegistry.OP_CHAPTER_PAGES)

            val effectiveAaReq = explicitAaReq ?: Decryptor.generateAaReq(
                queryHash = activeQueryHash,
                bootstrap = bootstrap,
                buildId = cfg.buildId,
                config = cfg
            )

            val variablesObj = JSONObject().apply {
                put("mangaId", mangaId)
                put("translationType", translationType.lowercase())
                put("chapterString", chapterString)
                put("limit", 10)
                put("offset", 0)
            }

            val extensionsObj = JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", activeQueryHash)
                })
                put("k", effectiveLane)
                put("aaReq", effectiveAaReq)
            }

            val getUrl = "${cfg.endpoints.apiBaseUrl}?variables=${android.net.Uri.encode(variablesObj.toString())}&extensions=${android.net.Uri.encode(extensionsObj.toString())}"
            Log.d("MangaDebug", "FULL REQUEST URL:\n$getUrl")
            Log.d("MangaDebug", "AUTH TOKEN: ${requestAuthToken ?: "NONE"}")
            Log.d("MangaDebug", "VARIABLES: $variablesObj")
            Log.d("MangaDebug", "EXTENSIONS: $extensionsObj")

            return MkissaGraphQlTransport.executePersistedQuery(
                client = httpClient,
                apiBaseUrl = cfg.endpoints.apiBaseUrl,
                variables = variablesObj,
                extensions = extensionsObj,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Referer" to cfg.endpoints.appReferer,
                    "Origin" to cfg.endpoints.appOrigin,
                    "x-build-id" to cfg.buildId
                ),
                authToken = requestAuthToken
            )
        }

        Log.d("MangaDebug", ">>> START fetchChapterPages: mangaId=$mangaId, chapter=$chapterString, trans=$translationType, endpoint=${config.endpoints.apiBaseUrl}, token=${requestAuthToken ?: "NONE"}")

        var bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = requestAuthToken, config = config)
        Log.d("MangaDebug", "BOOTSTRAP: epoch=${bootstrap.epoch}, lane=${bootstrap.k}, partB=${bootstrap.partB}")
        var responseStr = try {
            val res = executePagesQuery(bootstrap, config)
            Log.d("MangaDebug", "PRIMARY RESPONSE: $res")
            res
        } catch (e: Exception) {
            Log.e("MangaDebug", "PRIMARY FAILED: ${e.message}", e)
            ""
        }


        // Retry with refreshed bootstrap or raw query fallback if primary failed
        if (responseStr.isBlank() || responseStr.contains("PERSISTED_QUERY_NOT_FOUND", ignoreCase = true) || responseStr.contains("AA_CRYPTO_MISSING", ignoreCase = true)) {
            val isNotFound = responseStr.contains("PERSISTED_QUERY_NOT_FOUND", ignoreCase = true)
            Log.d("MangaDebug", "Primary response needs fallback. isNotFound=$isNotFound, rawLen=${responseStr.length}")
            if (isNotFound) {
                // Backend requires raw GraphQL query body when the hash is not in its Redis/persisted cache
                try {
                    val rawQuery = """query(
  ${'$'}mangaId: String!
  ${'$'}translationType: VaildTranslationTypeMangaEnumType!
  ${'$'}chapterString: String!
  ${'$'}limit: Int!
  ${'$'}offset: Int
) {
  chapterPages(
    mangaId: ${'$'}mangaId
    translationType: ${'$'}translationType
    chapterString: ${'$'}chapterString
    limit: ${'$'}limit
    offset: ${'$'}offset
  ) {
    edges {
      streamerId
      sourceName
      chapterString
      pictureUrls
      pictureUrlsProcessed
      pictureUrlHead
    }
    manga {
      countryOfOrigin
    }
  }
}"""

                    val variablesObj = JSONObject().apply {
                        put("mangaId", mangaId)
                        put("translationType", translationType.lowercase())
                        put("chapterString", chapterString)
                        put("limit", 10)
                        put("offset", 0)
                    }
                    val rawQueryHash = OperationRegistry.computeQueryHash(rawQuery)
                    val effectiveAaReq = Decryptor.generateAaReq(
                        queryHash = rawQueryHash,
                        bootstrap = bootstrap,
                        buildId = config.buildId,
                        config = config
                    )
                    val extensionsObj = JSONObject().apply {
                        put("k", effectiveLane)
                        put("aaReq", effectiveAaReq)
                    }
                    Log.d("MangaDebug", "Executing raw query fallback to ${config.endpoints.apiBaseUrl} with hash $rawQueryHash")
                    responseStr = MkissaGraphQlTransport.executeRawQuery(
                        client = httpClient,
                        apiBaseUrl = config.endpoints.apiBaseUrl,
                        query = rawQuery,
                        variables = variablesObj,
                        headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
                            "Accept" to "*/*",
                            "Accept-Language" to "en-US,en;q=0.9",
                            "Referer" to config.endpoints.appReferer,
                            "Origin" to config.endpoints.appOrigin,
                            "x-build-id" to config.buildId
                        ),
                        authToken = requestAuthToken,
                        extensions = extensionsObj
                    )
                    Log.d("MangaDebug", "Raw query response: ${responseStr.take(500)}")
                } catch (e: Exception) {
                    Log.e("MangaDebug", "Raw chapter query fallback failed: ${e.message}", e)
                }
            } else {
                try {
                    bootstrap = Decryptor.getBootstrap(context, lane = effectiveLane, buildId = config.buildId, authToken = requestAuthToken, forceRefresh = true, config = config)
                    responseStr = executePagesQuery(bootstrap, config)
                    Log.d("MangaDebug", "Refresh retry response: ${responseStr.take(500)}")
                } catch (e: Exception) {
                    Log.e("MangaDebug", "Chapter query retry failed: ${e.message}", e)
                }
            }
        }

        val pages = parseChapterPages(responseStr, bootstrap, config)
        if (pages.isNotEmpty()) {
            chapterPagesCache[cacheKey] = pages
        }
        pages
    }

    private fun parseChapterPages(
        rawResponse: String,
        bootstrap: CryptoBootstrap? = null,
        config: MkissaClientConfig? = null
    ): List<MangaPage> {
        if (rawResponse.isBlank()) return emptyList()
        try {
            var root = JSONObject(rawResponse)
            val data = root.optJSONObject("data") ?: return emptyList()

            // Handle encrypted response wrapper: {"data": {"_m": "...", "tobeparsed": "..."}}
            val encryptedPayload = data.optString("tobeparsed", "")
            val effectiveData = if (encryptedPayload.isNotBlank() && bootstrap != null) {
                try {
                    val decryptedJsonStr = Decryptor.decrypt(
                        payload = encryptedPayload,
                        partBBase64 = bootstrap.partB,
                        buildId = bootstrap.buildId,
                        config = config
                    )
                    Log.d("MangaDebug", "Decrypted tobeparsed response: ${decryptedJsonStr.take(500)}")
                    val decryptedObj = JSONObject(decryptedJsonStr)
                    decryptedObj.optJSONObject("data") ?: decryptedObj
                } catch (e: Exception) {
                    Log.e("MangaDebug", "Failed to decrypt tobeparsed payload: ${e.message}", e)
                    data
                }
            } else {
                data
            }

            val chapterPagesObj = effectiveData.optJSONObject("chapterPages")
                ?: effectiveData.optJSONObject("chapter")
                ?: return emptyList()

            val edges = chapterPagesObj.optJSONArray("edges")
            val pages = mutableListOf<MangaPage>()


            if (edges != null && edges.length() > 0) {
                // Find edge matching chapter or default to best edge
                var bestEdge: JSONObject? = null
                for (i in 0 until edges.length()) {
                    val edge = edges.optJSONObject(i) ?: continue
                    val picUrls = edge.optJSONArray("pictureUrls")
                    if (picUrls != null && picUrls.length() > 0) {
                        bestEdge = edge
                        break
                    }
                }
                if (bestEdge == null) {
                    bestEdge = edges.optJSONObject(0)
                }

                if (bestEdge != null) {
                    var head = bestEdge.optString("pictureUrlHead", "")
                    if (head.isNotBlank() && !head.contains("//")) {
                        head = "https://$head"
                    }
                    if (head.isNotBlank() && !head.endsWith("/")) {
                        head = "$head/"
                    }

                    val picUrls = bestEdge.optJSONArray("pictureUrls")
                    if (picUrls != null) {
                        val effectiveHead = if (head.isNotBlank()) head else "https://ytimgf.youtube-anime.com/"
                        for (i in 0 until picUrls.length()) {
                            val urlItem = picUrls.optJSONObject(i)?.optString("url")
                                ?: picUrls.optString(i, "")
                            if (urlItem.isBlank()) continue

                            val fullUrl = if (urlItem.startsWith("http://") || urlItem.startsWith("https://") || urlItem.startsWith("blob:")) {
                                urlItem
                            } else if (urlItem.startsWith("//")) {
                                "https:$urlItem"
                            } else {
                                val path = if (urlItem.startsWith("/")) urlItem.substring(1) else urlItem
                                "$effectiveHead$path"
                            }

                            pages.add(
                                MangaPage(
                                    pageNumber = i + 1,
                                    imageUrl = fullUrl,
                                    headers = mapOf(
                                        "Referer" to "https://mkissa.to/",
                                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:158.0) Gecko/20100101 Firefox/158.0",
                                        "Accept" to "image/avif,image/jxl,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5"
                                    )
                                )
                            )
                        }
                    }
                }
            }

            // Fallback for flat pictureUrls
            if (pages.isEmpty()) {
                val flatUrls = chapterPagesObj.optJSONArray("pictureUrls")
                if (flatUrls != null) {
                    for (i in 0 until flatUrls.length()) {
                        val urlItem = flatUrls.optJSONObject(i)?.optString("url")
                            ?: flatUrls.optString(i, "")
                        if (urlItem.isNotBlank()) {
                            val fullUrl = if (urlItem.startsWith("http://") || urlItem.startsWith("https://")) {
                                urlItem
                            } else {
                                val path = if (urlItem.startsWith("/")) urlItem.substring(1) else urlItem
                                "https://ytimgf.youtube-anime.com/$path"
                            }
                            pages.add(
                                MangaPage(
                                    pageNumber = i + 1,
                                    imageUrl = fullUrl,
                                    headers = mapOf(
                                        "Referer" to "https://mkissa.to/",
                                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:158.0) Gecko/20100101 Firefox/158.0"
                                    )
                                )
                            )
                        }
                    }
                }
            }

            return pages
        } catch (e: Exception) {
            Log.e("DefaultMangaRepository", "Failed to parse chapter pages JSON: ${e.message}")
            return emptyList()
        }
    }

}
