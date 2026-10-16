package com.ghostreborn.akira.features.home.services

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.network.MkissaGraphQlTransport
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.home.models.AiringScheduleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ScheduleRepository {
    private const val TAG = "ScheduleRepo"

    private val httpClient
        get() = com.ghostreborn.akira.core.di.ServiceLocator.okHttpClient

    private const val AIRING_SCHEDULE_QUERY = """
query AiringSchedule(${'$'}weekOffset: Int) {
  airingSchedule(weekOffset: ${'$'}weekOffset) {
    weekLabel
    timezone
    days {
      dayKey
      dayIndex
      entries {
        scheduleId
        aniListId
        airingAt
        episode
        media {
          id
          title
          coverImage
        }
        show {
          _id
          name
          englishName
          thumbnail
          score
          availableEpisodes
        }
        siteEstimates {
          label
        }
      }
    }
  }
}
"""

    suspend fun fetchAiringSchedule(
        context: Context,
        weekOffset: Int = 0
    ): AiringScheduleData? = withContext(Dispatchers.IO) {
        val config = MkissaConfigProvider.getConfig(context)
        val token = AuthRepository.getValidAccessTokenOrNull(context)

        val variables = JSONObject().apply {
            put("weekOffset", weekOffset)
        }

        try {
            val raw = MkissaGraphQlTransport.executeRawQuery(
                client = httpClient,
                apiBaseUrl = config.endpoints.apiBaseUrl,
                query = AIRING_SCHEDULE_QUERY.trim(),
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
            val scheduleObj = data.optJSONObject("airingSchedule") ?: return@withContext null
            AiringScheduleData.fromJson(scheduleObj)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch airing schedule: ${e.message}")
            null
        }
    }
}
