package com.ghostreborn.akira.core.config

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Registry and descriptor for GraphQL operations, their query hashes, and content lanes.
 */
data class OperationDescriptor(
    val operationName: String,
    val queryHash: String,
    val contentLane: String? = null // e.g. "k7" for episode, "k9" for chapterPages, "k2" for music
)

object OperationRegistry {
    // Operation identifiers
    const val OP_BROWSE = "browse"
    const val OP_MANGA_BROWSE = "mangaBrowse"
    const val OP_TOP_RANKED = "topRanked"
    const val OP_DETAIL = "detail"
    const val OP_EPISODE = "episode"
    const val OP_CHAPTER_PAGES = "chapterPages"
    const val OP_MUSIC = "music"
    const val OP_GUIDES_FOR_MEDIA = "guideThreadsForMedia"
    const val OP_GUIDE_DETAIL = "guideThread"
    const val OP_WORLD_MAPS = "worldMaps"
    const val OP_WORLD_MAP = "worldMap"

    // Default validated query hashes (fallback)
    const val DEFAULT_BROWSE_HASH = "8b319a0fda488e4319f1b6d99093ba12802f3fc039572f39bcc02d5f9b9d9b02"
    const val DEFAULT_MANGA_BROWSE_HASH = "b276e1bd49b2f9718dc8429035fc2f0a979e6027c561c6dd6605cd751a904331"
    const val DEFAULT_TOP_RANKED_HASH = "c947693e2a04dfa9074df5ec01c6c5209fc9f9556f3e5d420dbca97f9d1b6d98"
    const val DEFAULT_DETAIL_HASH = "c6c067496f962fba87c7aaf6c215a40a6d3933c69012bd15e4d8949e58f3c010"
    const val DEFAULT_EPISODE_HASH = "670bbf38d0868f446e2346c1e956ca2c40c416e733ca248fd54e04f1c8b99145"
    const val DEFAULT_CHAPTER_PAGES_HASH = "59c49e0a9d656fbcc048e332230820346ad038056ad00c22c137084fbe15ea52"
    const val DEFAULT_GUIDES_FOR_MEDIA_HASH = "b93e9e7c8fc522475a9009be0f0ff086e17be939c8fbcf3448cc8d29f5ba9960"
    const val DEFAULT_GUIDE_DETAIL_HASH = "b00f3566bf9b9a0e55fe7537c52508a8c3db255f90c452c1260bf1bda2ea0621"
    const val DEFAULT_WORLD_MAPS_HASH = "b3c84067b3446d5ff4b05750841a1ed23571fc2bfadfbef89707b84780773da8"
    const val DEFAULT_WORLD_MAP_HASH = "913ffcdf2fac51d0ca0d1053e400339cca4ff8e637e977c484f410a3d44d3494"

    // Content lane constants verified against frontend bundle
    const val LANE_EPISODE = "k7"
    const val LANE_CHAPTER_PAGES = "k9"
    const val LANE_MUSIC = "k2"

    private val HASH_REGEX = Regex("^[a-f0-9]{64}$", RegexOption.IGNORE_CASE)

    fun isValidSha256Hash(hash: String?): Boolean {
        return !hash.isNullOrBlank() && HASH_REGEX.matches(hash.trim())
    }

    /**
     * Computes the standard SHA-256 hex digest of a GraphQL query string.
     * Mirrors the frontend's persisted query hash generator: tR(e) -> Ml(e) -> YL(e) -> sha256Hex(query).
     */
    fun computeQueryHash(queryString: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(queryString.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Resolve the content lane for an operation or query string.
     * Verified pattern in frontend:
     *   chapterPages(...) -> k9
     *   music(...) -> k2
     *   episode(...) -> k7
     */
    fun resolveLaneForQuery(queryStringOrOp: String): String? {
        val str = queryStringOrOp.trim()
        return when {
            Regex("""\bchapterPages\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(str) || str == OP_CHAPTER_PAGES -> LANE_CHAPTER_PAGES
            Regex("""\bmusic\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(str) || str == OP_MUSIC -> LANE_MUSIC
            Regex("""\bepisode\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(str) || str == OP_EPISODE -> LANE_EPISODE
            else -> null
        }
    }

    /** Best-effort operation recognition for hashes found in downloaded frontend bundles. */
    fun resolveOperationForQuery(queryStringOrSnippet: String): String? {
        val str = queryStringOrSnippet.trim()
        val candidates = listOf(
            OP_CHAPTER_PAGES to Regex("""\bchapterPages\s*\(""", RegexOption.IGNORE_CASE),
            OP_TOP_RANKED to Regex("""\btop10\s*\(""", RegexOption.IGNORE_CASE),
            OP_EPISODE to Regex("""\bepisode\s*\(""", RegexOption.IGNORE_CASE),
            OP_MUSIC to Regex("""\bmusic\s*\(""", RegexOption.IGNORE_CASE),
            OP_MANGA_BROWSE to Regex("""\bmangas\s*\(""", RegexOption.IGNORE_CASE),
            OP_BROWSE to Regex("""\bshows\s*\(""", RegexOption.IGNORE_CASE),
            OP_DETAIL to Regex("""\bshow\s*\(""", RegexOption.IGNORE_CASE)
        )
        return candidates.mapNotNull { (operation, regex) ->
            regex.findAll(str).lastOrNull()?.range?.first?.let { operation to it }
        }.maxByOrNull { it.second }?.first
    }
}
