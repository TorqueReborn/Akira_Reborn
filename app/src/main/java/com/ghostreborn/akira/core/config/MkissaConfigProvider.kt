package com.ghostreborn.akira.core.config

import android.content.Context
import android.util.Log
import com.ghostreborn.akira.core.crypto.SafeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MkissaClientConfig(
    val buildId: String,
    val lane: String = OperationRegistry.LANE_EPISODE,
    val epochMs: Long = 604800000L,
    val graceMs: Long = 86400000L,
    val saltMul: Int = 5,
    val saltAdd: Int = 39,
    val fragMul: Int = 251,
    val fragAdd: Int = 21,
    val bootPrefix: String = "KoCGqjW:",
    val joinDelimiter: String = "|",
    val parts: List<String> = listOf("buildId", "group", "lane", "epoch", "host"),
    val fragments: List<ByteArray>,
    val operationHashes: Map<String, String> = mapOf(
        OperationRegistry.OP_BROWSE to OperationRegistry.DEFAULT_BROWSE_HASH,
        OperationRegistry.OP_TOP_RANKED to OperationRegistry.DEFAULT_TOP_RANKED_HASH,
        OperationRegistry.OP_DETAIL to OperationRegistry.DEFAULT_DETAIL_HASH,
        OperationRegistry.OP_EPISODE to OperationRegistry.DEFAULT_EPISODE_HASH
    ),
    val endpoints: MkissaEndpoints = MkissaEndpoints.default(),
    val fetchedAtMs: Long
) {
    fun getOperationHash(operationName: String): String {
        val hash = operationHashes[operationName]
        if (OperationRegistry.isValidSha256Hash(hash)) {
            return hash!!
        }
        return when (operationName) {
            OperationRegistry.OP_BROWSE -> OperationRegistry.DEFAULT_BROWSE_HASH
            OperationRegistry.OP_TOP_RANKED -> OperationRegistry.DEFAULT_TOP_RANKED_HASH
            OperationRegistry.OP_DETAIL -> OperationRegistry.DEFAULT_DETAIL_HASH
            OperationRegistry.OP_EPISODE -> OperationRegistry.DEFAULT_EPISODE_HASH
            else -> throw IllegalArgumentException("Unknown GraphQL operation: $operationName")
        }
    }

    companion object {
        val DEFAULT_FRAGMENTS: List<ByteArray> = listOf(
            SafeBase64.decode("o55/m8U/4FY="),
            SafeBase64.decode("9lYtQjBCN5s="),
            SafeBase64.decode("s3mBmnFuzkQ="),
            SafeBase64.decode("H8tdZAar+nQ=")
        )

        fun fallback(): MkissaClientConfig {
            return MkissaClientConfig(
                buildId = "179",
                lane = OperationRegistry.LANE_EPISODE,
                epochMs = 604800000L,
                graceMs = 86400000L,
                saltMul = 5,
                saltAdd = 39,
                fragMul = 251,
                fragAdd = 21,
                bootPrefix = "KoCGqjW:",
                joinDelimiter = "|",
                parts = listOf("buildId", "group", "lane", "epoch", "host"),
                fragments = DEFAULT_FRAGMENTS,
                operationHashes = mapOf(
                    OperationRegistry.OP_BROWSE to OperationRegistry.DEFAULT_BROWSE_HASH,
                    OperationRegistry.OP_TOP_RANKED to OperationRegistry.DEFAULT_TOP_RANKED_HASH,
                    OperationRegistry.OP_DETAIL to OperationRegistry.DEFAULT_DETAIL_HASH,
                    OperationRegistry.OP_EPISODE to OperationRegistry.DEFAULT_EPISODE_HASH
                ),
                endpoints = MkissaEndpoints.default(),
                fetchedAtMs = 0L
            )
        }

        fun fromJson(json: JSONObject): MkissaClientConfig {
            val fragsArray = json.optJSONArray("fragments")
            val frags = mutableListOf<ByteArray>()
            if (fragsArray != null) {
                for (i in 0 until fragsArray.length()) {
                    val decoded = SafeBase64.decode(fragsArray.getString(i))
                    if (decoded.size == 8) {
                        frags.add(decoded)
                    }
                }
            }

            val partsArray = json.optJSONArray("parts")
            val partsList = mutableListOf<String>()
            if (partsArray != null) {
                for (i in 0 until partsArray.length()) {
                    partsList.add(partsArray.getString(i))
                }
            } else {
                partsList.addAll(listOf("buildId", "group", "lane", "epoch", "host"))
            }

            val hashes = mutableMapOf<String, String>()
            val hashesObj = json.optJSONObject("operationHashes")
            if (hashesObj != null) {
                val keys = hashesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = hashesObj.optString(k)
                    if (OperationRegistry.isValidSha256Hash(v)) {
                        hashes[k] = v
                    }
                }
            }
            if (!hashes.containsKey(OperationRegistry.OP_BROWSE)) hashes[OperationRegistry.OP_BROWSE] = OperationRegistry.DEFAULT_BROWSE_HASH
            if (!hashes.containsKey(OperationRegistry.OP_TOP_RANKED)) hashes[OperationRegistry.OP_TOP_RANKED] = OperationRegistry.DEFAULT_TOP_RANKED_HASH
            if (!hashes.containsKey(OperationRegistry.OP_DETAIL)) hashes[OperationRegistry.OP_DETAIL] = OperationRegistry.DEFAULT_DETAIL_HASH
            if (!hashes.containsKey(OperationRegistry.OP_EPISODE)) hashes[OperationRegistry.OP_EPISODE] = OperationRegistry.DEFAULT_EPISODE_HASH

            val endpointsObj = json.optJSONObject("endpoints")
            val endpoints = MkissaEndpoints.fromJson(endpointsObj)

            val rawBuildId = json.optString("buildId", "")
            val validBuildId = if (rawBuildId.isNotBlank() && rawBuildId.matches(Regex("""^\d+$"""))) rawBuildId else "179"

            return MkissaClientConfig(
                buildId = validBuildId,
                lane = json.optString("lane", OperationRegistry.LANE_EPISODE),
                epochMs = json.optLong("epochMs", 604800000L),
                graceMs = json.optLong("graceMs", 86400000L),
                saltMul = json.optInt("saltMul", 5),
                saltAdd = json.optInt("saltAdd", 39),
                fragMul = json.optInt("fragMul", 251),
                fragAdd = json.optInt("fragAdd", 21),
                bootPrefix = json.optString("bootPrefix", "KoCGqjW:"),
                joinDelimiter = json.optString("joinDelimiter", "|"),
                parts = partsList,
                fragments = if (frags.size == 4) frags else DEFAULT_FRAGMENTS,
                operationHashes = hashes,
                endpoints = endpoints,
                fetchedAtMs = json.optLong("fetchedAt", 0L)
            )
        }
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("buildId", buildId)
        json.put("lane", lane)
        json.put("epochMs", epochMs)
        json.put("graceMs", graceMs)
        json.put("saltMul", saltMul)
        json.put("saltAdd", saltAdd)
        json.put("fragMul", fragMul)
        json.put("fragAdd", fragAdd)
        json.put("bootPrefix", bootPrefix)
        json.put("joinDelimiter", joinDelimiter)

        val partsArr = JSONArray()
        parts.forEach { partsArr.put(it) }
        json.put("parts", partsArr)

        val fragsArr = JSONArray()
        fragments.forEach { fragsArr.put(SafeBase64.encodeToString(it)) }
        json.put("fragments", fragsArr)

        val hashesObj = JSONObject()
        operationHashes.forEach { (k, v) -> hashesObj.put(k, v) }
        json.put("operationHashes", hashesObj)

        json.put("endpoints", endpoints.toJson())
        json.put("fetchedAt", fetchedAtMs)
        return json
    }
}

object MkissaConfigProvider {
    private const val TAG = "MkissaConfig"
    private const val PREF_NAME = "mkissa_config_pref"
    private const val PREF_KEY = "mkissa_client_config_v3"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    const val PRIMARY_VERSION_URL = "https://mkissa.to/_app/version.json"
    const val SECONDARY_VERSION_URL = "https://cdn.mkissa.net/all/mk/_app/version.json"

    // Frontend HTML entry URL to discover immutable script bundles
    const val FRONTEND_INDEX_URL = "https://mkissa.to/"
    const val CDN_BASE_URL = "https://cdn.mkissa.net"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var memoryConfig: MkissaClientConfig? = null
    private val mutex = Mutex()

    val currentBuildId: String
        get() = memoryConfig?.buildId ?: "179"

    val currentConfig: MkissaClientConfig
        get() = memoryConfig ?: MkissaClientConfig.fallback()

    val currentEndpoints: MkissaEndpoints
        get() = currentConfig.endpoints

    suspend fun invalidate(context: Context) {
        Log.d(TAG, "Invalidating cached configuration")
        memoryConfig = null
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(PREF_KEY).apply()
        }
    }

    suspend fun getConfig(context: Context, forceRefresh: Boolean = false): MkissaClientConfig {
        val now = System.currentTimeMillis()
        if (!forceRefresh && memoryConfig != null) {
            if (now - memoryConfig!!.fetchedAtMs < CACHE_TTL_MS) {
                return memoryConfig!!
            }
        }

        return mutex.withLock {
            if (!forceRefresh && memoryConfig != null) {
                if (now - memoryConfig!!.fetchedAtMs < CACHE_TTL_MS) {
                    return@withLock memoryConfig!!
                }
            }

            loadOrDiscover(context, forceRefresh)
        }
    }

    private suspend fun loadOrDiscover(context: Context, forceRefresh: Boolean): MkissaClientConfig = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            try {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val raw = prefs.getString(PREF_KEY, null)
                if (!raw.isNullOrBlank()) {
                    val json = JSONObject(raw)
                    val cached = MkissaClientConfig.fromJson(json)
                    if (now - cached.fetchedAtMs < CACHE_TTL_MS) {
                        memoryConfig = cached
                        return@withContext cached
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Persistent cache read error: $e")
            }
        }

        try {
            val discovered = discoverFromNetwork()
            memoryConfig = discovered
            try {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(PREF_KEY, discovered.toJson().toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Persistent cache write error: $e")
            }
            return@withContext discovered
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed: ${e.message}. Falling back.")
            if (memoryConfig != null) {
                return@withContext memoryConfig!!
            }

            try {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val raw = prefs.getString(PREF_KEY, null)
                if (!raw.isNullOrBlank()) {
                    val cached = MkissaClientConfig.fromJson(JSONObject(raw))
                    memoryConfig = cached
                    return@withContext cached
                }
            } catch (_: Exception) {}

            val fallback = MkissaClientConfig.fallback()
            memoryConfig = fallback
            return@withContext fallback
        }
    }

    /**
     * Executes authoritative dynamic discovery of build ID, bundle metadata, and operation hashes.
     */
    fun discoverFromNetwork(): MkissaClientConfig {
        var discoveredBuildId: String? = null

        // Step 1: Version endpoint discovery
        val urls = listOf(PRIMARY_VERSION_URL, SECONDARY_VERSION_URL)
        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()?.trim() ?: ""
                        val json = JSONObject(body)
                        val v = json.optString("version")
                        if (v.matches(Regex("""^\d+$"""))) {
                            discoveredBuildId = v
                            Log.d(TAG, "Discovered build ID $v from $url")
                            return@use
                        }
                    }
                }
                if (discoveredBuildId != null) break
            } catch (e: Exception) {
                Log.e(TAG, "Fetch from $url failed: ${e.message}")
            }
        }

        if (discoveredBuildId == null) {
            throw IllegalStateException("Unable to discover Mkissa build ID from version endpoints.")
        }

        // Step 2: Attempt bundle metadata inspection if accessible
        val discoveredHashes = mutableMapOf<String, String>()
        try {
            inspectFrontendBundle(discoveredHashes)
        } catch (e: Exception) {
            Log.d(TAG, "Frontend bundle inspection skipped: ${e.message}")
        }

        val fallback = MkissaClientConfig.fallback()

        // Merge discovered hashes with validated fallbacks
        val finalHashes = fallback.operationHashes.toMutableMap()
        discoveredHashes.forEach { (k, v) ->
            if (OperationRegistry.isValidSha256Hash(v)) {
                finalHashes[k] = v
            }
        }

        return MkissaClientConfig(
            buildId = discoveredBuildId,
            lane = fallback.lane,
            epochMs = fallback.epochMs,
            graceMs = fallback.graceMs,
            saltMul = fallback.saltMul,
            saltAdd = fallback.saltAdd,
            fragMul = fallback.fragMul,
            fragAdd = fallback.fragAdd,
            bootPrefix = fallback.bootPrefix,
            joinDelimiter = fallback.joinDelimiter,
            parts = fallback.parts,
            fragments = fallback.fragments,
            operationHashes = finalHashes,
            endpoints = fallback.endpoints,
            fetchedAtMs = System.currentTimeMillis()
        )
    }

    /**
     * Inspects the frontend index HTML to locate the core application script bundle and extract
     * query hashes if the bundle format matches expectations.
     */
    private fun inspectFrontendBundle(outHashes: MutableMap<String, String>) {
        val req = Request.Builder()
            .url(FRONTEND_INDEX_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        val html = httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() ?: "" else ""
        }
        if (html.isBlank()) return

        // Look for app bundle script: e.g. /_app/immutable/entry/app.*.js or chunks
        val scriptMatch = Regex("""src="([^"]*?/_app/immutable/[^"]*?\.js)"""").findAll(html)
        for (m in scriptMatch) {
            val scriptPath = m.groupValues[1]
            val fullScriptUrl = if (scriptPath.startsWith("http://") || scriptPath.startsWith("https://")) {
                scriptPath
            } else if (scriptPath.startsWith("/")) {
                "$CDN_BASE_URL$scriptPath"
            } else {
                "$CDN_BASE_URL/$scriptPath"
            }

            try {
                val scriptReq = Request.Builder()
                    .url(fullScriptUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                val jsContent = httpClient.newCall(scriptReq).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() ?: "" else ""
                }

                if (jsContent.contains("persistedQuery") || jsContent.contains("sha256Hash")) {
                    parseHashesFromBundle(jsContent, outHashes)
                    if (outHashes.isNotEmpty()) break
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Parses operation query hashes from JS bundle text without breaking on unknown tokens.
     */
    fun parseHashesFromBundle(jsContent: String, outHashes: MutableMap<String, String>) {
        // Pattern: persistedQuery:{version:1,sha256Hash:"..."}
        val pqRegex = Regex("""persistedQuery\s*:\s*\{\s*version\s*:\s*1\s*,\s*sha256Hash\s*:\s*["']([a-f0-9]{64})["']""", RegexOption.IGNORE_CASE)
        val matches = pqRegex.findAll(jsContent)
        for (m in matches) {
            val hash = m.groupValues[1].lowercase()
            if (OperationRegistry.isValidSha256Hash(hash)) {
                // Determine which operation matches by inspecting preceding context
                val start = (m.range.first - 400).coerceAtLeast(0)
                val snippet = jsContent.substring(start, m.range.first)
                val lane = OperationRegistry.resolveLaneForQuery(snippet)
                if (lane == OperationRegistry.LANE_EPISODE) {
                    outHashes[OperationRegistry.OP_EPISODE] = hash
                }
            }
        }
    }
}
