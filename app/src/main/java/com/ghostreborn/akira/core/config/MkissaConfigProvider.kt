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
import java.util.regex.Pattern

data class MkissaClientConfig(
    val buildId: String,
    val lane: String = OperationRegistry.LANE_EPISODE,
    val epochMs: Long = 604800000L,
    val graceMs: Long = 86400000L,
    val saltMul: Int = 12,
    val saltAdd: Int = 227,
    val fragMul: Int = 32,
    val fragAdd: Int = 217,
    val bootPrefix: String = "NmmeXF:",
    val joinDelimiter: String = "+",
    val parts: List<String> = listOf("lane", "buildId", "host", "group", "epoch"),
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
        // Fallback fragments validated against working reference build 180
        val DEFAULT_FRAGMENTS: List<ByteArray> = listOf(
            SafeBase64.decode("AFEIqKvfcWk="),
            SafeBase64.decode("SPqwGgteqB4="),
            SafeBase64.decode("s/WpUzSxn8I="),
            SafeBase64.decode("R7bCnc9cO5A=")
        )

        fun fallback(): MkissaClientConfig {
            return MkissaClientConfig(
                buildId = "180",
                lane = OperationRegistry.LANE_EPISODE,
                epochMs = 604800000L,
                graceMs = 86400000L,
                saltMul = 12,
                saltAdd = 227,
                fragMul = 32,
                fragAdd = 217,
                bootPrefix = "NmmeXF:",
                joinDelimiter = "+",
                parts = listOf("lane", "buildId", "host", "group", "epoch"),
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
            val validBuildId = if (rawBuildId.isNotBlank() && rawBuildId.matches(Regex("""^\d+$"""))) rawBuildId else "180"

            val fallbackCfg = fallback()

            // Strict consistency check: If fragments or parts are missing or invalid, use consistent fallback defaults
            val effectiveFragments = if (frags.size == 4) frags else fallbackCfg.fragments
            val effectiveParts = if (partsList.isNotEmpty()) partsList else fallbackCfg.parts
            val effectiveJoin = json.optString("joinDelimiter", fallbackCfg.joinDelimiter)
            val effectiveBootPrefix = json.optString("bootPrefix", fallbackCfg.bootPrefix)

            // Strict build ID consistency: If fallback fragments are substituted because the stored/parsed fragments
            // were missing or corrupted, do not allow an arbitrary build ID to be combined with fallback parameters.
            val finalBuildId = if (frags.size == 4 && partsList.isNotEmpty()) validBuildId else fallbackCfg.buildId

            return MkissaClientConfig(
                buildId = finalBuildId,
                lane = json.optString("lane", OperationRegistry.LANE_EPISODE),
                epochMs = json.optLong("epochMs", fallbackCfg.epochMs),
                graceMs = json.optLong("graceMs", fallbackCfg.graceMs),
                saltMul = json.optInt("saltMul", fallbackCfg.saltMul),
                saltAdd = json.optInt("saltAdd", fallbackCfg.saltAdd),
                fragMul = json.optInt("fragMul", fallbackCfg.fragMul),
                fragAdd = json.optInt("fragAdd", fallbackCfg.fragAdd),
                bootPrefix = effectiveBootPrefix,
                joinDelimiter = effectiveJoin,
                parts = effectiveParts,
                fragments = effectiveFragments,
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
    private const val PREF_KEY = "mkissa_client_config_v4"
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
        get() = memoryConfig?.buildId ?: "180"

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

    @Volatile
    private var lastKnownEtag: String? = null

    /**
     * Executes authoritative dynamic discovery of build ID, bundle metadata, and cryptographic parameters.
     */
    fun discoverFromNetwork(): MkissaClientConfig {
        var discoveredBuildId: String? = null

        // Step 1: Version endpoint discovery with conditional revalidation (ETag / If-None-Match)
        val urls = listOf(PRIMARY_VERSION_URL, SECONDARY_VERSION_URL)
        for (url in urls) {
            try {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")

                val currentEtag = lastKnownEtag
                if (!currentEtag.isNullOrBlank() && memoryConfig != null) {
                    reqBuilder.header("If-None-Match", currentEtag)
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.code == 304 && memoryConfig != null) {
                        Log.d(TAG, "Version endpoint returned HTTP 304 Not Modified; preserving current config")
                        return memoryConfig!!.copy(fetchedAtMs = System.currentTimeMillis())
                    }
                    if (response.isSuccessful) {
                        val etagHeader = response.header("ETag")
                        if (!etagHeader.isNullOrBlank()) {
                            lastKnownEtag = etagHeader
                        }
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

        // Step 2: Attempt bundle metadata inspection from frontend HTML entry point
        val discoveredHashes = mutableMapOf<String, String>()
        var extractedParams: ExtractedCryptoParams? = null
        try {
            extractedParams = inspectFrontendBundle(discoveredHashes)
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

        // Only adopt extracted params if complete and consistent (same deployment)
        return if (extractedParams != null && extractedParams.fragments.size == 4) {
            MkissaClientConfig(
                buildId = discoveredBuildId,
                lane = fallback.lane,
                epochMs = extractedParams.epochMs,
                graceMs = extractedParams.graceMs,
                saltMul = extractedParams.saltMul,
                saltAdd = extractedParams.saltAdd,
                fragMul = extractedParams.fragMul,
                fragAdd = extractedParams.fragAdd,
                bootPrefix = extractedParams.bootPrefix,
                joinDelimiter = extractedParams.joinDelimiter,
                parts = extractedParams.parts,
                fragments = extractedParams.fragments,
                operationHashes = finalHashes,
                endpoints = fallback.endpoints,
                fetchedAtMs = System.currentTimeMillis()
            )
        } else if (discoveredBuildId == fallback.buildId) {
            // Safe: discovered build matches verified fallback release
            MkissaClientConfig(
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
        } else {
            // Strict version boundary: Never mix a newer unknown build ID with stale cryptographic parameters from an older build!
            throw IllegalStateException(
                "Discovered new build ID '$discoveredBuildId' but frontend bundle parsing could not extract " +
                "its matching cryptographic parameters. Refusing to mix incompatible build configurations."
            )
        }
    }

    /**
     * Inspects the frontend index HTML to locate the core application script bundle and extract
     * query hashes and cryptographic parameters.
     */
    fun inspectFrontendBundle(outHashes: MutableMap<String, String>): ExtractedCryptoParams? {
        val req = Request.Builder()
            .url(FRONTEND_INDEX_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        val html = httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() ?: "" else ""
        }
        if (html.isBlank()) return null

        val scriptsToInspect = mutableListOf<String>()

        // 1. Script tags: src="..."
        val scriptSrcMatches = Regex("""src="([^"]*?\.js)"""").findAll(html)
        for (m in scriptSrcMatches) {
            scriptsToInspect.add(m.groupValues[1])
        }

        // 2. Dynamic module imports in HTML: import("...")
        val importMatches = Regex("""import\("([^"]*?\.js)"\)""").findAll(html)
        for (m in importMatches) {
            scriptsToInspect.add(m.groupValues[1])
        }

        var resultParams: ExtractedCryptoParams? = null

        for (scriptPath in scriptsToInspect) {
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

                if (jsContent.isBlank()) continue

                // Check for crypto parameters
                if (resultParams == null && jsContent.contains("saltMul:")) {
                    resultParams = MkissaBundleParser.parseCryptoParams(jsContent)
                }

                // Check for GraphQL persisted query hashes
                if (jsContent.contains("persistedQuery") || jsContent.contains("sha256Hash")) {
                    parseHashesFromBundle(jsContent, outHashes)
                }

                // If app entry script, look for additional chunk dependencies: e.g. chunks/BQqtsmMs.js
                if (fullScriptUrl.contains("app.") || fullScriptUrl.contains("entry/")) {
                    val chunkMatches = Regex(""""([^"]*?chunks/[^"]*?\.js)"""").findAll(jsContent)
                    for (cm in chunkMatches) {
                        val relChunk = cm.groupValues[1].replace("../", "")
                        val chunkUrl = "$CDN_BASE_URL/all/mk/_app/immutable/$relChunk"
                        try {
                            val cReq = Request.Builder()
                                .url(chunkUrl)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .build()
                            val chunkJs = httpClient.newCall(cReq).execute().use { r ->
                                if (r.isSuccessful) r.body?.string() ?: "" else ""
                            }
                            if (resultParams == null && chunkJs.contains("saltMul:")) {
                                resultParams = MkissaBundleParser.parseCryptoParams(chunkJs)
                            }
                            if (chunkJs.contains("persistedQuery") || chunkJs.contains("sha256Hash")) {
                                parseHashesFromBundle(chunkJs, outHashes)
                            }
                            if (resultParams != null && outHashes.isNotEmpty()) break
                        } catch (_: Exception) {}
                    }
                }

                if (resultParams != null && outHashes.isNotEmpty()) break
            } catch (_: Exception) {}
        }

        return resultParams
    }

    /**
     * Parses operation query hashes from JS bundle text without breaking on unknown tokens.
     */
    fun parseHashesFromBundle(jsContent: String, outHashes: MutableMap<String, String>) {
        val pqRegex = Regex("""persistedQuery\s*:\s*\{\s*version\s*:\s*1\s*,\s*sha256Hash\s*:\s*["']([a-f0-9]{64})["']""", RegexOption.IGNORE_CASE)
        val matches = pqRegex.findAll(jsContent)
        for (m in matches) {
            val hash = m.groupValues[1].lowercase()
            if (OperationRegistry.isValidSha256Hash(hash)) {
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
