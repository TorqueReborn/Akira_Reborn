package com.ghostreborn.akira.features.home.models

import com.ghostreborn.akira.utils.ImageUtils
import org.json.JSONObject

data class CommunityReview(
    val id: String,
    val userId: String?,
    val username: String?,
    val displayName: String?,
    val userAvatar: String?,
    val score: Double,
    val comment: String,
    val likesCount: Int,
    val createdDate: String?
) {
    companion object {
        fun fromJson(json: JSONObject): CommunityReview {
            val id = json.optString("_id", "")
            val userId = json.optString("userId").takeIf { it.isNotBlank() }
            val comment = json.optString("comment", "")
            val score = json.optDouble("uScore", json.optDouble("score", 0.0))
            val likesCount = json.optInt("likesCount", 0)
            val createdDate = json.optString("createdDate").takeIf { it.isNotBlank() }

            val userObj = json.optJSONObject("user")
            val username = userObj?.optString("username")?.takeIf { it.isNotBlank() }
            val displayName = userObj?.optString("displayName")?.takeIf { it.isNotBlank() } ?: username
            val rawAvatar = userObj?.optString("picture")?.takeIf { it.isNotBlank() }
            val userAvatar = ImageUtils.resolveUrl(rawAvatar)

            return CommunityReview(
                id = id,
                userId = userId,
                username = username,
                displayName = displayName,
                userAvatar = userAvatar,
                score = score,
                comment = comment,
                likesCount = likesCount,
                createdDate = createdDate
            )
        }
    }
}
