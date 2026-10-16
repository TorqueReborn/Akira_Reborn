package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.home.models.CommunityReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ReviewRepository {
    private const val TAG = "ReviewRepository"

    private val httpClient
        get() = com.ghostreborn.akira.core.di.ServiceLocator.okHttpClient

    private const val REVIEWS_QUERY = """
query(
  ${'$'}search: ReviewsSearch!
) {
  queryReviews(
    search: ${'$'}search
  ) {
    edges {
      _id
      userId
      comment
      createdDate
      likesCount
      uScore
      user {
        _id
        username
        displayName
        picture
      }
    }
    pageInfo {
      total
    }
  }
}
"""

    suspend fun fetchReviews(
        context: Context,
        showId: String,
        limit: Int = 10
    ): List<CommunityReview> = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)

        val searchObj = JSONObject().apply {
            put("showId", showId.trim())
            put("limit", limit)
        }

        val variables = JSONObject().apply {
            put("search", searchObj)
        }

        try {
            val raw = MkissaGraphQlTransport.executeRawQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = REVIEWS_QUERY.trim(),
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
            val queryReviews = data.optJSONObject("queryReviews") ?: return@withContext emptyList()
            val edges = queryReviews.optJSONArray("edges") ?: return@withContext emptyList()

            val list = mutableListOf<CommunityReview>()
            for (i in 0 until edges.length()) {
                val obj = edges.optJSONObject(i) ?: continue
                list.add(CommunityReview.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch reviews: ${e.message}")
            emptyList()
        }
    }
}
