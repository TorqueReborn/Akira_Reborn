package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.di.ServiceLocator
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.home.models.GuideThread
import com.ghostreborn.akira.features.home.models.WorldMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GuideRepository {
    private const val TAG = "GuideRepository"

    private val httpClient
        get() = ServiceLocator.okHttpClient

    private const val GUIDE_THREADS_FOR_MEDIA_QUERY = """
query GuideThreadsForMedia(
  ${'$'}scopeShowId: String
  ${'$'}scopeMangaId: String
  ${'$'}scopeWorkId: String
  ${'$'}page: Int
  ${'$'}size: Int
) {
  guideThreadsForMedia(
    scopeShowId: ${'$'}scopeShowId
    scopeMangaId: ${'$'}scopeMangaId
    scopeWorkId: ${'$'}scopeWorkId
    page: ${'$'}page
    size: ${'$'}size
  ) {
    edges {
      _id
      guideId
      creatorId
      creator {
        _id
        username
        displayName
        picture
      }
      kind
      title
      excerpt
      cardImageUrl
      postImagePath
      pinned
      locked
      replyCount
      createdDate
      views
      likesCount
      scopeShowId
      scopeMangaId
      relatedShowIds
      relatedMangaIds
      attachments {
        kind
        refId
        label
        sortOrder
      }
    }
    pageInfo {
      total
      hasNextPage
      nextPage
      limit
    }
  }
}
"""

    private const val GUIDE_THREAD_DETAIL_QUERY = """
query GuideThread(${'$'}threadId: String!) {
  guideThread(threadId: ${'$'}threadId) {
    _id
    guideId
    creatorId
    creator {
      _id
      username
      displayName
      picture
    }
    kind
    title
    content
    excerpt
    cardImageUrl
    postImagePath
    replyCount
    createdDate
    views
    likesCount
    attachments {
      kind
      refId
      label
      sortOrder
    }
    relatedMedia {
      _id
      format
      title
      name
      englishName
      cover
      isPrimary
    }
  }
}
"""

    private const val WORLD_MAPS_QUERY = """
query WorldMaps(${'$'}search: WorldMapSearch!, ${'$'}page: Int, ${'$'}size: Int) {
  worldMaps(search: ${'$'}search, page: ${'$'}page, size: ${'$'}size) {
    edges {
      _id
      title
      description
      creatorId
      privacy
      scopeTitle
      scopeShowId
      scopeWorkId
      scopeFranchiseKey
      parentMapId
      parentLocationId
      locationCount
      markerCount
      arcCount
      createdDate
      lastActionDate
      baseMapImage
      baseMapImageLive
      creator {
        _id
        username
        displayName
        picture
      }
    }
    pageInfo {
      total
      hasNextPage
      nextPage
      limit
    }
  }
}
"""

    private const val WORLD_MAP_DETAIL_QUERY = """
query WorldMap(${'$'}_id: String!, ${'$'}arcId: String, ${'$'}allowSpoiler: Boolean) {
  worldMap(_id: ${'$'}_id, arcId: ${'$'}arcId, allowSpoiler: ${'$'}allowSpoiler) {
    _id
    title
    description
    creatorId
    privacy
    scopeTitle
    scopeShowId
    scopeWorkId
    scopeFranchiseKey
    baseMapImage
    baseMapImageLive
    creator {
      _id
      username
      displayName
      picture
    }
    locations {
      locationId
      name
      englishName
      description
      x
      y
      thumb
    }
    arcs {
      arcId
      title
      description
      sortOrder
    }
    markers {
      markerId
      label
      locationId
      x
      y
    }
  }
}
"""

    suspend fun fetchGuidesForMedia(
        context: Context,
        showId: String? = null,
        mangaId: String? = null,
        page: Int = 1,
        size: Int = 20
    ): List<GuideThread> = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val queryHash = config.getOperationHash(com.ghostreborn.akira.core.config.OperationRegistry.OP_GUIDES_FOR_MEDIA)

        val variables = JSONObject().apply {
            if (!showId.isNullOrBlank()) put("scopeShowId", showId)
            if (!mangaId.isNullOrBlank()) put("scopeMangaId", mangaId)
            put("page", page)
            put("size", size)
        }

        val extensions = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        try {
            val raw = MkissaGraphQlTransport.executePersistedQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                variables = variables,
                extensions = extensions,
                headers = getHeaders(config),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val guideThreads = data.optJSONObject("guideThreadsForMedia") ?: return@withContext emptyList()
            val edges = guideThreads.optJSONArray("edges") ?: return@withContext emptyList()

            val list = mutableListOf<GuideThread>()
            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i)
                if (edge != null) {
                    list.add(GuideThread.fromJson(edge))
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch guides for media: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchGuideThreadDetail(
        context: Context,
        threadId: String
    ): GuideThread? = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val queryHash = config.getOperationHash(com.ghostreborn.akira.core.config.OperationRegistry.OP_GUIDE_DETAIL)

        val variables = JSONObject().apply {
            put("threadId", threadId)
        }

        val extensions = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        try {
            val raw = MkissaGraphQlTransport.executePersistedQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                variables = variables,
                extensions = extensions,
                headers = getHeaders(config),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext null
            val threadObj = data.optJSONObject("guideThread") ?: return@withContext null
            GuideThread.fromJson(threadObj)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch guide thread detail: ${e.message}")
            null
        }
    }

    suspend fun buildFranchiseWatchGuide(
        context: Context,
        animeDetail: com.ghostreborn.akira.features.home.models.AnimeDetail
    ): GuideThread = withContext(Dispatchers.IO) {
        val targetRelations = animeDetail.relatedShows.ifEmpty {
            animeDetail.relatedMangas
        }

        val primaryMedia = com.ghostreborn.akira.features.home.models.GuideRelatedMedia(
            id = animeDetail.id,
            format = animeDetail.type ?: "TV",
            title = animeDetail.name,
            name = animeDetail.name,
            englishName = animeDetail.englishName,
            cover = animeDetail.thumbnail,
            isPrimary = true
        )

        val mediaList = mutableListOf<com.ghostreborn.akira.features.home.models.GuideRelatedMedia>()
        mediaList.add(primaryMedia)

        // Resolve up to 10 related entries for watch order timeline
        val resolvedEntries = targetRelations.take(12).mapNotNull { rel ->
            val targetId = rel.targetId
            if (targetId.isBlank() || targetId == animeDetail.id) return@mapNotNull null
            try {
                val cached = AnimeRepository.getCachedAnimeDetail(targetId)
                val detail = cached ?: AnimeRepository.fetchAnimeDetail(targetId, context)
                rel to detail
            } catch (_: Exception) {
                null
            }
        }

        // Sort chronological order: prequels first, current show, then sequels, then side stories / movies
        val prequels = resolvedEntries.filter { it.first.relation.contains("prequel", ignoreCase = true) }
        val sequels = resolvedEntries.filter { it.first.relation.contains("sequel", ignoreCase = true) }
        val others = resolvedEntries.filter {
            !it.first.relation.contains("prequel", ignoreCase = true) &&
            !it.first.relation.contains("sequel", ignoreCase = true)
        }

        val sortedList = mutableListOf<Pair<com.ghostreborn.akira.features.home.models.AnimeRelation, com.ghostreborn.akira.features.home.models.AnimeDetail>>()
        sortedList.addAll(prequels)
        sortedList.addAll(sequels)
        sortedList.addAll(others)

        for ((rel, d) in sortedList) {
            mediaList.add(
                com.ghostreborn.akira.features.home.models.GuideRelatedMedia(
                    id = d.id,
                    format = "${rel.relation.uppercase()} • ${d.type ?: "ANIME"}",
                    title = d.name,
                    name = d.name,
                    englishName = d.englishName,
                    cover = d.thumbnail ?: d.banner,
                    isPrimary = false
                )
            )
        }

        val franchiseName = animeDetail.englishName ?: animeDetail.name
        val summaryNote = buildString {
            append("Official chronological and release watch order sequence for ")
            append(franchiseName)
            append(".\n\n")
            append("Tap any title below to instantly jump to its episodes and start watching in proper sequential order.")
        }

        GuideThread(
            id = "franchise_guide_${animeDetail.id}",
            guideId = animeDetail.id,
            kind = "WATCH_ORDER",
            title = "$franchiseName: Complete Watch Order",
            excerpt = "Chronological and release order guide for $franchiseName and its related seasons, movies & spin-offs.",
            content = summaryNote,
            cardImageUrl = animeDetail.banner ?: animeDetail.thumbnail,
            creatorName = "Akira Watch Order Engine",
            creatorAvatar = null,
            views = 12500,
            replyCount = mediaList.size,
            relatedMedia = mediaList
        )
    }


    suspend fun fetchWorldMaps(
        context: Context,
        showId: String? = null,
        page: Int = 1,
        size: Int = 10
    ): List<WorldMap> = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val queryHash = config.getOperationHash(com.ghostreborn.akira.core.config.OperationRegistry.OP_WORLD_MAPS)

        val search = JSONObject().apply {
            if (!showId.isNullOrBlank()) put("scopeShowId", showId)
            put("rootOnly", true)
        }

        val variables = JSONObject().apply {
            put("search", search)
            put("page", page)
            put("size", size)
        }

        val extensions = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        try {
            val raw = MkissaGraphQlTransport.executePersistedQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                variables = variables,
                extensions = extensions,
                headers = getHeaders(config),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val worldMaps = data.optJSONObject("worldMaps") ?: return@withContext emptyList()
            val edges = worldMaps.optJSONArray("edges") ?: return@withContext emptyList()

            val list = mutableListOf<WorldMap>()
            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i)
                if (edge != null) {
                    list.add(WorldMap.fromJson(edge))
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch world maps: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchWorldMapDetail(
        context: Context,
        mapId: String,
        arcId: String? = null
    ): WorldMap? = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)
        val queryHash = config.getOperationHash(com.ghostreborn.akira.core.config.OperationRegistry.OP_WORLD_MAP)

        val variables = JSONObject().apply {
            put("_id", mapId)
            if (!arcId.isNullOrBlank()) put("arcId", arcId)
            put("allowSpoiler", true)
        }

        val extensions = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", queryHash)
            })
        }

        try {
            val raw = MkissaGraphQlTransport.executePersistedQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                variables = variables,
                extensions = extensions,
                headers = getHeaders(config),
                authToken = token
            )

            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return@withContext null
            val mapObj = data.optJSONObject("worldMap") ?: return@withContext null
            WorldMap.fromJson(mapObj)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch world map detail: ${e.message}")
            null
        }
    }

    private fun getHeaders(config: com.ghostreborn.akira.core.config.MkissaClientConfig): Map<String, String> {
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Referer" to config.endpoints.appReferer,
            "Origin" to config.endpoints.appOrigin,
            "x-build-id" to config.buildId
        )
    }
}
