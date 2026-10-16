package com.ghostreborn.akira.features.auth.services

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.features.auth.models.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object AuthRepository {
    val API_URL: String
        get() = MkissaConfigProvider.currentEndpoints.apiBaseUrl

    private const val AUTHENTICATE_MUTATION = """
mutation(
  ${'$'}username: String
  ${'$'}email: String
  ${'$'}password: String!
  ${'$'}recaptchCode: String
  ${'$'}captchaProvider: String
) {
  authenticate(
    serviceName: "password"
    params: {
      user: { username: ${'$'}username, email: ${'$'}email }
      password: ${'$'}password
      recaptchCode: ${'$'}recaptchCode
      captchaProvider: ${'$'}captchaProvider
    }
  ) {
    sessionId
    tokens {
      refreshToken
      accessToken
    }
    user {
      _id
      username
      displayName
      picture
      emails {
        verified
        address
      }
    }
  }
}
"""

    private const val REFRESH_TOKENS_MUTATION = """
mutation(
  ${'$'}accessToken: String!
  ${'$'}refreshToken: String!
) {
  refreshTokens(refreshToken: ${'$'}refreshToken, accessToken: ${'$'}accessToken) {
    sessionId
    tokens {
      refreshToken
      accessToken
    }
    user {
      _id
      username
      displayName
      picture
      emails {
        verified
        address
      }
    }
  }
}
"""

    private const val LOGOUT_MUTATION = """
mutation {
  logout
}
"""

    private const val REGISTER_MUTATION = """
mutation(
  ${'$'}user: CreateUserInput!
  ${'$'}recaptchCode: String
  ${'$'}captchaProvider: String
) {
  createUser(user: ${'$'}user, recaptchCode: ${'$'}recaptchCode, captchaProvider: ${'$'}captchaProvider) {
    loginResult {
      sessionId
      tokens {
        refreshToken
        accessToken
      }
      user {
        _id
        username
        displayName
        picture
        emails {
          verified
          address
        }
      }
    }
    userId
  }
}
"""

    private const val RESEND_VERIFICATION_MUTATION = """
mutation(
  ${'$'}email: String!
  ${'$'}recaptchCode: String
  ${'$'}captchaProvider: String
) {
  resendVerificationLink(email: ${'$'}email, recaptchCode: ${'$'}recaptchCode, captchaProvider: ${'$'}captchaProvider) {
    success
    error
  }
}
"""

    private const val VERIFY_EMAIL_MUTATION = """
mutation(
  ${'$'}email: String!
  ${'$'}code: String!
) {
  verifyEmailCode(email: ${'$'}email, code: ${'$'}code)
}
"""


    private val httpClient: OkHttpClient
        get() = com.ghostreborn.akira.core.di.ServiceLocator.okHttpClient

    fun hashPasswordToSha256(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val refreshMutex = Mutex()

    /**
     * Returns an access token suitable for a request. Tokens close to expiry are refreshed using
     * the same refreshTokens mutation the Mkissa website uses. A temporary network failure keeps
     * a still-valid token usable; an expired token is never returned.
     */
    suspend fun getValidAccessTokenOrNull(context: Context?): String? {
        val current = TokenManager.getAccessToken(context)
        if (current.isNullOrBlank()) return null
        if (!TokenManager.isAccessTokenExpiringSoon(context, leewaySeconds = 120L)) return current

        val refresh = TokenManager.getRefreshToken(context)
        if (refresh.isNullOrBlank() || context == null) {
            return if (TokenManager.isAccessTokenExpired(context)) null else current
        }

        return refreshMutex.withLock {
            // Re-check under lock in case another coroutine just refreshed it
            val tokenAfterLock = TokenManager.getAccessToken(context)
            if (!tokenAfterLock.isNullOrBlank() && !TokenManager.isAccessTokenExpiringSoon(context, leewaySeconds = 120L)) {
                return@withLock tokenAfterLock
            }

            try {
                refreshTokens(context)
            } catch (e: Exception) {
                Log.w("AuthRepo", "Token refresh failed: ${e.message}")
                val msg = (e.message ?: "").lowercase()
                val isExplicitAuthRejection = msg.contains("401") ||
                        msg.contains("403") ||
                        msg.contains("invalid") ||
                        msg.contains("expired") ||
                        msg.contains("session") ||
                        msg.contains("unauthorized")

                if (isExplicitAuthRejection && TokenManager.isAccessTokenExpired(context)) {
                    TokenManager.clear(context)
                    null
                } else if (!TokenManager.isAccessTokenExpired(context)) {
                    // Still within valid lifetime despite refresh failure (e.g. temporary offline)
                    current
                } else {
                    null
                }
            }
        }
    }

    /** Refreshes the current session and persists the returned token pair and profile fields. */
    suspend fun refreshTokens(context: Context): String = withContext(Dispatchers.IO) {
        val oldAccessToken = TokenManager.getAccessToken(context).orEmpty()
        val oldRefreshToken = TokenManager.getRefreshToken(context).orEmpty()
        if (oldRefreshToken.isBlank()) {
            throw IllegalStateException("No refresh token is available. Please sign in again.")
        }

        val variables = JSONObject().apply {
            put("accessToken", oldAccessToken)
            put("refreshToken", oldRefreshToken)
        }
        val bodyJson = JSONObject().apply {
            put("query", REFRESH_TOKENS_MUTATION.trim())
            put("variables", variables)
        }
        val body = bodyJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
        val config = MkissaConfigProvider.getConfig(context)
        val endpoints = config.endpoints
        val request = Request.Builder()
            .url(endpoints.apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("Accept", "application/json, text/plain, */*")
            .header("Origin", endpoints.authOrigin)
            .header("Referer", endpoints.authReferer)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36")
            .header("x-build-id", config.buildId)
            .header("Host", endpoints.apiHostHeader)
            .apply {
                if (oldAccessToken.isNotBlank()) {
                    header("authorization", if (oldAccessToken.startsWith("Bearer ", true)) oldAccessToken else "Bearer $oldAccessToken")
                }
            }
            .build()

        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Token refresh failed with HTTP ${response.code}.")
            }
            val json = try { JSONObject(raw) } catch (_: Exception) {
                throw IllegalStateException("Token refresh returned an invalid response.")
            }
            val errors = json.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                val first = errors.optJSONObject(0)
                val message = first?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "Your session could not be refreshed. Please sign in again."
                throw IllegalStateException(message)
            }

            val result = json.optJSONObject("data")?.optJSONObject("refreshTokens")
                ?: throw IllegalStateException("Token refresh response did not contain a session.")
            val tokens = result.optJSONObject("tokens")
                ?: throw IllegalStateException("Token refresh response did not contain tokens.")
            val newAccess = tokens.optString("accessToken", "")
            val newRefresh = tokens.optString("refreshToken", "").ifBlank { oldRefreshToken }
            if (newAccess.isBlank()) {
                throw IllegalStateException("Token refresh returned an empty access token.")
            }

            val user = result.optJSONObject("user")
            val emails = user?.optJSONArray("emails")
            val firstEmail = if (emails != null && emails.length() > 0) emails.optJSONObject(0) else null
            TokenManager.saveAuthData(
                context = context,
                accessToken = newAccess,
                refreshToken = newRefresh,
                sessionId = result.optString("sessionId", TokenManager.getSessionId(context).orEmpty()),
                username = user?.optString("username")?.takeIf { it.isNotBlank() }
                    ?: TokenManager.getUsername(context).orEmpty(),
                displayName = user?.optString("displayName")?.takeIf { it.isNotBlank() }
                    ?: TokenManager.getDisplayName(context),
                picture = user?.optString("picture")?.takeIf { it.isNotBlank() }
                    ?: TokenManager.getPicture(context),
                userId = user?.optString("_id")?.takeIf { it.isNotBlank() }
                    ?: TokenManager.getUserId(context),
                email = firstEmail?.optString("address")?.takeIf { it.isNotBlank() }
                    ?: TokenManager.getEmail(context),
                isEmailVerified = firstEmail?.optBoolean("verified", TokenManager.isEmailVerified(context))
                    ?: TokenManager.isEmailVerified(context)
            )
            newAccess
        }
    }


    suspend fun authenticate(
        usernameOrEmail: String,
        rawPassword: String,
        recaptchCode: String,
        captchaProvider: String = MkissaConfigProvider.currentEndpoints.captchaProvider,
        context: Context? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val idTrimmed = usernameOrEmail.trim()
        val passTrimmed = rawPassword.trim()
        val capTrimmed = recaptchCode.trim()

        if (idTrimmed.isEmpty()) throw IllegalArgumentException("Username or email is required.")
        if (passTrimmed.isEmpty()) throw IllegalArgumentException("Password is required.")
        if (capTrimmed.isEmpty()) throw IllegalArgumentException("Cloudflare Turnstile token is required.")

        val isSha256 = passTrimmed.length == 64 && passTrimmed.matches(Regex("^[0-9a-fA-F]+$"))
        val hashedPassword = if (isSha256) passTrimmed.lowercase() else hashPasswordToSha256(passTrimmed)

        val isEmail = idTrimmed.contains("@")
        val variables = JSONObject().apply {
            if (isEmail) put("email", idTrimmed) else put("username", idTrimmed)
            put("password", hashedPassword)
            put("recaptchCode", capTrimmed)
            put("captchaProvider", captchaProvider)
        }

        val requestBodyJson = JSONObject().apply {
            put("query", AUTHENTICATE_MUTATION.trim())
            put("variables", variables)
        }

        val mediaType = "application/json; charset=UTF-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val endpoints = config.endpoints

        val request = Request.Builder()
            .url(endpoints.apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", endpoints.authOrigin)
            .header("Referer", endpoints.authReferer)
            .header("x-build-id", config.buildId)
            .header("Host", endpoints.apiHostHeader)
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Priority", "u=4")
            .build()

        Log.d("AuthRepo", "Sending authenticate mutation with id=$idTrimmed, buildId=${config.buildId}")
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string() ?: ""
            Log.d("AuthRepo", "Auth response code: ${response.code}")
            parseAuthResponse(raw, idTrimmed)
        }
    }

    suspend fun register(
        username: String,
        email: String,
        rawPassword: String,
        recaptchCode: String,
        captchaProvider: String = MkissaConfigProvider.currentEndpoints.captchaProvider,
        context: Context? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val userTrimmed = username.trim()
        val emailTrimmed = email.trim()
        val passTrimmed = rawPassword.trim()
        val capTrimmed = recaptchCode.trim()

        if (userTrimmed.isEmpty()) throw IllegalArgumentException("Username is required.")
        if (emailTrimmed.isEmpty()) throw IllegalArgumentException("Email is required.")
        if (passTrimmed.isEmpty()) throw IllegalArgumentException("Password is required.")
        if (capTrimmed.isEmpty()) throw IllegalArgumentException("Cloudflare Turnstile token is required.")

        val isSha256 = passTrimmed.length == 64 && passTrimmed.matches(Regex("^[0-9a-fA-F]+$"))
        val hashedPassword = if (isSha256) passTrimmed.lowercase() else hashPasswordToSha256(passTrimmed)

        val userInput = JSONObject().apply {
            put("username", userTrimmed)
            put("email", emailTrimmed)
            put("password", hashedPassword)
        }

        val variables = JSONObject().apply {
            put("user", userInput)
            put("recaptchCode", capTrimmed)
            put("captchaProvider", captchaProvider)
        }

        val requestBodyJson = JSONObject().apply {
            put("query", REGISTER_MUTATION.trim())
            put("variables", variables)
        }

        val mediaType = "application/json; charset=UTF-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val endpoints = config.endpoints

        val request = Request.Builder()
            .url(endpoints.apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", endpoints.authOrigin)
            .header("Referer", endpoints.authReferer)
            .header("x-build-id", config.buildId)
            .header("Host", endpoints.apiHostHeader)
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Priority", "u=4")
            .build()

        Log.d("AuthRepo", "Sending register mutation for username=$userTrimmed, email=$emailTrimmed")
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string() ?: ""
            Log.d("AuthRepo", "Register response code: ${response.code}")
            parseAuthResponse(raw, userTrimmed)
        }
    }

    suspend fun logout(accessToken: String?, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = JSONObject().apply {
                put("query", LOGOUT_MUTATION.trim())
            }
            val mediaType = "application/json; charset=UTF-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
            val endpoints = config.endpoints

            val reqBuilder = Request.Builder()
                .url(endpoints.apiBaseUrl)
                .post(body)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
                .header("Origin", endpoints.authOrigin)
                .header("Referer", endpoints.authReferer)
                .header("x-build-id", config.buildId)
                .header("Host", endpoints.apiHostHeader)

            if (!accessToken.isNullOrBlank()) {
                val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) accessToken else "Bearer $accessToken"
                reqBuilder.header("authorization", authHeader)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { true }
        } catch (_: Exception) {
            true
        }
    }

    suspend fun resendVerificationCode(
        email: String,
        recaptchCode: String,
        captchaProvider: String = MkissaConfigProvider.currentEndpoints.captchaProvider,
        accessToken: String? = null,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val emailTrimmed = email.trim()
        val capTrimmed = recaptchCode.trim()
        if (emailTrimmed.isEmpty()) throw IllegalArgumentException("Email is required.")
        if (capTrimmed.isEmpty()) throw IllegalArgumentException("Cloudflare Turnstile token is required.")

        val variables = JSONObject().apply {
            put("email", emailTrimmed)
            put("recaptchCode", capTrimmed)
            put("captchaProvider", captchaProvider)
        }

        val requestBodyJson = JSONObject().apply {
            put("query", RESEND_VERIFICATION_MUTATION.trim())
            put("variables", variables)
        }

        val mediaType = "application/json; charset=UTF-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val endpoints = config.endpoints

        val reqBuilder = Request.Builder()
            .url(endpoints.apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", endpoints.authOrigin)
            .header("Referer", endpoints.authReferer)
            .header("x-build-id", config.buildId)
            .header("Host", endpoints.apiHostHeader)
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Priority", "u=1, i")

        val token = accessToken ?: TokenManager.getAccessToken(context)
        if (!token.isNullOrBlank()) {
            val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            reqBuilder.header("authorization", authHeader)
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            val raw = response.body?.string() ?: ""
            val json = JSONObject(raw)
            if (json.has("errors")) {
                val errors = json.optJSONArray("errors")
                val errMsg = errors?.optJSONObject(0)?.optString("message") ?: "Failed to resend code"
                throw IllegalStateException(errMsg)
            }
            val resend = json.optJSONObject("data")?.optJSONObject("resendVerificationLink")
            val success = resend?.optBoolean("success", false) ?: false
            if (!success) {
                val errorMsg = resend?.optString("error")?.takeIf { it.isNotBlank() } ?: "Failed to resend verification code"
                throw IllegalStateException(errorMsg)
            }
            true
        }
    }

    suspend fun verifyEmailCode(
        email: String,
        code: String,
        accessToken: String? = null,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val emailTrimmed = email.trim()
        val codeTrimmed = code.trim()
        if (emailTrimmed.isEmpty()) throw IllegalArgumentException("Email is required.")
        if (codeTrimmed.isEmpty()) throw IllegalArgumentException("Verification code is required.")

        val variables = JSONObject().apply {
            put("email", emailTrimmed)
            put("code", codeTrimmed)
        }

        val requestBodyJson = JSONObject().apply {
            put("query", VERIFY_EMAIL_MUTATION.trim())
            put("variables", variables)
        }

        val mediaType = "application/json; charset=UTF-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val config = if (context != null) MkissaConfigProvider.getConfig(context) else MkissaConfigProvider.currentConfig
        val endpoints = config.endpoints

        val reqBuilder = Request.Builder()
            .url(endpoints.apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", endpoints.authOrigin)
            .header("Referer", endpoints.authReferer)
            .header("x-build-id", config.buildId)
            .header("Host", endpoints.apiHostHeader)
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Priority", "u=1, i")

        val token = accessToken ?: TokenManager.getAccessToken(context)
        if (!token.isNullOrBlank()) {
            val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            reqBuilder.header("authorization", authHeader)
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            val raw = response.body?.string() ?: ""
            val json = JSONObject(raw)
            if (json.has("errors")) {
                val errors = json.optJSONArray("errors")
                val errMsg = errors?.optJSONObject(0)?.optString("message") ?: "Verification failed"
                throw IllegalStateException(errMsg)
            }
            val verified = json.optJSONObject("data")?.optBoolean("verifyEmailCode", false) ?: false
            if (!verified) {
                throw IllegalStateException("Invalid verification code")
            }
            if (context != null) {
                TokenManager.setEmailVerified(context, true)
            }
            true
        }
    }

    private fun parseAuthResponse(rawResponse: String, inputIdentifier: String): AuthResult {
        val trimmed = rawResponse.trim()
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.startsWith("<?xml", ignoreCase = true)
        ) {
            val titleMatch = Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE).find(trimmed)
            val title = titleMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
            throw IllegalStateException("Server returned HTML ($title). Cloudflare verification failed or expired. Please retry.")
        }

        val decoded = JSONObject(trimmed)
        if (decoded.has("errors")) {
            val errors = decoded.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                val firstError = errors.optJSONObject(0)
                val errMsg = firstError?.optString("message") ?: "Authentication error"
                throw IllegalStateException(errMsg)
            }
        }

        val data = decoded.optJSONObject("data") ?: throw IllegalStateException("Response missing 'data' object.")
        val authObj = data.optJSONObject("authenticate")
            ?: data.optJSONObject("createUser")?.optJSONObject("loginResult")
            ?: throw IllegalStateException("Authentication / registration failed.")

        val sessionId = authObj.optString("sessionId", "")
        val tokens = authObj.optJSONObject("tokens") ?: throw IllegalStateException("Response missing 'tokens' object.")
        val accessToken = tokens.optString("accessToken", "")
        val refreshToken = tokens.optString("refreshToken", "")

        if (accessToken.isEmpty() || refreshToken.isEmpty()) {
            throw IllegalStateException("Failed to retrieve access/refresh tokens from server.")
        }

        val user = authObj.optJSONObject("user")
        val username = user?.optString("username")?.ifBlank { inputIdentifier } ?: inputIdentifier
        val displayName = user?.optString("displayName")?.takeIf { it.isNotBlank() }
        val picture = user?.optString("picture")?.takeIf { it.isNotBlank() }
        val userId = user?.optString("_id")?.takeIf { it.isNotBlank() }

        var email: String? = null
        var isEmailVerified = false
        val emails = user?.optJSONArray("emails")
        if (emails != null && emails.length() > 0) {
            val firstEmail = emails.optJSONObject(0)
            email = firstEmail?.optString("address")?.takeIf { it.isNotBlank() }
            isEmailVerified = firstEmail?.optBoolean("verified", false) ?: false
        }

        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            sessionId = sessionId,
            username = username,
            displayName = displayName,
            picture = picture,
            userId = userId,
            email = email,
            isEmailVerified = isEmailVerified,
            rawResponseJson = trimmed
        )
    }
}
