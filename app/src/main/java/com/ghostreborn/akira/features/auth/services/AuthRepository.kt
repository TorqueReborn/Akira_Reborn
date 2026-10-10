package com.ghostreborn.akira.features.auth.services

import android.util.Log
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.features.auth.models.AuthResult
import kotlinx.coroutines.Dispatchers
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
    const val API_URL = "https://api.mkissa.net/api"

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

    private const val LOGOUT_MUTATION = """
mutation {
  logout
}
"""

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun hashPasswordToSha256(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun authenticate(
        usernameOrEmail: String,
        rawPassword: String,
        recaptchCode: String,
        captchaProvider: String = "turnstile"
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

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://youtu-chan.com")
            .header("Referer", "https://youtu-chan.com/")
            .header("x-build-id", MkissaConfigProvider.currentBuildId)
            .header("Host", "api.mkissa.net")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Priority", "u=4")
            .build()

        Log.d("AuthRepo", "Sending authenticate mutation with id=$idTrimmed, buildId=${MkissaConfigProvider.currentBuildId}")
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string() ?: ""
            Log.d("AuthRepo", "Auth response code: ${response.code}, body: $raw")
            parseAuthenticateResponse(raw, idTrimmed)
        }
    }

    suspend fun logout(accessToken: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = JSONObject().apply {
                put("query", LOGOUT_MUTATION.trim())
            }
            val mediaType = "application/json; charset=UTF-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val reqBuilder = Request.Builder()
                .url(API_URL)
                .post(body)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
                .header("Origin", "https://youtu-chan.com")
                .header("Referer", "https://youtu-chan.com/")
                .header("x-build-id", MkissaConfigProvider.currentBuildId)
                .header("Host", "api.mkissa.net")

            if (!accessToken.isNullOrBlank()) {
                val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) accessToken else "Bearer $accessToken"
                reqBuilder.header("authorization", authHeader)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { true }
        } catch (_: Exception) {
            true
        }
    }

    private fun parseAuthenticateResponse(rawResponse: String, inputIdentifier: String): AuthResult {
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
        val authenticate = data.optJSONObject("authenticate") ?: throw IllegalStateException("Invalid Credentials")

        val sessionId = authenticate.optString("sessionId", "")
        val tokens = authenticate.optJSONObject("tokens") ?: throw IllegalStateException("Response missing 'tokens' object.")
        val accessToken = tokens.optString("accessToken", "")
        val refreshToken = tokens.optString("refreshToken", "")

        if (accessToken.isEmpty() || refreshToken.isEmpty()) {
            throw IllegalStateException("Failed to retrieve access/refresh tokens from server.")
        }

        val user = authenticate.optJSONObject("user")
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
