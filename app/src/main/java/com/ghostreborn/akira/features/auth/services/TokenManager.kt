package com.ghostreborn.akira.features.auth.services

import android.content.Context
import android.content.SharedPreferences

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
        isEmailVerified: Boolean = false
    ) {
        val p = getPrefs(context)
        p.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
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

    fun isLoggedIn(context: Context? = null): Boolean {
        val token = getAccessToken(context)
        return !token.isNullOrBlank()
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
