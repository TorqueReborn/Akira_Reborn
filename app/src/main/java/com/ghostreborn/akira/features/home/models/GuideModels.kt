package com.ghostreborn.akira.features.home.models

import org.json.JSONArray
import org.json.JSONObject

/**
 * Model representing a guide thread or watch guide entry.
 */
data class GuideThread(
    val id: String,
    val guideId: String,
    val kind: String, // e.g. "WATCH_ORDER", "FILLER", "CANON", "GENERAL", "MAP"
    val title: String,
    val excerpt: String? = null,
    val content: String? = null,
    val cardImageUrl: String? = null,
    val creatorName: String? = null,
    val creatorAvatar: String? = null,
    val replyCount: Int = 0,
    val views: Long = 0,
    val likesCount: Long = 0,
    val createdDate: String? = null,
    val attachments: List<GuideAttachment> = emptyList(),
    val relatedMedia: List<GuideRelatedMedia> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): GuideThread {
            val creatorObj = json.optJSONObject("creator")
            val creatorName = creatorObj?.optString("displayName").takeIf { !it.isNullOrBlank() }
                ?: creatorObj?.optString("username").takeIf { !it.isNullOrBlank() }
            val creatorAvatar = creatorObj?.optString("picture").takeIf { !it.isNullOrBlank() }

            val attachmentsList = mutableListOf<GuideAttachment>()
            val attachmentsArr = json.optJSONArray("attachments")
            if (attachmentsArr != null) {
                for (i in 0 until attachmentsArr.length()) {
                    val aObj = attachmentsArr.optJSONObject(i)
                    if (aObj != null) attachmentsList.add(GuideAttachment.fromJson(aObj))
                }
            }

            val mediaList = mutableListOf<GuideRelatedMedia>()
            val mediaArr = json.optJSONArray("relatedMedia")
            if (mediaArr != null) {
                for (i in 0 until mediaArr.length()) {
                    val mObj = mediaArr.optJSONObject(i)
                    if (mObj != null) mediaList.add(GuideRelatedMedia.fromJson(mObj))
                }
            }

            return GuideThread(
                id = json.optString("_id"),
                guideId = json.optString("guideId"),
                kind = json.optString("kind", "GENERAL"),
                title = json.optString("title", "Untitled Guide"),
                excerpt = json.optString("excerpt").takeIf { it.isNotBlank() },
                content = json.optString("content").takeIf { it.isNotBlank() },
                cardImageUrl = json.optString("cardImageUrl").takeIf { it.isNotBlank() }
                    ?: json.optString("postImagePath").takeIf { it.isNotBlank() },
                creatorName = creatorName,
                creatorAvatar = creatorAvatar,
                replyCount = json.optInt("replyCount", 0),
                views = json.optLong("views", 0),
                likesCount = json.optLong("likesCount", 0),
                createdDate = json.optString("createdDate").takeIf { it.isNotBlank() },
                attachments = attachmentsList,
                relatedMedia = mediaList
            )
        }
    }
}

data class GuideAttachment(
    val kind: String? = null,
    val refId: String? = null,
    val label: String? = null,
    val sortOrder: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): GuideAttachment {
            return GuideAttachment(
                kind = json.optString("kind").takeIf { it.isNotBlank() },
                refId = json.optString("refId").takeIf { it.isNotBlank() },
                label = json.optString("label").takeIf { it.isNotBlank() },
                sortOrder = json.optInt("sortOrder", 0)
            )
        }
    }
}

data class GuideRelatedMedia(
    val id: String,
    val format: String? = null,
    val title: String? = null,
    val name: String? = null,
    val englishName: String? = null,
    val cover: String? = null,
    val isPrimary: Boolean = false
) {
    val displayTitle: String
        get() = listOfNotNull(englishName, name, title)
            .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Anime"

    companion object {
        fun fromJson(json: JSONObject): GuideRelatedMedia {
            fun clean(s: String?): String? = s?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

            return GuideRelatedMedia(
                id = json.optString("_id"),
                format = clean(json.optString("format")),
                title = clean(json.optString("title")),
                name = clean(json.optString("name")),
                englishName = clean(json.optString("englishName")),
                cover = clean(json.optString("cover")),
                isPrimary = json.optBoolean("isPrimary", false)
            )
        }
    }
}

/**
 * Model representing an interactive world or universe map.
 */
data class WorldMap(
    val id: String,
    val title: String,
    val description: String? = null,
    val baseMapImage: String? = null,
    val scopeTitle: String? = null,
    val locationCount: Int = 0,
    val markerCount: Int = 0,
    val arcCount: Int = 0,
    val creatorName: String? = null,
    val locations: List<WorldMapLocation> = emptyList(),
    val arcs: List<WorldMapArc> = emptyList(),
    val markers: List<WorldMapMarker> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): WorldMap {
            val creatorObj = json.optJSONObject("creator")
            val creatorName = creatorObj?.optString("displayName").takeIf { !it.isNullOrBlank() }
                ?: creatorObj?.optString("username").takeIf { !it.isNullOrBlank() }

            val locationsList = mutableListOf<WorldMapLocation>()
            val locArr = json.optJSONArray("locations")
            if (locArr != null) {
                for (i in 0 until locArr.length()) {
                    val lObj = locArr.optJSONObject(i)
                    if (lObj != null) locationsList.add(WorldMapLocation.fromJson(lObj))
                }
            }

            val arcsList = mutableListOf<WorldMapArc>()
            val arcArr = json.optJSONArray("arcs")
            if (arcArr != null) {
                for (i in 0 until arcArr.length()) {
                    val aObj = arcArr.optJSONObject(i)
                    if (aObj != null) arcsList.add(WorldMapArc.fromJson(aObj))
                }
            }

            val markersList = mutableListOf<WorldMapMarker>()
            val markArr = json.optJSONArray("markers")
            if (markArr != null) {
                for (i in 0 until markArr.length()) {
                    val mObj = markArr.optJSONObject(i)
                    if (mObj != null) markersList.add(WorldMapMarker.fromJson(mObj))
                }
            }

            return WorldMap(
                id = json.optString("_id"),
                title = json.optString("title", "World Map"),
                description = json.optString("description").takeIf { it.isNotBlank() },
                baseMapImage = json.optString("baseMapImageLive").takeIf { it.isNotBlank() }
                    ?: json.optString("baseMapImage").takeIf { it.isNotBlank() },
                scopeTitle = json.optString("scopeTitle").takeIf { it.isNotBlank() },
                locationCount = json.optInt("locationCount", locationsList.size),
                markerCount = json.optInt("markerCount", markersList.size),
                arcCount = json.optInt("arcCount", arcsList.size),
                creatorName = creatorName,
                locations = locationsList,
                arcs = arcsList,
                markers = markersList
            )
        }
    }
}

data class WorldMapLocation(
    val locationId: String,
    val name: String,
    val englishName: String? = null,
    val description: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val thumb: String? = null
) {
    val displayName: String get() = englishName ?: name

    companion object {
        fun fromJson(json: JSONObject): WorldMapLocation {
            return WorldMapLocation(
                locationId = json.optString("locationId").takeIf { it.isNotBlank() } ?: json.optString("_id"),
                name = json.optString("name", "Unknown Place"),
                englishName = json.optString("englishName").takeIf { it.isNotBlank() },
                description = json.optString("description").takeIf { it.isNotBlank() },
                x = json.optDouble("x", 0.0).toFloat(),
                y = json.optDouble("y", 0.0).toFloat(),
                thumb = json.optString("thumb").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class WorldMapArc(
    val arcId: String,
    val title: String,
    val description: String? = null,
    val sortOrder: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): WorldMapArc {
            return WorldMapArc(
                arcId = json.optString("arcId"),
                title = json.optString("title", "Arc"),
                description = json.optString("description").takeIf { it.isNotBlank() },
                sortOrder = json.optInt("sortOrder", 0)
            )
        }
    }
}

data class WorldMapMarker(
    val markerId: String,
    val label: String? = null,
    val locationId: String? = null,
    val x: Float = 0f,
    val y: Float = 0f
) {
    companion object {
        fun fromJson(json: JSONObject): WorldMapMarker {
            return WorldMapMarker(
                markerId = json.optString("markerId").takeIf { it.isNotBlank() } ?: json.optString("_id"),
                label = json.optString("label").takeIf { it.isNotBlank() },
                locationId = json.optString("locationId").takeIf { it.isNotBlank() },
                x = json.optDouble("x", 0.0).toFloat(),
                y = json.optDouble("y", 0.0).toFloat()
            )
        }
    }
}
