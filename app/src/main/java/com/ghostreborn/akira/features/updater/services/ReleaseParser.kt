package com.ghostreborn.akira.features.updater.services

import com.ghostreborn.akira.features.updater.models.AppRelease
import com.ghostreborn.akira.features.updater.models.ReleaseAsset
import com.ghostreborn.akira.features.updater.models.ReleaseManifest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser for GitHub release JSON payloads and release manifests.
 */
object ReleaseParser {

    /**
     * Parses the GitHub release JSON object into an [AppRelease].
     * Returns null if the JSON is malformed or if no APK asset is found.
     */
    fun parseRelease(jsonString: String): AppRelease? {
        return try {
            val json = JSONObject(jsonString)
            val id = json.optLong("id", -1L)
            val tagName = json.optString("tag_name", "").trim()
            val name = json.optString("name", tagName).trim()
            val body = json.optString("body", "").trim()
            val htmlUrl = json.optString("html_url", "").trim()
            val draft = json.optBoolean("draft", false)
            val prerelease = json.optBoolean("prerelease", false)
            val publishedAt = if (json.has("published_at")) json.optString("published_at") else null

            // Assets list
            val assetsArray = json.optJSONArray("assets") ?: JSONArray()
            val assets = mutableListOf<ReleaseAsset>()
            for (i in 0 until assetsArray.length()) {
                val assetObj = assetsArray.optJSONObject(i) ?: continue
                val assetId = assetObj.optLong("id", -1L)
                val assetName = assetObj.optString("name", "")
                val size = assetObj.optLong("size", 0L)
                val contentType = if (assetObj.has("content_type") && !assetObj.isNull("content_type")) assetObj.optString("content_type") else null
                val downloadUrl = assetObj.optString("browser_download_url", "")
                val digest = if (assetObj.has("digest")) assetObj.optString("digest") else null
                val updatedAt = if (assetObj.has("updated_at") && !assetObj.isNull("updated_at")) assetObj.optString("updated_at") else null
                val createdAt = if (assetObj.has("created_at") && !assetObj.isNull("created_at")) assetObj.optString("created_at") else null

                if (assetName.isNotBlank() && downloadUrl.isNotBlank()) {
                    assets.add(
                        ReleaseAsset(
                            id = assetId,
                            name = assetName,
                            size = size,
                            contentType = contentType,
                            browserDownloadUrl = downloadUrl,
                            digest = digest,
                            updatedAt = updatedAt,
                            createdAt = createdAt
                        )
                    )
                }
            }

            // Find APK asset
            val apkAsset = selectApkAsset(assets) ?: return null

            // Look for release manifest asset (release-manifest.json, update-info.json, etc.)
            val manifestAsset = assets.firstOrNull {
                it.name.equals("release-manifest.json", ignoreCase = true) ||
                        it.name.equals("update-info.json", ignoreCase = true) ||
                        it.name.equals("manifest.json", ignoreCase = true)
            }

            // Parse embedded metadata from body or name if available
            val explicitVersionCode = parseVersionCodeFromBody(body)
            val minSupportedVersionCode = parseMinVersionCodeFromBody(body)
            val sha256 = parseSha256FromBody(body) ?: parseSha256FromDigest(apkAsset.digest)

            AppRelease(
                id = id,
                tagName = tagName,
                name = name,
                body = body,
                htmlUrl = htmlUrl,
                isDraft = draft,
                isPrerelease = prerelease,
                publishedAt = publishedAt,
                apkAsset = apkAsset,
                manifestAsset = manifestAsset,
                explicitVersionCode = explicitVersionCode,
                minSupportedVersionCode = minSupportedVersionCode,
                sha256Checksum = sha256
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Selects the most appropriate APK asset.
     * Excludes source archives (.zip, .tar.gz), symbols, or other files.
     */
    fun selectApkAsset(assets: List<ReleaseAsset>): ReleaseAsset? {
        val apkAssets = assets.filter {
            it.name.endsWith(".apk", ignoreCase = true)
        }
        if (apkAssets.isEmpty()) return null

        // Priority 1: release or standard universal apk
        val preferred = apkAssets.firstOrNull {
            it.name.contains("release", ignoreCase = true) && !it.name.contains("debug", ignoreCase = true)
        } ?: apkAssets.firstOrNull {
            it.name.contains("akira", ignoreCase = true) && !it.name.contains("debug", ignoreCase = true)
        } ?: apkAssets.firstOrNull {
            !it.name.contains("debug", ignoreCase = true)
        } ?: apkAssets.first()

        return preferred
    }

    /**
     * Parses a standalone release manifest JSON file.
     */
    fun parseManifest(manifestJson: String): ReleaseManifest? {
        return try {
            val json = JSONObject(manifestJson)
            val versionCode = json.optLong("versionCode", -1L)
            if (versionCode <= 0) return null

            val versionName = if (json.has("versionName") && !json.isNull("versionName")) json.optString("versionName") else null
            val minSupported = if (json.has("minSupportedVersionCode") && !json.isNull("minSupportedVersionCode")) json.optLong("minSupportedVersionCode") else null
            val apkFileName = if (json.has("apkFileName") && !json.isNull("apkFileName")) json.optString("apkFileName") else null
            val sha256 = if (json.has("sha256") && !json.isNull("sha256")) json.optString("sha256") else null
            val releaseNotes = if (json.has("releaseNotes") && !json.isNull("releaseNotes")) json.optString("releaseNotes") else null

            ReleaseManifest(
                versionCode = versionCode,
                versionName = versionName,
                minSupportedVersionCode = minSupported,
                apkFileName = apkFileName,
                sha256 = sha256,
                releaseNotes = releaseNotes
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts versionCode from release markdown body if documented as:
     * `versionCode: 12` or `versionCode=12` or `version_code: 12`.
     */
    fun parseVersionCodeFromBody(body: String): Long? {
        val regex = Regex("""(?i)(?:version[_\s-]?code)\s*[:=]\s*(\d+)""")
        return regex.find(body)?.groupValues?.get(1)?.toLongOrNull()
    }

    /**
     * Extracts minSupportedVersionCode from release markdown body if documented as:
     * `minSupportedVersionCode: 5` or `min_version_code: 5`.
     */
    fun parseMinVersionCodeFromBody(body: String): Long? {
        val regex = Regex("""(?i)(?:min(?:imum)?[_\s-]?supported[_\s-]?(?:version)?[_\s-]?code|min[_\s-]?version[_\s-]?code)\s*[:=]\s*(\d+)""")
        return regex.find(body)?.groupValues?.get(1)?.toLongOrNull()
    }

    /**
     * Extracts SHA-256 checksum from release markdown body if documented as:
     * `sha256: <64-hex>` or `SHA256: <64-hex>` or `checksum: <64-hex>`.
     */
    fun parseSha256FromBody(body: String): String? {
        val regex = Regex("""(?i)(?:sha256|sha-256|checksum)\s*[:=]\s*([a-f0-9]{64})""")
        return regex.find(body)?.groupValues?.get(1)?.lowercase()
    }

    /**
     * Extracts SHA-256 from asset digest string format `sha256:64-hex`.
     */
    fun parseSha256FromDigest(digest: String?): String? {
        if (digest.isNullOrBlank()) return null
        val prefix = "sha256:"
        return if (digest.startsWith(prefix, ignoreCase = true)) {
            val hash = digest.substring(prefix.length).trim().lowercase()
            if (hash.matches(Regex("^[a-f0-9]{64}$"))) hash else null
        } else {
            null
        }
    }
}
