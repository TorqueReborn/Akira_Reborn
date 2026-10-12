package com.ghostreborn.akira.features.updater.models

/**
 * Metadata representation of a release asset from GitHub.
 */
data class ReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val contentType: String?,
    val browserDownloadUrl: String,
    val digest: String? = null, // e.g. "sha256:abc..." if provided by GitHub API
    val updatedAt: String? = null,
    val createdAt: String? = null
)

/**
 * Canonical model for an app update release retrieved from GitHub.
 */
data class AppRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val isDraft: Boolean,
    val isPrerelease: Boolean,
    val publishedAt: String?,
    val apkAsset: ReleaseAsset,
    val manifestAsset: ReleaseAsset? = null,
    val explicitVersionCode: Long? = null,
    val minSupportedVersionCode: Long? = null,
    val sha256Checksum: String? = null
)

/**
 * Standalone release manifest that can be published alongside releases
 * (e.g., release-manifest.json or update-info.json).
 */
data class ReleaseManifest(
    val versionCode: Long,
    val versionName: String?,
    val minSupportedVersionCode: Long? = null,
    val apkFileName: String?,
    val sha256: String?,
    val releaseNotes: String? = null
)
