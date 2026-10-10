package com.ghostreborn.akira.features.player.services

import android.net.Uri
import android.util.Log
import com.ghostreborn.akira.features.player.models.StreamSource
import org.json.JSONArray
import org.json.JSONObject

object StreamParser {
    private const val TAG = "StreamParser"

    val knownIframeHosts = listOf(
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

    fun parseStreams(decryptedJson: String): List<StreamSource> {
        val trimmed = decryptedJson.trim()
        val streams = mutableListOf<StreamSource>()

        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        streams.addAll(parseStreamObject(item))
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val epObj = root.optJSONObject("episode")
                val array = epObj?.optJSONArray("sourceUrls")
                    ?: root.optJSONArray("sourceUrls")
                    ?: root.optJSONArray("sources")
                    ?: root.optJSONArray("links")
                    ?: root.optJSONArray("data")

                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i)
                        if (item != null) {
                            streams.addAll(parseStreamObject(item))
                        }
                    }
                } else {
                    streams.addAll(parseStreamObject(root))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding JSON: ${e.message}")
            streams.addAll(extractUrlsFallback(trimmed))
        }

        if (streams.isEmpty()) {
            streams.addAll(extractUrlsFallback(trimmed))
        }

        val seenUrls = mutableSetOf<String>()
        val unique = mutableListOf<StreamSource>()
        for (s in streams) {
            if (!seenUrls.contains(s.url)) {
                seenUrls.add(s.url)
                unique.add(s)
            }
        }

        unique.sortWith { a, b ->
            if (a.isDirect != b.isDirect) {
                if (a.isDirect) -1 else 1
            } else {
                b.priority.compareTo(a.priority)
            }
        }

        Log.d(TAG, "Parsed ${unique.size} streams (${unique.count { it.isDirect }} direct playable)")
        return unique
    }

    private fun normalizeUrl(urlStr: String?): String? {
        if (urlStr == null) return null
        var u = urlStr.replace(Regex("""\\/"""), "/").trim()
        if (u.startsWith("//")) {
            u = "https:$u"
        }
        return u
    }

    private fun checkIsDirect(url: String, itemType: String?, itemExt: String, name: String): Boolean {
        val lower = url.lowercase()
        val uri = try { Uri.parse(url) } catch (_: Exception) { null }
        val host = uri?.host?.lowercase() ?: ""

        for (iframeHost in knownIframeHosts) {
            if (host.contains(iframeHost)) {
                if (itemType != "player" && !lower.endsWith(".mp4") && !lower.endsWith(".m3u8")) {
                    return false
                }
            }
        }

        if (itemType == "player") return true
        if (itemExt == "mp4" || itemExt == "m3u8") return true
        if (lower.contains(".mp4") || lower.contains(".m3u8")) return true
        if (itemType == "iframe" || itemType == "embed") return false
        if (name.lowercase().contains("yt-mp4") || name.lowercase().contains("direct")) return true

        return false
    }

    private fun parseStreamObject(obj: JSONObject): List<StreamSource> {
        val result = mutableListOf<StreamSource>()

        val downloadsObj = obj.optJSONObject("downloads")
        val downloadUrlRaw = downloadsObj?.optString("downloadUrl")
        val downloadName = downloadsObj?.optString("sourceName") ?: obj.optString("sourceName")

        val rawUrlCandidate = when {
            obj.optString("sourceUrl").isNotBlank() -> obj.optString("sourceUrl")
            downloadsObj?.optString("downloadUrl")?.isNotBlank() == true -> downloadsObj.optString("downloadUrl")
            obj.optString("url").isNotBlank() -> obj.optString("url")
            obj.optString("link").isNotBlank() -> obj.optString("link")
            else -> null
        }?.trim()

        val name = when {
            obj.optString("sourceName").isNotBlank() -> obj.optString("sourceName")
            obj.optString("name").isNotBlank() -> obj.optString("name")
            else -> "Server"
        }

        val rawPriority = obj.optDouble("priority", 0.0)
        val type = obj.optString("type").takeIf { it.isNotBlank() }
        val ext = (if (obj.has("fileExtenstion")) obj.optString("fileExtenstion") else obj.optString("fileExtension")).trim().lowercase()

        val rawUrl = normalizeUrl(rawUrlCandidate)
        val downloadUrl = normalizeUrl(downloadUrlRaw)

        if (rawUrl != null && (rawUrl.startsWith("http://") || rawUrl.startsWith("https://"))) {
            val isHls = rawUrl.lowercase().contains(".m3u8") || ext == "m3u8"
            val isDirect = checkIsDirect(rawUrl, type, ext, name)
            val effectivePriority = if (isDirect) rawPriority + 10.0 else rawPriority

            result.add(
                StreamSource(
                    sourceName = name,
                    url = rawUrl,
                    priority = effectivePriority,
                    isHls = isHls,
                    type = type,
                    fileExtension = ext.ifEmpty { if (isHls) "m3u8" else if (rawUrl.lowercase().contains(".mp4")) "mp4" else null },
                    isDirect = isDirect
                )
            )
        }

        if (downloadUrl != null && (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) && downloadUrl != rawUrl) {
            val isHls = downloadUrl.lowercase().contains(".m3u8")
            val isDirect = checkIsDirect(downloadUrl, "player", "mp4", downloadName ?: "Download")

            result.add(
                StreamSource(
                    sourceName = "${downloadName ?: "Download"} (Direct)",
                    url = downloadUrl,
                    priority = rawPriority + 5.0,
                    isHls = isHls,
                    type = "player",
                    fileExtension = if (isHls) "m3u8" else "mp4",
                    isDirect = isDirect
                )
            )
        }

        return result
    }

    private fun extractUrlsFallback(text: String): List<StreamSource> {
        val regex = Regex("""https?://[^\s"<>\x27]+""")
        val matches = regex.findAll(text)
        val result = mutableListOf<StreamSource>()
        var index = 1
        for (m in matches) {
            val url = m.value.replace(Regex("""\\/"""), "/")
            val isHls = url.lowercase().contains(".m3u8")
            val isMp4 = url.lowercase().contains(".mp4")
            val isDirect = isHls || isMp4
            result.add(
                StreamSource(
                    sourceName = "Source $index",
                    url = url,
                    priority = 0.0,
                    isHls = isHls,
                    fileExtension = if (isHls) "m3u8" else if (isMp4) "mp4" else null,
                    isDirect = isDirect
                )
            )
            index++
        }
        return result
    }
}
