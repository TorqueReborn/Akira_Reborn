package com.ghostreborn.akira.core.config

import android.content.Context
import android.util.Base64
import android.util.Log
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
    val lane: String = "k7",
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
    val fetchedAtMs: Long
) {
    companion object {
        fun fallback(): MkissaClientConfig {
            return MkissaClientConfig(
                buildId = "179",
                lane = "k7",
                epochMs = 604800000L,
                graceMs = 86400000L,
                saltMul = 5,
                saltAdd = 39,
                fragMul = 251,
                fragAdd = 21,
                bootPrefix = "KoCGqjW:",
                joinDelimiter = "|",
                parts = listOf("buildId", "group", "lane", "epoch", "host"),
                fragments = listOf(
                    Base64.decode("o55/m8U/4FY=", Base64.DEFAULT),
                    Base64.decode("9lYtQjBCN5s=", Base64.DEFAULT),
                    Base64.decode("s3mBmnFuzkQ=", Base64.DEFAULT),
                    Base64.decode("H8tdZAar+nQ=", Base64.DEFAULT)
                ),
                fetchedAtMs = 0L
            )
        }

        fun fromJson(json: JSONObject): MkissaClientConfig {
            val fragsArray = json.optJSONArray("fragments")
            val frags = mutableListOf<ByteArray>()
            if (fragsArray != null) {
                for (i in 0 until fragsArray.length()) {
                    frags.add(Base64.decode(fragsArray.getString(i), Base64.DEFAULT))
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

            return MkissaClientConfig(
                buildId = json.optString("buildId", "179"),
                lane = json.optString("lane", "k7"),
                epochMs = json.optLong("epochMs", 604800000L),
                graceMs = json.optLong("graceMs", 86400000L),
                saltMul = json.optInt("saltMul", 5),
                saltAdd = json.optInt("saltAdd", 39),
                fragMul = json.optInt("fragMul", 251),
                fragAdd = json.optInt("fragAdd", 21),
                bootPrefix = json.optString("bootPrefix", "KoCGqjW:"),
                joinDelimiter = json.optString("joinDelimiter", "|"),
                parts = partsList,
                fragments = if (frags.isNotEmpty()) frags else fallback().fragments,
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
        fragments.forEach { fragsArr.put(Base64.encodeToString(it, Base64.NO_WRAP)) }
        json.put("fragments", fragsArr)

        json.put("fetchedAt", fetchedAtMs)
        return json
    }
}

object MkissaConfigProvider {
    private const val TAG = "MkissaConfig"
    private const val PREF_NAME = "mkissa_config_pref"
    private const val PREF_KEY = "mkissa_client_config_v1"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    const val PRIMARY_VERSION_URL = "https://mkissa.to/_app/version.json"
    const val SECONDARY_VERSION_URL = "https://cdn.mkissa.net/all/mk/_app/version.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var memoryConfig: MkissaClientConfig? = null
    private val mutex = Mutex()

    val currentBuildId: String
        get() = memoryConfig?.buildId ?: "179"

    val currentConfig: MkissaClientConfig
        get() = memoryConfig ?: MkissaClientConfig.fallback()

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

    private fun discoverFromNetwork(): MkissaClientConfig {
        var discoveredBuildId: String? = null

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
                        if (v.matches(Regex("^\\d+$"))) {
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

        return MkissaClientConfig(
            buildId = discoveredBuildId,
            lane = "k7",
            fragments = listOf(
                Base64.decode("o55/m8U/4FY=", Base64.DEFAULT),
                Base64.decode("9lYtQjBCN5s=", Base64.DEFAULT),
                Base64.decode("s3mBmnFuzkQ=", Base64.DEFAULT),
                Base64.decode("H8tdZAar+nQ=", Base64.DEFAULT)
            ),
            fetchedAtMs = System.currentTimeMillis()
        )
    }
}
