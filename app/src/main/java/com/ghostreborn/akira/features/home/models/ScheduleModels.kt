package com.ghostreborn.akira.features.home.models

import com.ghostreborn.akira.utils.ImageUtils
import org.json.JSONObject

data class ScheduleEntry(
    val scheduleId: String,
    val showId: String?,
    val title: String,
    val coverImage: String?,
    val airingAt: Long, // unix timestamp seconds
    val episode: Int,
    val score: Double?,
    val estimateLabel: String?
) {
    companion object {
        fun fromJson(json: JSONObject): ScheduleEntry {
            val scheduleId = json.optString("scheduleId", "")
            val airingAt = json.optLong("airingAt", 0L)
            val episode = json.optInt("episode", 0)

            val showObj = json.optJSONObject("show")
            val mediaObj = json.optJSONObject("media")

            val showId = showObj?.optString("_id")?.takeIf { it.isNotBlank() }

            val title = when {
                showObj != null -> {
                    val eng = showObj.optString("englishName").takeIf { it.isNotBlank() }
                    eng ?: showObj.optString("name", "Unknown Title")
                }
                mediaObj != null -> mediaObj.optString("title", "Unknown Title")
                else -> "Unknown Title"
            }

            val rawThumb = showObj?.optString("thumbnail") ?: mediaObj?.optString("coverImage")
            val coverImage = ImageUtils.resolveUrl(rawThumb)

            val score = showObj?.optDouble("score")?.takeIf { !it.isNaN() && it > 0 }

            val estimatesArr = json.optJSONArray("siteEstimates")
            val estimateLabel = if (estimatesArr != null && estimatesArr.length() > 0) {
                estimatesArr.optJSONObject(0)?.optString("label")?.takeIf { it.isNotBlank() }
            } else null

            return ScheduleEntry(
                scheduleId = scheduleId,
                showId = showId,
                title = title,
                coverImage = coverImage,
                airingAt = airingAt,
                episode = episode,
                score = score,
                estimateLabel = estimateLabel
            )
        }
    }
}

data class ScheduleDay(
    val dayKey: String, // e.g. "monday", "tuesday"
    val dayIndex: Int,
    val entries: List<ScheduleEntry>
) {
    companion object {
        fun fromJson(json: JSONObject): ScheduleDay {
            val dayKey = json.optString("dayKey", "")
            val dayIndex = json.optInt("dayIndex", 0)
            val entriesArr = json.optJSONArray("entries")
            val list = mutableListOf<ScheduleEntry>()
            if (entriesArr != null) {
                for (i in 0 until entriesArr.length()) {
                    val obj = entriesArr.optJSONObject(i) ?: continue
                    list.add(ScheduleEntry.fromJson(obj))
                }
            }
            return ScheduleDay(dayKey, dayIndex, list)
        }
    }
}

data class AiringScheduleData(
    val weekLabel: String,
    val timezone: String,
    val days: List<ScheduleDay>
) {
    companion object {
        fun fromJson(json: JSONObject): AiringScheduleData {
            val weekLabel = json.optString("weekLabel", "This Week")
            val timezone = json.optString("timezone", "UTC")
            val daysArr = json.optJSONArray("days")
            val list = mutableListOf<ScheduleDay>()
            if (daysArr != null) {
                for (i in 0 until daysArr.length()) {
                    val obj = daysArr.optJSONObject(i) ?: continue
                    list.add(ScheduleDay.fromJson(obj))
                }
            }
            return AiringScheduleData(weekLabel, timezone, list)
        }
    }
}
