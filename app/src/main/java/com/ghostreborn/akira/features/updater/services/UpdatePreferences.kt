package com.ghostreborn.akira.features.updater.services

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence manager for update check state, rate limits, caching, and dismissed updates.
 */
class UpdatePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastCheckTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK_TIME, value).apply()

    var lastETag: String?
        get() = prefs.getString(KEY_LAST_ETAG, null)
        set(value) = prefs.edit().putString(KEY_LAST_ETAG, value).apply()

    var lastCachedReleaseJson: String?
        get() = prefs.getString(KEY_CACHED_RELEASE_JSON, null)
        set(value) = prefs.edit().putString(KEY_CACHED_RELEASE_JSON, value).apply()

    var rateLimitResetTimestamp: Long
        get() = prefs.getLong(KEY_RATE_LIMIT_RESET, 0L)
        set(value) = prefs.edit().putLong(KEY_RATE_LIMIT_RESET, value).apply()

    var dismissedVersionCode: Long
        get() = prefs.getLong(KEY_DISMISSED_VERSION_CODE, -1L)
        set(value) = prefs.edit().putLong(KEY_DISMISSED_VERSION_CODE, value).apply()

    var installedAssetId: Long
        get() = prefs.getLong(KEY_INSTALLED_ASSET_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_INSTALLED_ASSET_ID, value).apply()

    var installedAssetUpdatedAt: String?
        get() = prefs.getString(KEY_INSTALLED_ASSET_UPDATED_AT, null)
        set(value) = prefs.edit().putString(KEY_INSTALLED_ASSET_UPDATED_AT, value).apply()

    fun isFresh(intervalMs: Long = DEFAULT_FRESHNESS_INTERVAL_MS): Boolean {
        val now = System.currentTimeMillis()
        val last = lastCheckTimestamp
        return (now - last) in 0 until intervalMs
    }

    fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        return rateLimitResetTimestamp > now
    }

    fun remainingRateLimitSeconds(): Long {
        val diffMs = rateLimitResetTimestamp - System.currentTimeMillis()
        return if (diffMs > 0) (diffMs / 1000L) else 0L
    }

    fun clearDismissed() {
        prefs.edit().remove(KEY_DISMISSED_VERSION_CODE).apply()
    }

    companion object {
        private const val PREFS_NAME = "akira_update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_LAST_ETAG = "last_etag"
        private const val KEY_CACHED_RELEASE_JSON = "cached_release_json"
        private const val KEY_RATE_LIMIT_RESET = "rate_limit_reset"
        private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
        private const val KEY_INSTALLED_ASSET_ID = "installed_asset_id"
        private const val KEY_INSTALLED_ASSET_UPDATED_AT = "installed_asset_updated_at"

        const val DEFAULT_FRESHNESS_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
