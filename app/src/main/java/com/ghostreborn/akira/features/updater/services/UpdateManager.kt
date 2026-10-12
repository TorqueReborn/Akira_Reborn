package com.ghostreborn.akira.features.updater.services

import android.content.Context
import com.ghostreborn.akira.features.updater.models.AppRelease
import com.ghostreborn.akira.features.updater.models.UpdateErrorType
import com.ghostreborn.akira.features.updater.models.UpdateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Coordinator singleton managing update checks, downloads, verifications, and UI states.
 */
class UpdateManager(
    private val context: Context,
    private val repository: UpdateRepository = UpdateRepository(),
    private val preferences: UpdatePreferences = UpdatePreferences(context),
    private val verifier: ApkVerifier = ApkVerifier(context),
    private val downloader: ApkDownloader = ApkDownloader(context),
    private val installerHelper: PackageInstallerHelper = PackageInstallerHelper(context)
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val checkMutex = Mutex()
    private var downloadJob: Job? = null

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var currentCachedRelease: AppRelease? = null

    init {
        // Load cached release from preferences if exists
        preferences.lastCachedReleaseJson?.let {
            currentCachedRelease = ReleaseParser.parseRelease(it)
        }
    }

    /**
     * Checks for updates.
     * @param isManual True if initiated by the user (bypasses 24-hr freshness check).
     */
    fun checkForUpdates(isManual: Boolean = false) {
        scope.launch {
            checkMutex.withLock {
                // If a download is active, don't interrupt it
                if (_uiState.value is UpdateUiState.Downloading || _uiState.value is UpdateUiState.Verifying) {
                    return@withLock
                }

                val currentVersionCode = verifier.getInstalledVersionCode()
                val currentVersionName = verifier.getInstalledVersionName()

                // Check rate limiting
                if (preferences.isRateLimited()) {
                    val remainingSec = preferences.remainingRateLimitSeconds()
                    if (isManual) {
                        _uiState.value = UpdateUiState.Error(
                            errorType = UpdateErrorType.RATE_LIMITED,
                            message = "GitHub rate limit exceeded. Please wait $remainingSec seconds before retrying.",
                            retryAfterSeconds = remainingSec
                        )
                    }
                    return@withLock
                }

                // Freshness check for automatic checks
                if (!isManual && preferences.isFresh()) {
                    // Check if we have an un-dismissed available update from cache
                    val cached = currentCachedRelease
                    if (cached != null) {
                        evaluateReleaseAvailability(cached, currentVersionCode, currentVersionName, isManual = false)
                    }
                    return@withLock
                }

                _uiState.value = UpdateUiState.Checking

                val fetchResult = repository.fetchLatestRelease(
                    eTag = preferences.lastETag,
                    cachedRelease = currentCachedRelease
                )

                when (fetchResult) {
                    is ReleaseFetchResult.Success -> {
                        preferences.lastCheckTimestamp = System.currentTimeMillis()
                        fetchResult.eTag?.let { preferences.lastETag = it }

                        val release = fetchResult.release
                        currentCachedRelease = release

                        evaluateReleaseAvailability(
                            release = release,
                            currentVersionCode = currentVersionCode,
                            currentVersionName = currentVersionName,
                            isManual = isManual
                        )
                    }
                    is ReleaseFetchResult.Failure -> {
                        if (fetchResult.errorType == UpdateErrorType.RATE_LIMITED) {
                            val waitSec = fetchResult.retryAfterSeconds ?: 60L
                            preferences.rateLimitResetTimestamp = System.currentTimeMillis() + (waitSec * 1000L)
                        }

                        // Only show error for manual checks, or if it was checking
                        if (isManual) {
                            _uiState.value = UpdateUiState.Error(
                                errorType = fetchResult.errorType,
                                message = fetchResult.message,
                                retryAfterSeconds = fetchResult.retryAfterSeconds
                            )
                        } else {
                            // In automatic background check, revert safely to Idle
                            _uiState.value = UpdateUiState.Idle
                        }
                    }
                }
            }
        }
    }

    private fun evaluateReleaseAvailability(
        release: AppRelease,
        currentVersionCode: Long,
        currentVersionName: String,
        isManual: Boolean
    ) {
        val targetCode = release.explicitVersionCode
        val isCritical = release.minSupportedVersionCode?.let { currentVersionCode < it } ?: false

        // Check asset-based freshness (Option B: same tag/release edited with updated APK)
        val assetId = release.apkAsset.id
        val assetUpdatedAt = release.apkAsset.updatedAt ?: release.apkAsset.createdAt
        val lastInstalledAssetId = preferences.installedAssetId
        val lastInstalledAssetTime = preferences.installedAssetUpdatedAt

        // If versionCode is explicitly lower than installed, or if the exact same asset ID was already installed: Up to date!
        val isExplicitDowngrade = targetCode != null && targetCode < currentVersionCode
        val isSameInstalledAsset = lastInstalledAssetId > 0 && lastInstalledAssetId == assetId &&
                (lastInstalledAssetTime == null || lastInstalledAssetTime == assetUpdatedAt)

        if (isExplicitDowngrade || isSameInstalledAsset) {
            if (isManual) {
                _uiState.value = UpdateUiState.UpToDate(
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName,
                    checkedAtMs = System.currentTimeMillis()
                )
            } else {
                _uiState.value = UpdateUiState.Idle
            }
            return
        }

        // Check if user already dismissed this specific version or asset (ignored if critical or manual)
        val dismissKey = release.explicitVersionCode ?: release.apkAsset.id
        val isDismissed = !isCritical && (preferences.dismissedVersionCode == dismissKey)

        if (isDismissed && !isManual) {
            _uiState.value = UpdateUiState.Idle
            return
        }

        val targetVersionName = release.name.ifBlank { release.tagName }

        _uiState.value = UpdateUiState.Available(
            release = release,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName,
            targetVersionCode = targetCode,
            targetVersionName = targetVersionName,
            isMandatory = isCritical,
            isDismissed = isDismissed
        )
    }

    /**
     * Dismisses the currently available update.
     */
    fun dismissUpdate(release: AppRelease) {
        val dismissKey = release.explicitVersionCode ?: release.apkAsset.id
        preferences.dismissedVersionCode = dismissKey
        val current = _uiState.value
        if (current is UpdateUiState.Available) {
            _uiState.value = current.copy(isDismissed = true)
        }
    }

    /**
     * Resets dismiss state and hides update dialog.
     */
    fun clearState() {
        _uiState.value = UpdateUiState.Idle
    }

    /**
     * Initiates downloading of the update APK.
     */
    fun startDownload(release: AppRelease) {
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch {
            _uiState.value = UpdateUiState.Downloading(
                release = release,
                bytesDownloaded = 0L,
                totalBytes = release.apkAsset.size,
                progressPercent = 0f
            )

            val downloadResult = downloader.downloadApk(release) { downloaded, total ->
                val percent = if (total > 0) (downloaded.toFloat() / total.toFloat()) else null
                _uiState.value = UpdateUiState.Downloading(
                    release = release,
                    bytesDownloaded = downloaded,
                    totalBytes = total,
                    progressPercent = percent
                )
            }

            when (downloadResult) {
                is DownloadResult.Success -> {
                    verifyDownloadedApk(release, downloadResult.apkFile)
                }
                is DownloadResult.Failure -> {
                    _uiState.value = UpdateUiState.Error(
                        errorType = downloadResult.errorType,
                        message = downloadResult.message
                    )
                }
                is DownloadResult.Cancelled -> {
                    _uiState.value = UpdateUiState.Idle
                }
            }
        }
    }

    /**
     * Cancels an active download.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloader.cleanup()
        _uiState.value = UpdateUiState.Idle
    }

    /**
     * Verifies downloaded APK file.
     */
    private fun verifyDownloadedApk(release: AppRelease, apkFile: File) {
        _uiState.value = UpdateUiState.Verifying(release, apkFile)

        val verification = verifier.verify(apkFile, release)
        when (verification) {
            is VerificationResult.Success -> {
                val currentVersionCode = verifier.getInstalledVersionCode()
                val isCritical = release.minSupportedVersionCode?.let { currentVersionCode < it } ?: false

                val canInstall = installerHelper.canRequestPackageInstalls()

                _uiState.value = UpdateUiState.ReadyToInstall(
                    release = release,
                    apkFile = verification.apkFile,
                    targetVersionName = verification.archiveVersionName ?: release.name,
                    targetVersionCode = verification.archiveVersionCode,
                    isMandatory = isCritical,
                    requiresUnknownSourcesPermission = !canInstall
                )
            }
            is VerificationResult.Failure -> {
                // Remove invalid APK
                apkFile.delete()
                val errorType = when (verification.reason) {
                    VerificationFailureReason.CHECKSUM_MISMATCH -> UpdateErrorType.CHECKSUM_MISMATCH
                    VerificationFailureReason.PACKAGE_NAME_MISMATCH -> UpdateErrorType.PACKAGE_NAME_MISMATCH
                    VerificationFailureReason.VERSION_DOWNGRADE_OR_EQUAL -> UpdateErrorType.VERSION_DOWNGRADE_OR_MISMATCH
                    VerificationFailureReason.CERTIFICATE_MISMATCH -> UpdateErrorType.CERTIFICATE_MISMATCH
                    else -> UpdateErrorType.DOWNLOAD_FAILED
                }

                _uiState.value = UpdateUiState.Error(
                    errorType = errorType,
                    message = verification.message
                )
            }
        }
    }

    /**
     * Launches the package installer.
     */
    fun installApk(apkFile: File) {
        val currentReadyState = _uiState.value as? UpdateUiState.ReadyToInstall
        if (currentReadyState != null) {
            preferences.installedAssetId = currentReadyState.release.apkAsset.id
            preferences.installedAssetUpdatedAt = currentReadyState.release.apkAsset.updatedAt ?: currentReadyState.release.apkAsset.createdAt
        }

        val result = installerHelper.createInstallIntent(apkFile)
        when (result) {
            is InstallIntentResult.LaunchIntent -> {
                try {
                    context.startActivity(result.intent)
                } catch (e: Exception) {
                    _uiState.value = UpdateUiState.Error(
                        errorType = UpdateErrorType.INSTALLATION_FAILED,
                        message = "Failed to launch package installer: ${e.localizedMessage}"
                    )
                }
            }
            is InstallIntentResult.PermissionRequired -> {
                val current = _uiState.value
                if (current is UpdateUiState.ReadyToInstall) {
                    _uiState.value = current.copy(requiresUnknownSourcesPermission = true)
                }
                try {
                    context.startActivity(result.settingsIntent)
                } catch (e: Exception) {
                    _uiState.value = UpdateUiState.Error(
                        errorType = UpdateErrorType.INSTALLATION_FAILED,
                        message = "Could not open Install Unknown Apps setting: ${e.localizedMessage}"
                    )
                }
            }
            is InstallIntentResult.Error -> {
                _uiState.value = UpdateUiState.Error(
                    errorType = UpdateErrorType.INSTALLATION_FAILED,
                    message = result.message
                )
            }
        }
    }

    companion object {
        @Volatile
        private var instance: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
