package com.ghostreborn.akira

import com.ghostreborn.akira.core.crypto.SafeBase64
import com.ghostreborn.akira.core.config.MkissaClientConfig
import com.ghostreborn.akira.core.config.MkissaEndpoints
import com.ghostreborn.akira.core.config.OperationRegistry
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.player.services.CryptoBootstrap
import com.ghostreborn.akira.features.player.services.Decryptor
import com.ghostreborn.akira.features.player.services.StreamParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MkissaIntegrationUnitTest {

    @Test
    fun test1_validAndMalformedRemoteConfiguration() {
        val validJson = JSONObject().apply {
            put("buildId", "185")
            put("lane", "k7")
            put("epochMs", 604800000L)
            put("graceMs", 86400000L)
            put("saltMul", 5)
            put("saltAdd", 39)
            put("fragMul", 251)
            put("fragAdd", 21)
            put("bootPrefix", "KoCGqjW:")
            put("joinDelimiter", "|")
            val partsArr = JSONArray().apply {
                put("buildId")
                put("group")
                put("lane")
                put("epoch")
                put("host")
            }
            put("parts", partsArr)
            val fragsArr = JSONArray().apply {
                put(SafeBase64.encodeToString(ByteArray(8) { 1 }))
                put(SafeBase64.encodeToString(ByteArray(8) { 2 }))
                put(SafeBase64.encodeToString(ByteArray(8) { 3 }))
                put(SafeBase64.encodeToString(ByteArray(8) { 4 }))
            }
            put("fragments", fragsArr)
            put("operationHashes", JSONObject().apply {
                put("browse", "8b319a0fda488e4319f1b6d99093ba12802f3fc039572f39bcc02d5f9b9d9b02")
                put("episode", "670bbf38d0868f446e2346c1e956ca2c40c416e733ca248fd54e04f1c8b99145")
            })
            put("fetchedAt", 1000L)
        }

        val parsed = MkissaClientConfig.fromJson(validJson)
        assertEquals("185", parsed.buildId)
        assertEquals(4, parsed.fragments.size)
        assertEquals("8b319a0fda488e4319f1b6d99093ba12802f3fc039572f39bcc02d5f9b9d9b02", parsed.getOperationHash("browse"))

        // Malformed JSON (missing fragments, invalid non-numeric buildId)
        val malformedJson = JSONObject().apply {
            put("buildId", "not-a-number")
            put("fragments", JSONArray()) // empty fragments
        }
        val safeFallbackParsed = MkissaClientConfig.fromJson(malformedJson)
        // Must fallback safely
        assertEquals("179", safeFallbackParsed.buildId)
        assertEquals(4, safeFallbackParsed.fragments.size)
    }

    @Test
    fun test2_buildIdResolutionAndFallbackBehavior() {
        val fallback = MkissaClientConfig.fallback()
        assertEquals("179", fallback.buildId)
        assertEquals(OperationRegistry.LANE_EPISODE, fallback.lane)
        assertEquals(604800000L, fallback.epochMs)
        assertEquals(86400000L, fallback.graceMs)
        assertEquals(4, fallback.fragments.size)
        fallback.fragments.forEach { assertEquals(8, it.size) }
    }

    @Test
    fun test3_unknownOrMalformedGraphQLOperationMetadata() {
        val config = MkissaClientConfig.fallback()

        // Valid operations resolve their correct hashes
        assertEquals(OperationRegistry.DEFAULT_BROWSE_HASH, config.getOperationHash(OperationRegistry.OP_BROWSE))
        assertEquals(OperationRegistry.DEFAULT_TOP_RANKED_HASH, config.getOperationHash(OperationRegistry.OP_TOP_RANKED))
        assertEquals(OperationRegistry.DEFAULT_DETAIL_HASH, config.getOperationHash(OperationRegistry.OP_DETAIL))
        assertEquals(OperationRegistry.DEFAULT_EPISODE_HASH, config.getOperationHash(OperationRegistry.OP_EPISODE))

        // Hashes validation
        assertTrue(OperationRegistry.isValidSha256Hash(OperationRegistry.DEFAULT_EPISODE_HASH))
        assertFalse(OperationRegistry.isValidSha256Hash("short_hash"))
        assertFalse(OperationRegistry.isValidSha256Hash("xyz123_invalid_chars!"))
        assertFalse(OperationRegistry.isValidSha256Hash(null))

        // Unknown operation throws exception rather than substituting arbitrary hash
        try {
            config.getOperationHash("unknownOperation")
            fail("Expected IllegalArgumentException for unknown operation")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown GraphQL operation"))
        }
    }

    @Test
    fun test4_correctOperationToLaneSelection() {
        // Verified mappings from frontend bundle:
        // episode -> k7
        // chapterPages -> k9
        // music -> k2
        assertEquals(OperationRegistry.LANE_EPISODE, OperationRegistry.resolveLaneForQuery("query episode(\$id: String!) { episode { tobeparsed } }"))
        assertEquals(OperationRegistry.LANE_CHAPTER_PAGES, OperationRegistry.resolveLaneForQuery("query chapterPages(\$mangaId: String!) { chapterPages { tobeparsed } }"))
        assertEquals(OperationRegistry.LANE_MUSIC, OperationRegistry.resolveLaneForQuery("query music(\$id: String!) { music { tobeparsed } }"))

        assertEquals(OperationRegistry.LANE_EPISODE, OperationRegistry.resolveLaneForQuery(OperationRegistry.OP_EPISODE))
        assertEquals(OperationRegistry.LANE_CHAPTER_PAGES, OperationRegistry.resolveLaneForQuery(OperationRegistry.OP_CHAPTER_PAGES))
        assertEquals(OperationRegistry.LANE_MUSIC, OperationRegistry.resolveLaneForQuery(OperationRegistry.OP_MUSIC))

        // Unknown operation returns null rather than silently assigning k7
        assertNull(OperationRegistry.resolveLaneForQuery("query browseShows { shows { edges } }"))
        assertNull(OperationRegistry.resolveLaneForQuery("unknownQuery"))
    }

    @Test
    fun test5_bootstrapValidationAndCacheInvalidation() {
        val buildId = "180"
        val partBBytes = ByteArray(32) { (it + 5).toByte() }
        val partBBase64 = SafeBase64.encodeToString(partBBytes)

        val bootstrapJson = JSONObject().apply {
            put("epoch", 2800L)
            put("epochMs", 604800000L)
            put("graceMs", 86400000L)
            put("switchAt", System.currentTimeMillis() + 100000L)
            put("partB", partBBase64)
            put("k", "k7")
        }

        val bootstrap = CryptoBootstrap.fromJson(bootstrapJson, defaultLane = "k7", buildId = buildId)
        assertEquals(2800L, bootstrap.epoch)
        assertEquals("k7", bootstrap.k)
        assertEquals(buildId, bootstrap.buildId)

        // Invalid partB (too short) must be rejected
        val shortPartBJson = JSONObject().apply {
            put("epoch", 2800L)
            put("partB", SafeBase64.encodeToString(ByteArray(16)))
            put("k", "k7")
        }
        try {
            CryptoBootstrap.fromJson(shortPartBJson, defaultLane = "k7", buildId = buildId)
            fail("Expected exception for short partB")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("invalid length"))
        }

        // Test lane invalidation
        Decryptor.invalidateBootstrapCache()
        Decryptor.invalidateBootstrapCacheForLane("k7")
    }

    @Test
    fun test6_epochCalculationAndExpiry() {
        val epochMs = 604800000L
        val graceMs = 86400000L

        val t0 = 2800L * epochMs + 10000L // 10s into epoch 2800, within grace window
        val curEpoch = Decryptor.calculateCurrentEpoch(currentTimeMs = t0, epochMs = epochMs)
        assertEquals(2800L, curEpoch)

        val transEpoch = Decryptor.calculateTransitionEpoch(currentTimeMs = t0, epochMs = epochMs, graceMs = graceMs)
        assertEquals(2799L, transEpoch) // Should offer candidate 2799 in grace window

        val t1 = 2800L * epochMs + 2 * graceMs // past grace window
        val transEpochAfterGrace = Decryptor.calculateTransitionEpoch(currentTimeMs = t1, epochMs = epochMs, graceMs = graceMs)
        assertEquals(2800L, transEpochAfterGrace)
    }

    @Test
    fun test7_cryptoDerivationAndAaReqCompatibility() {
        val config = MkissaClientConfig.fallback()
        val buildId = "179"

        val seed = Decryptor.deriveSeed(buildId, config)
        assertEquals(32, seed.size)

        val mask = Decryptor.deriveMask(buildId, config)
        assertEquals(32, mask.size)

        val partBBytes = ByteArray(32) { (it * 3).toByte() }
        val partBBase64 = SafeBase64.encodeToString(partBBytes)

        val derivedKey = Decryptor.deriveKey(buildId, partBBase64, config)
        assertEquals(32, derivedKey.size)

        // Verify deriveKey matches bitwise XOR
        for (i in 0 until 32) {
            val expected = ((partBBytes[i].toInt() xor mask[i].toInt()) and 0xFF).toByte()
            assertEquals(expected, derivedKey[i])
        }

        val bootstrap = CryptoBootstrap(
            epoch = 2800L,
            epochMs = 604800000L,
            graceMs = 86400000L,
            switchAt = System.currentTimeMillis() + 100000L,
            partB = partBBase64,
            k = "k7",
            buildId = buildId
        )

        val queryHash = OperationRegistry.DEFAULT_EPISODE_HASH
        val aaReq = Decryptor.generateAaReq(
            queryHash = queryHash,
            bootstrap = bootstrap,
            buildId = buildId,
            currentTimeMs = 1700000000000L,
            config = config
        )

        assertNotNull(aaReq)
        val decodedAaReq = SafeBase64.decode(aaReq)
        // Format: version (1 byte, 0x01) + IV (12 bytes) + ciphertext + tag
        assertEquals(0x01.toByte(), decodedAaReq[0])
        assertTrue(decodedAaReq.size > 13 + 16)
    }

    @Test
    fun test8_roundTripEncryptionAndDecryption() {
        val config = MkissaClientConfig.fallback()
        val buildId = "180"

        val partBBytes = ByteArray(32) { (it xor 42).toByte() }
        val partBBase64 = SafeBase64.encodeToString(partBBytes)

        // Mock payload JSON
        val originalJson = """[{"sourceName":"Server 1","sourceUrl":"https://stream.example.com/master.m3u8","type":"player"}]"""

        // Encrypt with BouncyCastle exactly as Mkissa server responds (version 1 byte + 12-byte IV + GCM tag 16 bytes)
        val keyBytes = Decryptor.deriveKey(buildId, partBBase64, config)
        val iv = ByteArray(12) { (it + 1).toByte() }

        val cipher = org.bouncycastle.crypto.modes.GCMBlockCipher(org.bouncycastle.crypto.engines.AESEngine())
        val params = org.bouncycastle.crypto.params.AEADParameters(
            org.bouncycastle.crypto.params.KeyParameter(keyBytes),
            128,
            iv,
            ByteArray(0)
        )
        cipher.init(true, params)
        val plaintextBytes = originalJson.toByteArray(Charsets.UTF_8)
        val encryptedWithTag = ByteArray(cipher.getOutputSize(plaintextBytes.size))
        val len = cipher.processBytes(plaintextBytes, 0, plaintextBytes.size, encryptedWithTag, 0)
        cipher.doFinal(encryptedWithTag, len)

        val payloadBytes = ByteArray(1 + 12 + encryptedWithTag.size)
        payloadBytes[0] = 0x01
        System.arraycopy(iv, 0, payloadBytes, 1, 12)
        System.arraycopy(encryptedWithTag, 0, payloadBytes, 13, encryptedWithTag.size)
        val payloadBase64 = SafeBase64.encodeToString(payloadBytes)

        // Decrypt using Decryptor
        val decrypted = Decryptor.decrypt(
            payload = payloadBase64,
            partBBase64 = partBBase64,
            buildId = buildId,
            config = config
        )

        assertEquals(originalJson, decrypted)

        // Parse streams from decrypted json
        val streams = StreamParser.parseStreams(decrypted)
        assertEquals(1, streams.size)
        assertEquals("https://stream.example.com/master.m3u8", streams[0].url)
        assertEquals("player", streams[0].type)
    }

    @Test
    fun test9_endpointsValidationAndHeaderSafety() {
        val endpoints = MkissaEndpoints.default()
        assertTrue(MkissaEndpoints.isValidHttpsUrl(endpoints.apiBaseUrl))
        assertTrue(MkissaEndpoints.isValidHttpsUrl(endpoints.bootstrapBaseUrl))
        assertTrue(MkissaEndpoints.isValidHttpsUrl(endpoints.appOrigin))
        assertFalse(MkissaEndpoints.isValidHttpsUrl("http://insecure.api.net"))
        assertFalse(MkissaEndpoints.isValidHttpsUrl("not_a_url"))
        assertFalse(MkissaEndpoints.isValidHttpsUrl(""))

        assertEquals("api.mkissa.net", endpoints.apiHostHeader)
        assertEquals("https://youtu-chan.com", endpoints.authOrigin)
    }

    @Test
    fun test10_passwordHashingIntegrity() {
        // Auth password SHA-256 hash
        val pass = "mySecretPassword123"
        val hash = AuthRepository.hashPasswordToSha256(pass)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
        // Deterministic
        assertEquals(hash, AuthRepository.hashPasswordToSha256(pass))
    }

    @Test
    fun test11_computeQueryHashAndBundleExtraction() {
        // Test query hash computation with SHA-256
        val testQuery = "query episode(\${'$'}showId: String!) { episode(showId: \${'$'}showId) { id } }"
        val computedHash = OperationRegistry.computeQueryHash(testQuery)
        assertTrue(OperationRegistry.isValidSha256Hash(computedHash))

        // Deterministic hash matches standard SHA-256
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val expected = md.digest(testQuery.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        assertEquals(expected, computedHash)

        // Test bundle parsing
        val mockJs = "var someCode = 123;\n// query episode\nvar q = { query: \"query episode(\$id: String!) { episode { tobeparsed } }\", persistedQuery: { version: 1, sha256Hash: \"670bbf38d0868f446e2346c1e956ca2c40c416e733ca248fd54e04f1c8b99145\" } };"

        val parsedHashes = mutableMapOf<String, String>()
        com.ghostreborn.akira.core.config.MkissaConfigProvider.parseHashesFromBundle(mockJs, parsedHashes)
        assertEquals("670bbf38d0868f446e2346c1e956ca2c40c416e733ca248fd54e04f1c8b99145", parsedHashes[OperationRegistry.OP_EPISODE])
    }

    @Test
    fun test12_endpointsJsonSerializationAndCustomHosts() {
        val customEndpoints = MkissaEndpoints(
            apiBaseUrl = "https://custom.api.mkissa.net/api",
            bootstrapBaseUrl = "https://custom.api.mkissa.net/client-crypto/v1/bootstrap",
            appOrigin = "https://custom.mkissa.to",
            appReferer = "https://custom.mkissa.to/",
            supportedStreamingHosts = listOf("customhost.com", "vidprovider.io")
        )

        val json = customEndpoints.toJson()
        val restored = MkissaEndpoints.fromJson(json)

        assertEquals("https://custom.api.mkissa.net/api", restored.apiBaseUrl)
        assertEquals("https://custom.api.mkissa.net/client-crypto/v1/bootstrap", restored.bootstrapBaseUrl)
        assertEquals("https://custom.mkissa.to", restored.appOrigin)
        assertEquals("https://custom.mkissa.to/", restored.appReferer)
        assertEquals(2, restored.supportedStreamingHosts.size)
        assertTrue(restored.supportedStreamingHosts.contains("customhost.com"))
        assertTrue(restored.supportedStreamingHosts.contains("vidprovider.io"))

        // Malicious or insecure HTTP URLs should be rejected and fall back to defaults
        val badJson = JSONObject().apply {
            put("apiBaseUrl", "http://insecure-http.com/api")
            put("bootstrapBaseUrl", "ftp://evil.com/bootstrap")
        }
        val safeRestored = MkissaEndpoints.fromJson(badJson)
        assertEquals(MkissaEndpoints.DEFAULT_API_BASE_URL, safeRestored.apiBaseUrl)
        assertEquals(MkissaEndpoints.DEFAULT_BOOTSTRAP_BASE_URL, safeRestored.bootstrapBaseUrl)
    }

    @Test
    fun test13_streamParserUsesConfiguredStreamingHosts() {
        assertTrue(StreamParser.knownIframeHosts.contains("filemoon"))
        assertTrue(StreamParser.knownIframeHosts.contains("mp4upload.com"))
        assertTrue(StreamParser.knownIframeHosts.contains("streamwish"))
    }

}
