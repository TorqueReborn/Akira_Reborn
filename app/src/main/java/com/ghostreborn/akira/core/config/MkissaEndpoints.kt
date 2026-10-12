package com.ghostreborn.akira.core.config

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/**
 * Validated endpoint configuration profiles for Mkissa services and third-party media hosts.
 */
data class MkissaEndpoints(
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,
    val bootstrapBaseUrl: String = DEFAULT_BOOTSTRAP_BASE_URL,
    val appOrigin: String = DEFAULT_APP_ORIGIN,
    val appReferer: String = DEFAULT_APP_REFERER,
    val authOrigin: String = DEFAULT_AUTH_ORIGIN,
    val authReferer: String = DEFAULT_AUTH_REFERER,
    val apiHostHeader: String = DEFAULT_API_HOST_HEADER,
    val imageBaseUrl: String = DEFAULT_IMAGE_BASE_URL,
    val captchaProvider: String = DEFAULT_CAPTCHA_PROVIDER,
    val captchaUrl: String = DEFAULT_CAPTCHA_URL,
    val supportedStreamingHosts: List<String> = DEFAULT_STREAMING_HOSTS
) {
    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.mkissa.net/api"
        const val DEFAULT_BOOTSTRAP_BASE_URL = "https://api.mkissa.net/client-crypto/v1/bootstrap"
        const val DEFAULT_APP_ORIGIN = "https://mkissa.to"
        const val DEFAULT_APP_REFERER = "https://mkissa.to/"
        const val DEFAULT_AUTH_ORIGIN = "https://youtu-chan.com"
        const val DEFAULT_AUTH_REFERER = "https://youtu-chan.com/"
        const val DEFAULT_API_HOST_HEADER = "api.mkissa.net"
        const val DEFAULT_IMAGE_BASE_URL = "https://aln.youtube-anime.com"
        const val DEFAULT_CAPTCHA_PROVIDER = "turnstile"
        const val DEFAULT_CAPTCHA_URL = "https://api.allanime.day/captcha/turnstile"

        // Unified list of verified streaming hosts
        val DEFAULT_STREAMING_HOSTS: List<String> = listOf(
            "filemoon",
            "ok.ru",
            "odnoklassniki",
            "vidguard",
            "mp4upload.com",
            "streamwish",
            "doodstream",
            "dood.",
            "voe.sx",
            "streamtape",
            "mixdrop"
        )

        fun default(): MkissaEndpoints = MkissaEndpoints()

        /**
         * Validates that a candidate URL is a safe HTTPS URL with a valid host.
         */
        fun isValidHttpsUrl(candidate: String?): Boolean {
            if (candidate.isNullOrBlank()) return false
            return try {
                val uri = URI(candidate.trim())
                uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }

        fun fromJson(json: JSONObject?): MkissaEndpoints {
            if (json == null) return default()
            val def = default()

            val apiBase = json.optString("apiBaseUrl", def.apiBaseUrl).takeIf { isValidHttpsUrl(it) } ?: def.apiBaseUrl
            val bootBase = json.optString("bootstrapBaseUrl", def.bootstrapBaseUrl).takeIf { isValidHttpsUrl(it) } ?: def.bootstrapBaseUrl
            val appOrig = json.optString("appOrigin", def.appOrigin).takeIf { isValidHttpsUrl(it) } ?: def.appOrigin
            val appRef = json.optString("appReferer", def.appReferer).takeIf { isValidHttpsUrl(it) } ?: def.appReferer
            val authOrig = json.optString("authOrigin", def.authOrigin).takeIf { isValidHttpsUrl(it) } ?: def.authOrigin
            val authRef = json.optString("authReferer", def.authReferer).takeIf { isValidHttpsUrl(it) } ?: def.authReferer
            val apiHost = json.optString("apiHostHeader", def.apiHostHeader).takeIf { it.isNotBlank() } ?: def.apiHostHeader
            val imgBase = json.optString("imageBaseUrl", def.imageBaseUrl).takeIf { isValidHttpsUrl(it) } ?: def.imageBaseUrl
            val capProv = json.optString("captchaProvider", def.captchaProvider).takeIf { it.isNotBlank() } ?: def.captchaProvider
            val capUrl = json.optString("captchaUrl", def.captchaUrl).takeIf { isValidHttpsUrl(it) } ?: def.captchaUrl

            val hostsList = mutableListOf<String>()
            val hostsArr = json.optJSONArray("supportedStreamingHosts")
            if (hostsArr != null) {
                for (i in 0 until hostsArr.length()) {
                    val h = hostsArr.optString(i)?.trim()
                    if (!h.isNullOrBlank()) hostsList.add(h)
                }
            }

            return MkissaEndpoints(
                apiBaseUrl = apiBase,
                bootstrapBaseUrl = bootBase,
                appOrigin = appOrig,
                appReferer = appRef,
                authOrigin = authOrig,
                authReferer = authRef,
                apiHostHeader = apiHost,
                imageBaseUrl = imgBase,
                captchaProvider = capProv,
                captchaUrl = capUrl,
                supportedStreamingHosts = if (hostsList.isNotEmpty()) hostsList else def.supportedStreamingHosts
            )
        }
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("apiBaseUrl", apiBaseUrl)
        json.put("bootstrapBaseUrl", bootstrapBaseUrl)
        json.put("appOrigin", appOrigin)
        json.put("appReferer", appReferer)
        json.put("authOrigin", authOrigin)
        json.put("authReferer", authReferer)
        json.put("apiHostHeader", apiHostHeader)
        json.put("imageBaseUrl", imageBaseUrl)
        json.put("captchaProvider", captchaProvider)
        json.put("captchaUrl", captchaUrl)

        val arr = JSONArray()
        supportedStreamingHosts.forEach { arr.put(it) }
        json.put("supportedStreamingHosts", arr)
        return json
    }
}
