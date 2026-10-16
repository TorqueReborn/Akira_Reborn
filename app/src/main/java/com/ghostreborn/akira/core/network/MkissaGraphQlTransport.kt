package com.ghostreborn.akira.core.network

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Shared transport for Mkissa persisted-query calls. Normal requests use GET; a request that
 * explicitly receives NEED_CAPTCHA asks the user to complete Turnstile and retries once as POST.
 */
object MkissaGraphQlTransport {
    private val jsonMediaType = "application/json; charset=UTF-8".toMediaType()

    suspend fun executePersistedQuery(
        client: OkHttpClient,
        apiBaseUrl: String,
        variables: JSONObject,
        extensions: JSONObject,
        headers: Map<String, String>,
        authToken: String? = null
    ): String {
        val first = try {
            executeGet(client, apiBaseUrl, variables, extensions, headers, authToken)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!authToken.isNullOrBlank() && (msg.contains("HTTP 401") || msg.contains("HTTP 403"))) {
                // If the access token was revoked/rejected by the backend, retry once unauthenticated
                android.util.Log.w("Transport", "API rejected auth token ($msg). Retrying unauthenticated...")
                executeGet(client, apiBaseUrl, variables, extensions, headers, authToken = null)
            } else {
                throw e
            }
        }
        if (!requiresCaptcha(first)) return first

        val solution = CaptchaChallengeManager.awaitSolution()
        val retryExtensions = JSONObject(extensions.toString()).apply {
            put("captcha", JSONObject().apply {
                put("token", solution.token)
                put("provider", solution.provider)
            })
        }

        // Persisted queries can be sent in a POST body too. Keep the exact variables, hash,
        // content-lane and crypto extensions used for the original request.
        val retry = try {
            executePost(client, apiBaseUrl, variables, retryExtensions, headers, authToken)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!authToken.isNullOrBlank() && (msg.contains("HTTP 401") || msg.contains("HTTP 403"))) {
                executePost(client, apiBaseUrl, variables, retryExtensions, headers, authToken = null)
            } else {
                throw e
            }
        }
        if (requiresCaptcha(retry)) {
            throw IllegalStateException("The security check was rejected. Please try again to get a fresh verification token.")
        }
        return retry
    }

    suspend fun executeRawQuery(
        client: OkHttpClient,
        apiBaseUrl: String,
        query: String,
        variables: JSONObject,
        headers: Map<String, String>,
        authToken: String? = null,
        extensions: JSONObject? = null
    ): String {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
            if (extensions != null) {
                put("extensions", extensions)
            }
        }
        val body = payload.toString().toRequestBody(jsonMediaType)
        val builder = Request.Builder()
            .url(apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
        applyHeaders(builder, headers, authToken)
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful && !requiresCaptcha(raw)) {
                throw IllegalStateException("Mkissa API query failed with HTTP ${response.code}: ${raw.take(300)}")
            }
            return raw
        }
    }

    suspend fun executeMultipartUpload(
        client: OkHttpClient,
        apiBaseUrl: String,
        query: String,
        variables: JSONObject,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String = "application/xml",
        headers: Map<String, String>,
        authToken: String? = null
    ): String {
        val operationsObj = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject(variables.toString()).apply {
                put("file", JSONObject.NULL)
            })
        }
        val mapObj = JSONObject().apply {
            put("0", org.json.JSONArray().put("variables.file"))
        }

        val filePartMediaType = mimeType.toMediaType()
        val fileRequestBody = fileBytes.toRequestBody(filePartMediaType)

        val multipartBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("operations", operationsObj.toString())
            .addFormDataPart("map", mapObj.toString())
            .addFormDataPart("0", fileName, fileRequestBody)
            .build()

        val builder = Request.Builder()
            .url(apiBaseUrl)
            .post(multipartBody)

        applyHeaders(builder, headers, authToken)
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful && !requiresCaptcha(raw)) {
                throw IllegalStateException("Upload failed with HTTP ${response.code}: ${raw.take(300)}")
            }
            return raw
        }
    }



    private fun executeGet(
        client: OkHttpClient,
        apiBaseUrl: String,
        variables: JSONObject,
        extensions: JSONObject,
        headers: Map<String, String>,
        authToken: String?
    ): String {
        val url = "$apiBaseUrl?variables=${Uri.encode(variables.toString())}&extensions=${Uri.encode(extensions.toString())}"
        val builder = Request.Builder().url(url).get()
        applyHeaders(builder, headers, authToken)
        client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful && !requiresCaptcha(body)) {
                throw IllegalStateException("Mkissa API error HTTP ${response.code}: ${body.take(300)}")
            }
            return body
        }
    }

    private fun executePost(
        client: OkHttpClient,
        apiBaseUrl: String,
        variables: JSONObject,
        extensions: JSONObject,
        headers: Map<String, String>,
        authToken: String?
    ): String {
        val payload = JSONObject().apply {
            put("variables", variables)
            put("extensions", extensions)
        }
        val body = payload.toString().toRequestBody(jsonMediaType)
        val builder = Request.Builder()
            .url(apiBaseUrl)
            .post(body)
            .header("Content-Type", "application/json; charset=UTF-8")
        applyHeaders(builder, headers, authToken)
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful && !requiresCaptcha(raw)) {
                throw IllegalStateException("Mkissa API retry failed with HTTP ${response.code}: ${raw.take(300)}")
            }
            return raw
        }
    }

    private fun applyHeaders(builder: Request.Builder, headers: Map<String, String>, authToken: String?) {
        headers.forEach { (name, value) -> builder.header(name, value) }
        if (!authToken.isNullOrBlank()) {
            val value = if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken"
            builder.header("authorization", value)
        }
    }

    fun requiresCaptcha(rawResponse: String): Boolean {
        if (rawResponse.contains("NEED_CAPTCHA", ignoreCase = true)) return true
        return try {
            val errors = JSONObject(rawResponse).optJSONArray("errors") ?: return false
            (0 until errors.length()).any { index ->
                val error = errors.optJSONObject(index) ?: return@any false
                val extensions = error.optJSONObject("extensions")
                val exception = extensions?.optJSONObject("exception")
                error.optString("message").contains("NEED_CAPTCHA", ignoreCase = true) ||
                    extensions?.optString("code").equals("NEED_CAPTCHA", ignoreCase = true) ||
                    exception?.optString("code").equals("NEED_CAPTCHA", ignoreCase = true)
            }
        } catch (_: Exception) {
            false
        }
    }
}
