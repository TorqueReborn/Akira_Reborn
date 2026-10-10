package com.ghostreborn.akira.utils

object ImageUtils {
    const val BASE_HOST = "https://aln.youtube-anime.com"

    val imageHeaders = mapOf(
        "Referer" to "https://mkissa.to/",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0",
        "sec-ch-ua-platform" to "\"Windows\"",
        "sec-ch-ua" to "\"Chromium\";v=\"154\", \"Microsoft Edge\";v=\"154\", \"Not A(Brand\";v=\"99\"",
        "sec-ch-ua-mobile" to "?0"
    )

    fun resolveUrl(url: String?): String? {
        if (url == null) return null
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        return if (trimmed.startsWith("/")) {
            "$BASE_HOST$trimmed"
        } else {
            "$BASE_HOST/$trimmed"
        }
    }
}
