package com.ghostreborn.akira.features.auth.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject

object TokenManager {
    private const val PREFS_NAME = "akira_auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_PICTURE = "picture"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_EMAIL_VERIFIED = "email_verified"
    private const val KEY_ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"
    private const val DEFAULT_TOKEN_LIFETIME_MS = 9L * 60L * 60L * 1000L

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(context: Context? = null): SharedPreferences {
        if (prefs == null && context != null) {
            init(context)
        }
        return prefs ?: throw IllegalStateException("TokenManager must be initialized with Context first.")
    }

    fun saveAuthData(
        context: Context,
        accessToken: String,
        refreshToken: String,
        sessionId: String,
        username: String,
        displayName: String? = null,
        picture: String? = null,
        userId: String? = null,
        email: String? = null,
        isEmailVerified: Boolean = false,
        tokenExpiresAtMs: Long? = null
    ) {
        val p = getPrefs(context)
        p.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(
                KEY_ACCESS_TOKEN_EXPIRES_AT,
                tokenExpiresAtMs ?: parseJwtExpirationMs(accessToken)
                    ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
            )
            putString(KEY_SESSION_ID, sessionId)
            putString(KEY_USERNAME, username)
            if (displayName != null) putString(KEY_DISPLAY_NAME, displayName) else remove(KEY_DISPLAY_NAME)
            if (picture != null) putString(KEY_PICTURE, picture) else remove(KEY_PICTURE)
            if (userId != null) putString(KEY_USER_ID, userId) else remove(KEY_USER_ID)
            if (email != null) putString(KEY_EMAIL, email) else remove(KEY_EMAIL)
            putBoolean(KEY_EMAIL_VERIFIED, isEmailVerified)
            apply()
        }
    }

    fun getAccessToken(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_ACCESS_TOKEN, null) } catch (_: Exception) { null }

    fun getRefreshToken(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_REFRESH_TOKEN, null) } catch (_: Exception) { null }

    fun getAccessTokenExpiresAt(context: Context? = null): Long =
        try { getPrefs(context).getLong(KEY_ACCESS_TOKEN_EXPIRES_AT, 0L) } catch (_: Exception) { 0L }

    fun isAccessTokenExpired(context: Context? = null, leewaySeconds: Long = 0L): Boolean {
        val token = getAccessToken(context)
        if (token.isNullOrBlank()) return true
        val storedExpiry = getAccessTokenExpiresAt(context)
        val expiresAt = if (storedExpiry > 0L) storedExpiry else parseJwtExpirationMs(token) ?: return false
        return expiresAt <= System.currentTimeMillis() + leewaySeconds.coerceAtLeast(0L) * 1000L
    }

    fun isAccessTokenExpiringSoon(context: Context? = null, leewaySeconds: Long = 120L): Boolean {
        val token = getAccessToken(context)
        if (token.isNullOrBlank()) return true
        val storedExpiry = getAccessTokenExpiresAt(context)
        // If a pre-update install has no expiry metadata and the token is opaque, refresh it once
        // when a refresh token is available instead of silently treating it as valid forever.
        val expiresAt = if (storedExpiry > 0L) storedExpiry else parseJwtExpirationMs(token)
        if (expiresAt == null) return !getRefreshToken(context).isNullOrBlank()
        return expiresAt <= System.currentTimeMillis() + leewaySeconds.coerceAtLeast(0L) * 1000L
    }

    fun getSessionId(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_SESSION_ID, null) } catch (_: Exception) { null }

    fun getUsername(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_USERNAME, null) } catch (_: Exception) { null }

    fun getDisplayName(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_DISPLAY_NAME, null) } catch (_: Exception) { null }

    fun getPicture(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_PICTURE, null) } catch (_: Exception) { null }

    fun getUserId(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_USER_ID, null) } catch (_: Exception) { null }

    fun getEmail(context: Context? = null): String? =
        try { getPrefs(context).getString(KEY_EMAIL, null) } catch (_: Exception) { null }

    fun isEmailVerified(context: Context? = null): Boolean =
        try { getPrefs(context).getBoolean(KEY_EMAIL_VERIFIED, false) } catch (_: Exception) { false }

    fun setEmailVerified(context: Context, verified: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EMAIL_VERIFIED, verified).apply()
    }

    fun isLoggedIn(context: Context? = null): Boolean {
        // A refresh token is enough to restore an expired access-token session.
        return !getAccessToken(context).isNullOrBlank() || !getRefreshToken(context).isNullOrBlank()
    }

    private fun parseJwtExpirationMs(token: String): Long? {
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val expSeconds = JSONObject(String(decoded, Charsets.UTF_8)).optLong("exp", 0L)
            if (expSeconds > 0L) expSeconds * 1000L else null
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
