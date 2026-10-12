package com.ghostreborn.akira.features.updater.services

import android.content.Context
import com.ghostreborn.akira.features.updater.models.AppRelease
import com.ghostreborn.akira.features.updater.models.UpdateErrorType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class DownloadResult {
    data class Success(val apkFile: File) : DownloadResult()
    data class Failure(val errorType: UpdateErrorType, val message: String) : DownloadResult()
    object Cancelled : DownloadResult()
}

/**
 * Downloads update APK securely into app-private cache storage.
 * Streams data directly to storage to prevent high memory usage.
 * Supports download progress callbacks and cancellation with file cleanup.
 */
class ApkDownloader(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val updateDir: File
        get() {
            val dir = File(context.cacheDir, "updates")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Downloads the APK asset associated with the release.
     * Invokes [onProgress] with (bytesDownloaded, totalBytes).
     */
    suspend fun downloadApk(
        release: AppRelease,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val asset = release.apkAsset
        val sanitizedName = asset.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destinationFile = File(updateDir, "update_${release.id}_$sanitizedName")
        val tempFile = File(updateDir, "${destinationFile.name}.download")

        // Pre-flight storage check if size is known
        val requiredBytes = if (asset.size > 0) asset.size else 30L * 1024L * 1024L
        val usableSpace = updateDir.usableSpace
        if (usableSpace < requiredBytes + (10L * 1024L * 1024L)) { // 10MB safety buffer
            return@withContext DownloadResult.Failure(
                errorType = UpdateErrorType.STORAGE_FULL,
                message = "Insufficient storage space to download update. Requires at least ${(requiredBytes / (1024 * 1024))} MB."
            )
        }

        // Clean up any stale partial files
        if (tempFile.exists()) tempFile.delete()

        val request = Request.Builder()
            .url(asset.browserDownloadUrl)
            .header("User-Agent", "Akira-Android-App-Updater")
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext DownloadResult.Failure(
                        errorType = UpdateErrorType.DOWNLOAD_FAILED,
                        message = "Download failed with HTTP ${resp.code}: ${resp.message}"
                    )
                }

                val body = resp.body ?: return@withContext DownloadResult.Failure(
                    errorType = UpdateErrorType.DOWNLOAD_FAILED,
                    message = "Server returned empty response body."
                )

                val contentLength = body.contentLength().let { if (it > 0) it else asset.size }
                var downloadedBytes = 0L

                body.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        var lastProgressReportTime = 0L

                        while (inputStream.read(buffer).also { read = it } != -1) {
                            if (!currentCoroutineContext().isActive) {
                                tempFile.delete()
                                return@withContext DownloadResult.Cancelled
                            }

                            outputStream.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastProgressReportTime > 150L || downloadedBytes == contentLength) {
                                lastProgressReportTime = now
                                onProgress(downloadedBytes, contentLength)
                            }
                        }
                        outputStream.flush()
                    }
                }

                // If content length was known, ensure complete download
                if (contentLength > 0 && downloadedBytes < contentLength) {
                    tempFile.delete()
                    return@withContext DownloadResult.Failure(
                        errorType = UpdateErrorType.DOWNLOAD_FAILED,
                        message = "Download interrupted: received $downloadedBytes bytes of $contentLength."
                    )
                }

                // Atomically rename temp file to target destination
                if (destinationFile.exists()) destinationFile.delete()
                if (!tempFile.renameTo(destinationFile)) {
                    // Fallback to copy if rename fails across file systems
                    tempFile.copyTo(destinationFile, overwrite = true)
                    tempFile.delete()
                }

                DownloadResult.Success(destinationFile)
            }
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            DownloadResult.Cancelled
        } catch (e: IOException) {
            if (tempFile.exists()) tempFile.delete()
            DownloadResult.Failure(
                errorType = UpdateErrorType.DOWNLOAD_FAILED,
                message = "Network error during download: ${e.localizedMessage ?: "Connection interrupted"}"
            )
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            DownloadResult.Failure(
                errorType = UpdateErrorType.DOWNLOAD_FAILED,
                message = e.localizedMessage ?: "Unexpected error during APK download."
            )
        }
    }

    /**
     * Cleans up cached update files older than 3 days or matching a specific file.
     */
    fun cleanup(keepFile: File? = null) {
        try {
            val files = updateDir.listFiles() ?: return
            val cutoff = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            for (file in files) {
                if (keepFile != null && file.absolutePath == keepFile.absolutePath) continue
                if (file.name.endsWith(".download") || file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
