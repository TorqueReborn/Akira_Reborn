package com.ghostreborn.akira

import com.ghostreborn.akira.features.updater.models.AppRelease
import com.ghostreborn.akira.features.updater.models.ReleaseAsset
import com.ghostreborn.akira.features.updater.models.UpdateErrorType
import com.ghostreborn.akira.features.updater.services.ApkVerifier
import com.ghostreborn.akira.features.updater.services.ReleaseFetchResult
import com.ghostreborn.akira.features.updater.services.ReleaseParser
import com.ghostreborn.akira.features.updater.services.UpdateRepository
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class UpdaterUnitTest {

    // Helper to create an OkHttpClient with a custom mock response interceptor
    private fun createMockClient(
        statusCode: Int,
        body: String,
        headers: Map<String, String> = emptyMap(),
        throwIoException: Boolean = false
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                if (throwIoException) {
                    throw IOException("Simulated network connection failure")
                }
                val request = chain.request()
                val responseBuilder = Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Status $statusCode")
                    .body(body.toResponseBody("application/json".toMediaType()))

                headers.forEach { (k, v) ->
                    responseBuilder.header(k, v)
                }

                responseBuilder.build()
            })
            .build()
    }

    // 1. Release JSON parsing with complete fields
    @Test
    fun testParseReleaseSuccess() {
        val json = """
            {
              "id": 12345,
              "tag_name": "v1.2.0",
              "name": "Akira 1.2.0 Update",
              "body": "Bug fixes and improvements.\n\nversionCode: 25\nmin_supported_version_code: 10\nsha256: 2b252a6c9933e39a4c92f9b71fb850d3c317a437fba2a018c4bcf49411bb823e",
              "html_url": "https://github.com/TorqueReborn/Akira_Reborn/releases/tag/v1.2.0",
              "draft": false,
              "prerelease": false,
              "published_at": "2026-10-12T10:00:00Z",
              "assets": [
                {
                  "id": 991,
                  "name": "source_code.zip",
                  "size": 1024,
                  "content_type": "application/zip",
                  "browser_download_url": "https://github.com/TorqueReborn/Akira_Reborn/releases/download/v1.2.0/source.zip"
                },
                {
                  "id": 992,
                  "name": "Akira-release.apk",
                  "size": 45000000,
                  "content_type": "application/vnd.android.package-archive",
                  "browser_download_url": "https://github.com/TorqueReborn/Akira_Reborn/releases/download/v1.2.0/Akira-release.apk",
                  "digest": "sha256:2b252a6c9933e39a4c92f9b71fb850d3c317a437fba2a018c4bcf49411bb823e"
                }
              ]
            }
        """.trimIndent()

        val release = ReleaseParser.parseRelease(json)
        assertNotNull(release)
        assertEquals(12345L, release!!.id)
        assertEquals("v1.2.0", release.tagName)
        assertEquals("Akira 1.2.0 Update", release.name)
        assertFalse(release.isDraft)
        assertFalse(release.isPrerelease)
        assertEquals("Akira-release.apk", release.apkAsset.name)
        assertEquals(45000000L, release.apkAsset.size)
        assertEquals(25L, release.explicitVersionCode)
        assertEquals(10L, release.minSupportedVersionCode)
        assertEquals("2b252a6c9933e39a4c92f9b71fb850d3c317a437fba2a018c4bcf49411bb823e", release.sha256Checksum)
    }

    // 2. Ignores release without APK assets
    @Test
    fun testParseReleaseWithoutApkReturnsNull() {
        val json = """
            {
              "id": 54321,
              "tag_name": "v1.0.1",
              "name": "v1.0.1",
              "body": "No APK attached",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "id": 101,
                  "name": "release.zip",
                  "size": 500,
                  "browser_download_url": "https://github.com/TorqueReborn/Akira_Reborn/releases/download/v1.0.1/release.zip"
                }
              ]
            }
        """.trimIndent()

        val release = ReleaseParser.parseRelease(json)
        assertNull(release)
    }

    // 3. Selects the most appropriate APK when multiple APKs exist
    @Test
    fun testSelectApkAssetPrefersReleaseOverDebug() {
        val assets = listOf(
            ReleaseAsset(1, "source.zip", 100, null, "https://url1"),
            ReleaseAsset(2, "app-debug.apk", 200, null, "https://url2"),
            ReleaseAsset(3, "Akira-v2.0-release.apk", 300, null, "https://url3")
        )

        val selected = ReleaseParser.selectApkAsset(assets)
        assertNotNull(selected)
        assertEquals("Akira-v2.0-release.apk", selected!!.name)
    }

    // 4. Standalone manifest parsing
    @Test
    fun testParseManifestSuccess() {
        val manifestJson = """
            {
              "versionCode": 30,
              "versionName": "2.0.0",
              "minSupportedVersionCode": 15,
              "apkFileName": "Akira.apk",
              "sha256": "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
              "releaseNotes": "Major update!"
            }
        """.trimIndent()

        val manifest = ReleaseParser.parseManifest(manifestJson)
        assertNotNull(manifest)
        assertEquals(30L, manifest!!.versionCode)
        assertEquals("2.0.0", manifest.versionName)
        assertEquals(15L, manifest.minSupportedVersionCode)
        assertEquals("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", manifest.sha256)
    }

    // 5. VersionCode extraction patterns
    @Test
    fun testParseVersionCodeFromBody() {
        assertEquals(42L, ReleaseParser.parseVersionCodeFromBody("versionCode: 42"))
        assertEquals(99L, ReleaseParser.parseVersionCodeFromBody("Version-Code: 99"))
        assertEquals(101L, ReleaseParser.parseVersionCodeFromBody("version_code = 101"))
        assertNull(ReleaseParser.parseVersionCodeFromBody("no version code here"))
    }

    // 6. SHA-256 Checksum extraction patterns
    @Test
    fun testParseSha256FromBody() {
        val validHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(validHash, ReleaseParser.parseSha256FromBody("sha256: $validHash"))
        assertEquals(validHash, ReleaseParser.parseSha256FromBody("SHA-256 = $validHash"))
        assertEquals(validHash, ReleaseParser.parseSha256FromBody("checksum: $validHash"))
        assertNull(ReleaseParser.parseSha256FromBody("checksum: short"))
    }

    // 7. UpdateRepository HTTP 200 with ETag & Manifest
    @Test
    fun testFetchLatestReleaseHttp200() = kotlinx.coroutines.runBlocking {
        val releaseJson = """
            {
              "id": 9999,
              "tag_name": "v2.0.0",
              "name": "Akira 2.0",
              "body": "versionCode: 15\nsha256: 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
              "html_url": "https://github.com/TorqueReborn/Akira_Reborn/releases/tag/v2.0.0",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "id": 1,
                  "name": "Akira.apk",
                  "size": 35000000,
                  "browser_download_url": "https://github.com/TorqueReborn/Akira_Reborn/releases/download/v2.0.0/Akira.apk"
                }
              ]
            }
        """.trimIndent()

        val client = createMockClient(
            statusCode = 200,
            body = releaseJson,
            headers = mapOf("ETag" to "W/\"tag-12345\"")
        )

        val repo = UpdateRepository(
            httpClient = client,
            customBaseUrl = "https://mock.github.api/releases/latest"
        )

        val result = repo.fetchLatestRelease()
        assertTrue(result is ReleaseFetchResult.Success)
        val success = result as ReleaseFetchResult.Success
        assertEquals(9999L, success.release.id)
        assertEquals("W/\"tag-12345\"", success.eTag)
        assertEquals(15L, success.release.explicitVersionCode)
    }

    // 8. UpdateRepository HTTP 304 Not Modified
    @Test
    fun testFetchLatestReleaseHttp304NotModified() = kotlinx.coroutines.runBlocking {
        val cachedRelease = AppRelease(
            id = 555,
            tagName = "v1.5",
            name = "Akira 1.5",
            body = "",
            htmlUrl = "https://example.com",
            isDraft = false,
            isPrerelease = false,
            publishedAt = null,
            apkAsset = ReleaseAsset(1, "Akira.apk", 1000, null, "https://apk")
        )

        val client = createMockClient(
            statusCode = 304,
            body = "",
            headers = mapOf("ETag" to "\"tag-cached\"")
        )

        val repo = UpdateRepository(
            httpClient = client,
            customBaseUrl = "https://mock.github.api/releases/latest"
        )

        val result = repo.fetchLatestRelease(eTag = "\"tag-cached\"", cachedRelease = cachedRelease)
        assertTrue(result is ReleaseFetchResult.Success)
        val success = result as ReleaseFetchResult.Success
        assertTrue(success.isNotModified)
        assertEquals(555L, success.release.id)
    }

    // 9. UpdateRepository Rate Limiting Handling (HTTP 403 / 429)
    @Test
    fun testFetchLatestReleaseRateLimited() = kotlinx.coroutines.runBlocking {
        val resetTime = (System.currentTimeMillis() / 1000L) + 120
        val client = createMockClient(
            statusCode = 403,
            body = "{\"message\":\"API rate limit exceeded\"}",
            headers = mapOf(
                "X-RateLimit-Remaining" to "0",
                "X-RateLimit-Reset" to "$resetTime"
            )
        )

        val repo = UpdateRepository(
            httpClient = client,
            customBaseUrl = "https://mock.github.api/releases/latest"
        )

        val result = repo.fetchLatestRelease()
        assertTrue(result is ReleaseFetchResult.Failure)
        val failure = result as ReleaseFetchResult.Failure
        assertEquals(UpdateErrorType.RATE_LIMITED, failure.errorType)
        assertTrue((failure.retryAfterSeconds ?: 0) in 100..125)
    }

    // 10. UpdateRepository 404 Not Found
    @Test
    fun testFetchLatestReleaseNotFound() = kotlinx.coroutines.runBlocking {
        val client = createMockClient(
            statusCode = 404,
            body = "{\"message\":\"Not Found\"}"
        )

        val repo = UpdateRepository(
            httpClient = client,
            customBaseUrl = "https://mock.github.api/releases/latest"
        )

        val result = repo.fetchLatestRelease()
        assertTrue(result is ReleaseFetchResult.Failure)
        val failure = result as ReleaseFetchResult.Failure
        assertEquals(UpdateErrorType.NOT_FOUND, failure.errorType)
    }

    // 11. UpdateRepository Network Offline Handling
    @Test
    fun testFetchLatestReleaseNetworkOffline() = kotlinx.coroutines.runBlocking {
        val client = createMockClient(
            statusCode = 0,
            body = "",
            throwIoException = true
        )

        val repo = UpdateRepository(
            httpClient = client,
            customBaseUrl = "https://mock.github.api/releases/latest"
        )

        val result = repo.fetchLatestRelease()
        assertTrue(result is ReleaseFetchResult.Failure)
        val failure = result as ReleaseFetchResult.Failure
        assertEquals(UpdateErrorType.NETWORK_OFFLINE, failure.errorType)
    }

    // 12. SHA-256 Computation verification
    @Test
    fun testComputeSha256Checksum() {
        val testFile = File.createTempFile("sha_test", ".tmp").apply {
            writeText("Hello Akira Auto Updater!")
            deleteOnExit()
        }

        val calculated = ApkVerifier.computeSha256(testFile)

        // Compute expected standard hash
        val md = MessageDigest.getInstance("SHA-256")
        val expected = md.digest("Hello Akira Auto Updater!".toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertEquals(expected, calculated)
    }

    // 13. Draft / Prerelease rejection
    @Test
    fun testRejectsDraftAndPrerelease() = kotlinx.coroutines.runBlocking {
        val draftJson = """
            {
              "id": 888,
              "tag_name": "v3.0.0-beta",
              "name": "Beta 3.0",
              "body": "Pre-release version",
              "draft": false,
              "prerelease": true,
              "assets": [
                {
                  "id": 1,
                  "name": "Akira.apk",
                  "size": 100,
                  "browser_download_url": "https://apk"
                }
              ]
            }
        """.trimIndent()

        val client = createMockClient(
            statusCode = 200,
            body = draftJson
        )

        val repo = UpdateRepository(
            httpClient = client,
            customBaseUrl = "https://mock.github.api/releases/latest"
        )

        val result = repo.fetchLatestRelease()
        assertTrue(result is ReleaseFetchResult.Failure)
        val failure = result as ReleaseFetchResult.Failure
        assertEquals(UpdateErrorType.NOT_FOUND, failure.errorType)
        assertTrue(failure.message.contains("pre-release", ignoreCase = true))
    }
}
