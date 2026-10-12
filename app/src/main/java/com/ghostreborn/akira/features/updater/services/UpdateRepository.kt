package com.ghostreborn.akira.features.updater.services

import com.ghostreborn.akira.features.updater.models.AppRelease
import com.ghostreborn.akira.features.updater.models.ReleaseManifest
import com.ghostreborn.akira.features.updater.models.UpdateErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ReleaseFetchResult {
    data class Success(val release: AppRelease, val eTag: String?, val isNotModified: Boolean = false) : ReleaseFetchResult()
    data class Failure(val errorType: UpdateErrorType, val message: String, val retryAfterSeconds: Long? = null) : ReleaseFetchResult()
}

/**
 * Repository for interacting with GitHub Releases API.
 * Uses anonymous public requests with conditional HTTP headers (ETag, If-None-Match)
 * and handles GitHub rate limiting gracefully.
 */
class UpdateRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val owner: String = "TorqueReborn",
    private val repo: String = "Akira_Reborn",
    private val customBaseUrl: String? = null
) {

    private val latestReleaseEndpoint: String
        get() = customBaseUrl ?: "https://api.github.com/repos/$owner/$repo/releases/latest"

    /**
     * Fetches the latest published release.
     * Respects ETag for 304 Not Modified responses.
     */
    suspend fun fetchLatestRelease(
        eTag: String? = null,
        cachedRelease: AppRelease? = null
    ): ReleaseFetchResult = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(latestReleaseEndpoint)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Akira-Android-App-Updater")

        if (!eTag.isNullOrBlank()) {
            requestBuilder.header("If-None-Match", eTag)
        }

        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                val code = resp.code
                val responseETag = resp.header("ETag")

                // Handle 304 Not Modified
                if (code == 304 && cachedRelease != null) {
                    return@withContext ReleaseFetchResult.Success(
                        release = cachedRelease,
                        eTag = responseETag ?: eTag,
                        isNotModified = true
                    )
                }

                // Check Rate Limiting (403 with X-RateLimit-Remaining: 0 or 429)
                val remaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                val reset = resp.header("X-RateLimit-Reset")?.toLongOrNull()
                val retryAfter = resp.header("Retry-After")?.toLongOrNull()

                if (code == 429 || (code == 403 && remaining == 0)) {
                    val waitSec = retryAfter ?: if (reset != null) {
                        val currentSec = System.currentTimeMillis() / 1000L
                        (reset - currentSec).coerceAtLeast(0L)
                    } else 60L
                    return@withContext ReleaseFetchResult.Failure(
                        errorType = UpdateErrorType.RATE_LIMITED,
                        message = "GitHub API rate limit reached. Please try again later.",
                        retryAfterSeconds = waitSec
                    )
                }

                if (code == 404) {
                    return@withContext ReleaseFetchResult.Failure(
                        errorType = UpdateErrorType.NOT_FOUND,
                        message = "No release found on GitHub repository ($owner/$repo)."
                    )
                }

                if (!resp.isSuccessful) {
                    return@withContext ReleaseFetchResult.Failure(
                        errorType = UpdateErrorType.SERVER_ERROR,
                        message = "GitHub API error: HTTP $code ${resp.message}"
                    )
                }

                val bodyString = resp.body?.string().orEmpty()
                if (bodyString.isBlank()) {
                    return@withContext ReleaseFetchResult.Failure(
                        errorType = UpdateErrorType.SERVER_ERROR,
                        message = "Empty response received from GitHub API."
                    )
                }

                val parsedRelease = ReleaseParser.parseRelease(bodyString)
                    ?: return@withContext ReleaseFetchResult.Failure(
                        errorType = UpdateErrorType.NO_COMPATIBLE_APK,
                        message = "Latest release found but contains no compatible Android APK asset."
                    )

                // Filter out draft or prerelease if retrieved
                if (parsedRelease.isDraft || parsedRelease.isPrerelease) {
                    return@withContext ReleaseFetchResult.Failure(
                        errorType = UpdateErrorType.NOT_FOUND,
                        message = "Latest release is marked as draft or pre-release. Stable release required."
                    )
                }

                // If a release manifest asset is present, fetch it to enhance metadata
                val enhancedRelease = if (parsedRelease.manifestAsset != null) {
                    val manifest = fetchManifest(parsedRelease.manifestAsset.browserDownloadUrl)
                    if (manifest != null) {
                        parsedRelease.copy(
                            explicitVersionCode = manifest.versionCode,
                            minSupportedVersionCode = manifest.minSupportedVersionCode ?: parsedRelease.minSupportedVersionCode,
                            sha256Checksum = manifest.sha256 ?: parsedRelease.sha256Checksum
                        )
                    } else {
                        parsedRelease
                    }
                } else {
                    parsedRelease
                }

                ReleaseFetchResult.Success(
                    release = enhancedRelease,
                    eTag = responseETag
                )
            }
        } catch (e: IOException) {
            ReleaseFetchResult.Failure(
                errorType = UpdateErrorType.NETWORK_OFFLINE,
                message = "Unable to connect to GitHub. Please check your network connection."
            )
        } catch (e: Exception) {
            ReleaseFetchResult.Failure(
                errorType = UpdateErrorType.UNKNOWN,
                message = e.localizedMessage ?: "Unexpected error checking for updates."
            )
        }
    }

    /**
     * Downloads and parses a release manifest JSON file.
     */
    suspend fun fetchManifest(manifestUrl: String): ReleaseManifest? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(manifestUrl)
                .header("User-Agent", "Akira-Android-App-Updater")
                .build()
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val content = resp.body?.string().orEmpty()
                ReleaseParser.parseManifest(content)
            }
        } catch (_: Exception) {
            null
        }
    }
}
