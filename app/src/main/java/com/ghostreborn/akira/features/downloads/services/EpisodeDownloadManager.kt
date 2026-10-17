package com.ghostreborn.akira.features.downloads.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ghostreborn.akira.features.downloads.models.DownloadStatus
import com.ghostreborn.akira.features.downloads.models.DownloadedEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Robust episode download manager that handles background streaming downloads directly
 * to internal app storage, tracks byte progress, persists completed media records,
 * and allows instant offline playback without active internet.
 */
object EpisodeDownloadManager {

    private const val TAG = "EpisodeDownloadManager"
    private const val PREFS_NAME = "akira_downloads_db"
    private const val KEY_DOWNLOADS = "saved_episodes"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _downloads = MutableStateFlow<Map<String, DownloadedEpisode>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadedEpisode>> = _downloads.asStateFlow()

    private var prefs: SharedPreferences? = null

    private fun episodeKey(animeId: String, episodeNumber: String): String = "$animeId:$episodeNumber"

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadSavedDownloads(context)
        }
    }

    private fun getDownloadsDir(context: Context): File {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadSavedDownloads(context: Context) {
        val p = prefs ?: return
        val rawJson = p.getString(KEY_DOWNLOADS, null) ?: return
        try {
            val array = JSONArray(rawJson)
            val map = mutableMapOf<String, DownloadedEpisode>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val animeId = obj.optString("animeId")
                val epNum = obj.optString("episodeNumber")
                val path = obj.optString("videoPath")
                val file = File(path)
                val status = if (file.exists() && file.length() > 0) DownloadStatus.COMPLETED else DownloadStatus.FAILED

                val ep = DownloadedEpisode(
                    animeId = animeId,
                    episodeNumber = epNum,
                    animeTitle = obj.optString("animeTitle"),
                    thumbnail = obj.optString("thumbnail").takeIf { it.isNotBlank() },
                    videoPath = path,
                    totalBytes = obj.optLong("totalBytes", file.length()),
                    downloadedBytes = file.length(),
                    status = status,
                    quality = obj.optString("quality", "1080p"),
                    downloadUrl = obj.optString("downloadUrl"),
                    completedAt = obj.optLong("completedAt", 0L)
                )
                map[episodeKey(animeId, epNum)] = ep
            }
            _downloads.value = map
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring saved downloads: ${e.message}")
        }
    }

    private fun persistDownloads() {
        val p = prefs ?: return
        try {
            val array = JSONArray()
            _downloads.value.values.forEach { ep ->
                if (ep.status == DownloadStatus.COMPLETED) {
                    val obj = JSONObject().apply {
                        put("animeId", ep.animeId)
                        put("episodeNumber", ep.episodeNumber)
                        put("animeTitle", ep.animeTitle)
                        put("thumbnail", ep.thumbnail ?: "")
                        put("videoPath", ep.videoPath)
                        put("totalBytes", ep.totalBytes)
                        put("quality", ep.quality)
                        put("downloadUrl", ep.downloadUrl)
                        put("completedAt", ep.completedAt)
                    }
                    array.put(obj)
                }
            }
            p.edit().putString(KEY_DOWNLOADS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting downloads: ${e.message}")
        }
    }

    /**
     * Enqueues and starts downloading an episode in the background.
     */
    fun enqueueDownload(
        context: Context,
        animeId: String,
        episodeNumber: String,
        animeTitle: String,
        downloadUrl: String,
        thumbnail: String? = null,
        quality: String = "1080p"
    ) {
        init(context)
        val key = episodeKey(animeId, episodeNumber)
        if (activeJobs.containsKey(key)) return

        val ext = if (downloadUrl.contains(".mp4", ignoreCase = true)) "mp4" else "mkv"
        val fileName = "${animeId}_ep_${episodeNumber.replace(".", "_")}.$ext"
        val targetFile = File(getDownloadsDir(context), fileName)

        val episodeRecord = DownloadedEpisode(
            animeId = animeId,
            episodeNumber = episodeNumber,
            animeTitle = animeTitle,
            thumbnail = thumbnail,
            videoPath = targetFile.absolutePath,
            status = DownloadStatus.DOWNLOADING,
            quality = quality,
            downloadUrl = downloadUrl
        )

        _downloads.update { it + (key to episodeRecord) }

        val job = scope.launch {
            var input: InputStream? = null
            var output: FileOutputStream? = null
            try {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _downloads.update {
                            it + (key to episodeRecord.copy(status = DownloadStatus.FAILED, error = "HTTP ${response.code}"))
                        }
                        return@launch
                    }

                    val body = response.body ?: throw IllegalStateException("Empty body")
                    val contentLength = body.contentLength()
                    input = body.byteStream()
                    output = FileOutputStream(targetFile)

                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastUpdate = System.currentTimeMillis()

                    while (input!!.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            targetFile.delete()
                            _downloads.update { it + (key to episodeRecord.copy(status = DownloadStatus.FAILED, error = "Cancelled")) }
                            return@launch
                        }

                        output!!.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 300L) {
                            _downloads.update {
                                it + (key to episodeRecord.copy(
                                    downloadedBytes = totalRead,
                                    totalBytes = if (contentLength > 0) contentLength else totalRead,
                                    status = DownloadStatus.DOWNLOADING
                                ))
                            }
                            lastUpdate = now
                        }
                    }
                    output!!.flush()

                    _downloads.update {
                        it + (key to episodeRecord.copy(
                            downloadedBytes = totalRead,
                            totalBytes = totalRead,
                            status = DownloadStatus.COMPLETED,
                            completedAt = System.currentTimeMillis()
                        ))
                    }
                    persistDownloads()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error for $key: ${e.message}")
                if (targetFile.exists()) targetFile.delete()
                _downloads.update {
                    it + (key to episodeRecord.copy(status = DownloadStatus.FAILED, error = e.message))
                }
            } finally {
                input?.close()
                output?.close()
                activeJobs.remove(key)
            }
        }

        activeJobs[key] = job
    }

    fun cancelDownload(animeId: String, episodeNumber: String) {
        val key = episodeKey(animeId, episodeNumber)
        activeJobs[key]?.cancel()
        activeJobs.remove(key)
        _downloads.value[key]?.let { ep ->
            val file = File(ep.videoPath)
            if (file.exists()) file.delete()
        }
        _downloads.update { it - key }
        persistDownloads()
    }

    fun deleteDownloadedEpisode(animeId: String, episodeNumber: String) {
        val key = episodeKey(animeId, episodeNumber)
        _downloads.value[key]?.let { ep ->
            val file = File(ep.videoPath)
            if (file.exists()) file.delete()
        }
        _downloads.update { it - key }
        persistDownloads()
    }

    /**
     * Checks if a specific episode has been downloaded and is available for offline playback.
     */
    fun getDownloadedEpisode(animeId: String, episodeNumber: String): DownloadedEpisode? {
        val key = episodeKey(animeId, episodeNumber)
        val ep = _downloads.value[key] ?: return null
        if (ep.status == DownloadStatus.COMPLETED && File(ep.videoPath).exists()) {
            return ep
        }
        return null
    }

    fun isDownloaded(animeId: String, episodeNumber: String): Boolean {
        return getDownloadedEpisode(animeId, episodeNumber) != null
    }
}
