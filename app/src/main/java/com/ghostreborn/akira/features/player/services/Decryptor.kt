package com.ghostreborn.akira.features.player.services

import android.content.Context
import android.util.Base64
import android.util.Log
import com.ghostreborn.akira.core.config.MkissaClientConfig
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.core.config.OperationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class CryptoBootstrap(
    val epoch: Long,
    val epochMs: Long,
    val graceMs: Long,
    val switchAt: Long,
    val partB: String,
    val k: String,
    val buildId: String
) {
    companion object {
        fun fromJson(
            json: JSONObject,
            defaultLane: String = OperationRegistry.LANE_EPISODE,
            buildId: String,
            currentTimeMs: Long? = null,
            config: MkissaClientConfig? = null
        ): CryptoBootstrap {
            val effectiveConfig = config ?: MkissaConfigProvider.currentConfig
            val now = currentTimeMs ?: System.currentTimeMillis()
            val epoch = json.optLong("epoch", -1L)
            if (epoch == -1L) throw IllegalStateException("Missing 'epoch' in bootstrap response.")

            val epochMs = if (json.has("epochMs")) json.getLong("epochMs") else effectiveConfig.epochMs
            val graceMs = if (json.has("graceMs")) json.getLong("graceMs") else effectiveConfig.graceMs
            val switchAt = if (json.has("switchAt")) json.getLong("switchAt") else (now + epochMs)
            val partB = json.optString("partB", "")
            if (partB.isBlank()) throw IllegalStateException("Missing or blank 'partB' in bootstrap response.")

            // Validate partB decodes to at least 32 bytes
            val decodedPartB = com.ghostreborn.akira.core.crypto.SafeBase64.decode(partB.trim())
            if (decodedPartB.size < 32) {
                throw IllegalStateException("Decoded bootstrap partB has invalid length: ${decodedPartB.size} bytes (minimum 32 required).")
            }

            val k = json.optString("k", defaultLane)

            return CryptoBootstrap(
                epoch = epoch,
                epochMs = epochMs,
                graceMs = graceMs,
                switchAt = switchAt,
                partB = partB,
                k = k,
                buildId = buildId
            )
        }
    }
}

object Decryptor {
    private const val TAG = "Decryptor"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Multi-lane, buildId-aware bootstrap cache: Key is "buildId:lane"
    private val bootstrapCache = ConcurrentHashMap<String, CryptoBootstrap>()

    val defaultBuildId: String
        get() = MkissaConfigProvider.currentBuildId

    val defaultLane: String
        get() = MkissaConfigProvider.currentConfig.lane

    fun invalidateBootstrapCache() {
        Log.d(TAG, "Invalidating bootstrap cache across all lanes")
        bootstrapCache.clear()
    }

    fun invalidateBootstrapCacheForLane(lane: String) {
        bootstrapCache.keys.filter { it.endsWith(":$lane") }.forEach { bootstrapCache.remove(it) }
    }

    fun deriveSeed(buildId: String, config: MkissaClientConfig? = null): ByteArray {
        require(buildId.isNotEmpty()) { "buildId cannot be empty" }
        val cfg = config ?: MkissaConfigProvider.currentConfig
        val seed = ByteArray(32)
        for (i in 0 until 32) {
            val charCode = buildId[i % buildId.length].code
            seed[i] = ((charCode xor ((i * cfg.saltMul + cfg.saltAdd) and 0xFF)) and 0xFF).toByte()
        }
        return seed
    }

    fun deriveMask(buildId: String, config: MkissaClientConfig? = null): ByteArray {
        val cfg = config ?: MkissaConfigProvider.currentConfig
        val seed = deriveSeed(buildId, cfg)
        val mask = ByteArray(32)
        val frags = cfg.fragments
        require(frags.size >= 4) { "Configuration fragments must contain at least 4 items" }
        for (lane in 0 until 4) {
            val fragment = frags[lane]
            require(fragment.size >= 8) { "Fragment $lane must be at least 8 bytes" }
            val offset = lane * 8
            for (pos in 0 until 8) {
                val mix = (lane * cfg.fragMul + pos * cfg.fragAdd) and 0xFF
                val fragmentByte = fragment[pos].toInt() and 0xFF
                val seedByte = seed[offset + pos].toInt() and 0xFF
                mask[offset + pos] = ((fragmentByte xor seedByte xor mix) and 0xFF).toByte()
            }
        }
        return mask
    }

    fun calculateCurrentEpoch(currentTimeMs: Long? = null, epochMs: Long? = null): Long {
        val effectiveEpochMs = epochMs ?: MkissaConfigProvider.currentConfig.epochMs
        require(effectiveEpochMs > 0) { "epochMs must be greater than 0" }
        val now = currentTimeMs ?: System.currentTimeMillis()
        return now / effectiveEpochMs
    }

    fun calculateTransitionEpoch(currentTimeMs: Long? = null, epochMs: Long? = null, graceMs: Long? = null): Long {
        val cfg = MkissaConfigProvider.currentConfig
        val effectiveEpochMs = epochMs ?: cfg.epochMs
        val effectiveGraceMs = graceMs ?: cfg.graceMs
        require(effectiveEpochMs > 0) { "epochMs must be greater than 0" }
        val now = currentTimeMs ?: System.currentTimeMillis()
        val epoch = calculateCurrentEpoch(now, effectiveEpochMs)
        return if (now - epoch * effectiveEpochMs < effectiveGraceMs && epoch > 0) {
            epoch - 1
        } else {
            epoch
        }
    }

    fun resolveKeyGroup(refererHost: String): String {
        val host = refererHost.trim().lowercase().replaceFirst("www.", "")
        if (host == "mkissa.to" || host.contains("mkissa")) return "mkissa"
        if (host.startsWith("192.168.")) return "192.168."
        if (host == "localhost" || host == "127.0.0.1") return "mirror"
        return host
    }

    fun generateXAaBoot(
        buildId: String,
        lane: String,
        epoch: Long? = null,
        refererHost: String = "mkissa.to",
        config: MkissaClientConfig? = null
    ): String {
        require(buildId.isNotBlank()) { "buildId cannot be blank for x-aa-boot" }
        require(lane.isNotBlank()) { "lane cannot be blank for x-aa-boot" }
        val cfg = config ?: MkissaConfigProvider.currentConfig
        val mask = deriveMask(buildId, cfg)
        val keyGroup = resolveKeyGroup(refererHost)
        val currentEpoch = epoch ?: calculateCurrentEpoch()

        // Stage 1 HMAC-SHA256
        val hmac1 = Mac.getInstance("HmacSHA256")
        hmac1.init(SecretKeySpec(mask, "HmacSHA256"))
        val stage1Message = "${cfg.bootPrefix}$buildId".toByteArray(StandardCharsets.UTF_8)
        val intermediateKey = hmac1.doFinal(stage1Message)

        // Stage 2 HMAC-SHA256
        val partValues = mapOf(
            "buildId" to buildId,
            "group" to keyGroup,
            "lane" to lane,
            "epoch" to currentEpoch.toString(),
            "host" to refererHost
        )
        val context = cfg.parts.joinToString(cfg.joinDelimiter) { partValues[it] ?: "" }

        val hmac2 = Mac.getInstance("HmacSHA256")
        hmac2.init(SecretKeySpec(intermediateKey, "HmacSHA256"))
        val stage2Message = context.toByteArray(StandardCharsets.UTF_8)
        val finalDigest = hmac2.doFinal(stage2Message)

        return finalDigest.joinToString("") { "%02x".format(it) }
    }

    fun deriveKey(buildId: String, partBBase64: String, config: MkissaClientConfig? = null): ByteArray {
        val trimmed = partBBase64.trim()
        val partB = com.ghostreborn.akira.core.crypto.SafeBase64.decode(trimmed)
        if (partB.size < 32) {
            throw IllegalArgumentException("partB decoded length (${partB.size}) is less than 32 bytes.")
        }
        val mask = deriveMask(buildId, config)
        val keyBytes = ByteArray(32)
        for (i in 0 until 32) {
            keyBytes[i] = ((partB[i].toInt() xor mask[i].toInt()) and 0xFF).toByte()
        }
        return keyBytes
    }

    suspend fun getBootstrap(
        context: Context,
        lane: String? = null,
        buildId: String? = null,
        authToken: String? = null,
        forceRefresh: Boolean = false,
        config: MkissaClientConfig? = null
    ): CryptoBootstrap = withContext(Dispatchers.IO) {
        val initialConfig = config ?: MkissaConfigProvider.getConfig(context, forceRefresh = forceRefresh)
        val targetLane = lane ?: initialConfig.lane
        val targetBuildId = buildId ?: initialConfig.buildId

        try {
            fetchBootstrapInternal(context, targetLane, targetBuildId, authToken, forceRefresh, initialConfig)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val isBootTokenError = msg.contains("invalid_boot_token", ignoreCase = true)
            val isAuthOrForbidden = msg.contains("403") || msg.contains("401")
            if (isBootTokenError || isAuthOrForbidden) {
                Log.w(TAG, "Bootstrap failed ($msg). Invalidating configuration and retrying once with fresh snapshot...")
                // Invalidate memory and persistent config and lane caches
                MkissaConfigProvider.invalidate(context)
                invalidateBootstrapCacheForLane(targetLane)

                // Fetch fresh configuration directly from network
                val refreshedConfig = MkissaConfigProvider.getConfig(context, forceRefresh = true)
                val refreshedLane = lane ?: refreshedConfig.lane
                // Crucial: Retry must use refreshedConfig.buildId to avoid mixing an old rejected buildId with a new config
                val refreshedBuildId = refreshedConfig.buildId
                fetchBootstrapInternal(context, refreshedLane, refreshedBuildId, authToken, forceRefresh = true, effectiveConfig = refreshedConfig)
            } else {
                throw e
            }
        }
    }

    private fun fetchBootstrapInternal(
        context: Context,
        effectiveLane: String,
        effectiveBuildId: String,
        authToken: String?,
        forceRefresh: Boolean,
        effectiveConfig: MkissaClientConfig
    ): CryptoBootstrap {
        val cacheKey = "$effectiveBuildId:$effectiveLane"
        val now = System.currentTimeMillis()
        val cached = bootstrapCache[cacheKey]
        if (!forceRefresh && cached != null && cached.k == effectiveLane && cached.buildId == effectiveBuildId && now < cached.switchAt) {
            return cached
        }

        val epochCandidates = listOf(
            calculateTransitionEpoch(now, effectiveConfig.epochMs, effectiveConfig.graceMs),
            calculateCurrentEpoch(now, effectiveConfig.epochMs)
        )

        val bootstrapBaseUrl = effectiveConfig.endpoints.bootstrapBaseUrl
        var lastError: Exception? = null

        for (candidateEpoch in epochCandidates) {
            try {
                val xAaBoot = generateXAaBoot(
                    buildId = effectiveBuildId,
                    lane = effectiveLane,
                    epoch = candidateEpoch,
                    config = effectiveConfig
                )
                val url = "$bootstrapBaseUrl?buildId=$effectiveBuildId&k=$effectiveLane"

                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Origin", effectiveConfig.endpoints.appOrigin)
                    .header("Referer", effectiveConfig.endpoints.appReferer)
                    .header("x-build-id", effectiveBuildId)
                    .header("x-aa-boot", xAaBoot)

                if (!authToken.isNullOrBlank()) {
                    val authHeader = if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken"
                    reqBuilder.header("Authorization", authHeader)
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Bootstrap HTTP ${response.code}: $body")
                    }

                    val json = JSONObject(body)
                    val bootstrap = CryptoBootstrap.fromJson(
                        json = json,
                        defaultLane = effectiveLane,
                        buildId = effectiveBuildId,
                        currentTimeMs = now,
                        config = effectiveConfig
                    )

                    // Verify returned lane matches requested lane
                    if (bootstrap.k != effectiveLane) {
                        throw IllegalStateException("Bootstrap lane mismatch: expected $effectiveLane but server returned ${bootstrap.k}")
                    }

                    bootstrapCache[cacheKey] = bootstrap
                    Log.d(TAG, "Bootstrap received: epoch=${bootstrap.epoch}, lane=${bootstrap.k}, switchAt=${bootstrap.switchAt}")
                    return bootstrap
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("Bootstrap fetch failed for all candidate epochs.")
    }

    fun generateAaReq(
        queryHash: String,
        bootstrap: CryptoBootstrap,
        buildId: String? = null,
        currentTimeMs: Long? = null,
        config: MkissaClientConfig? = null
    ): String {
        require(OperationRegistry.isValidSha256Hash(queryHash)) {
            "Invalid queryHash format: must be 64 lowercase hex characters"
        }
        val effectiveBuildId = buildId ?: bootstrap.buildId
        val now = currentTimeMs ?: System.currentTimeMillis()
        val ts = (now / 300000L) * 300000L
        val epoch = bootstrap.epoch

        // IV: first 12 bytes of SHA-256(epoch + ":" + buildId + ":" + queryHash + ":" + ts + ":" + lane)
        val ivSource = "$epoch:$effectiveBuildId:$queryHash:$ts:${bootstrap.k}"
        val md = MessageDigest.getInstance("SHA-256")
        val fullHash = md.digest(ivSource.toByteArray(StandardCharsets.UTF_8))
        val iv = fullHash.copyOfRange(0, 12)

        // Exact JSON payload ordering
        val jsonPayload = JSONObject()
        jsonPayload.put("v", 1)
        jsonPayload.put("ts", ts)
        jsonPayload.put("epoch", epoch)
        jsonPayload.put("buildId", effectiveBuildId)
        jsonPayload.put("qh", queryHash)
        jsonPayload.put("k", bootstrap.k)
        val plaintextBytes = jsonPayload.toString().toByteArray(StandardCharsets.UTF_8)

        val keyBytes = deriveKey(effectiveBuildId, bootstrap.partB, config)

        // AES-256-GCM encryption with BouncyCastle
        val cipher = GCMBlockCipher(AESEngine())
        val params = AEADParameters(KeyParameter(keyBytes), 128, iv, ByteArray(0))
        cipher.init(true, params)

        val encryptedWithTag = ByteArray(cipher.getOutputSize(plaintextBytes.size))
        val len = cipher.processBytes(plaintextBytes, 0, plaintextBytes.size, encryptedWithTag, 0)
        cipher.doFinal(encryptedWithTag, len)

        // byte 0 = 0x01, bytes 1..12 = IV, remaining = ciphertext + tag
        val output = ByteArray(1 + 12 + encryptedWithTag.size)
        output[0] = 0x01
        System.arraycopy(iv, 0, output, 1, 12)
        System.arraycopy(encryptedWithTag, 0, output, 13, encryptedWithTag.size)

        return com.ghostreborn.akira.core.crypto.SafeBase64.encodeToString(output)
    }

    fun decrypt(
        payload: String,
        partBBase64: String,
        buildId: String? = null,
        config: MkissaClientConfig? = null
    ): String {
        val effectiveBuildId = buildId ?: MkissaConfigProvider.currentBuildId
        val trimmed = payload.trim()
        require(trimmed.isNotEmpty()) { "Payload cannot be empty" }

        val raw = com.ghostreborn.akira.core.crypto.SafeBase64.decode(trimmed)
        if (raw.isEmpty()) throw IllegalStateException("Decoded payload is empty")
        if (raw[0].toInt() != 1) throw IllegalStateException("Unsupported payload version (${raw[0]}). Only version 1 is supported.")
        if (raw.size < 1 + 12 + 16) throw IllegalStateException("Payload buffer too short (${raw.size} bytes).")

        val iv = raw.copyOfRange(1, 13)
        val encryptedWithTag = raw.copyOfRange(13, raw.size)

        val keyBytes = deriveKey(effectiveBuildId, partBBase64, config)

        val cipher = GCMBlockCipher(AESEngine())
        val params = AEADParameters(KeyParameter(keyBytes), 128, iv, ByteArray(0))
        cipher.init(false, params)

        val plaintextBytes = ByteArray(cipher.getOutputSize(encryptedWithTag.size))
        val len = cipher.processBytes(encryptedWithTag, 0, encryptedWithTag.size, plaintextBytes, 0)
        val finalLen = len + cipher.doFinal(plaintextBytes, len)

        return String(plaintextBytes, 0, finalLen, StandardCharsets.UTF_8)
    }
}
