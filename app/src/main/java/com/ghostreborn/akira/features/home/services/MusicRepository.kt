package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.home.models.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object MusicRepository {
    private const val TAG = "MusicRepo"

    private val httpClient
        get() = com.ghostreborn.akira.core.di.ServiceLocator.okHttpClient

    private const val MUSICS_QUERY = """
query(
  ${'$'}search: MusicSearchInput
  ${'$'}page: Int
  ${'$'}limit: Int
) {
  musics(
    search: ${'$'}search
    page: ${'$'}page
    limit: ${'$'}limit
  ) {
    edges {
      _id
      musicTitle
      artist
      cover
      type
      duration
      musicUrls
      album
      show {
        showId
        name
        thumbnail
      }
    }
    pageInfo {
      total
    }
  }
}
"""

    suspend fun fetchMusics(
        context: Context,
        query: String? = null,
        showId: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): List<MusicTrack> = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)

        val searchObj = JSONObject().apply {
            if (!query.isNullOrBlank()) {
                put("query", query.trim())
            }
            if (!showId.isNullOrBlank()) {
                put("showId", showId.trim())
            }
        }

        val variables = JSONObject().apply {
            put("search", searchObj)
            put("page", page)
            put("limit", limit)
        }

        try {
            val raw = MkissaGraphQlTransport.executeRawQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = MUSICS_QUERY.trim(),
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
            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val musics = data.optJSONObject("musics") ?: return@withContext emptyList()
            val edges = musics.optJSONArray("edges") ?: return@withContext emptyList()

            val list = mutableListOf<MusicTrack>()
            for (i in 0 until edges.length()) {
                val obj = edges.optJSONObject(i) ?: continue
                list.add(MusicTrack.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch musics: ${e.message}")
            emptyList()
        }
    }
}
