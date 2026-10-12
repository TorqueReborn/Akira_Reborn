package com.ghostreborn.akira.features.updater.models

import java.io.File

/**
 * High-level state representing the Updater status for UI.
 */
sealed class UpdateUiState {
    object Idle : UpdateUiState()

    object Checking : UpdateUiState()

    data class UpToDate(
        val currentVersionCode: Long,
        val currentVersionName: String,
        val checkedAtMs: Long
    ) : UpdateUiState()

    data class Available(
        val release: AppRelease,
        val currentVersionCode: Long,
        val currentVersionName: String,
        val targetVersionCode: Long?,
        val targetVersionName: String,
        val isMandatory: Boolean,
        val isDismissed: Boolean = false
    ) : UpdateUiState()

    data class Downloading(
        val release: AppRelease,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Float?
    ) : UpdateUiState()

    data class Verifying(
        val release: AppRelease,
        val apkFile: File
    ) : UpdateUiState()

    data class ReadyToInstall(
        val release: AppRelease,
        val apkFile: File,
        val targetVersionName: String,
        val targetVersionCode: Long,
        val isMandatory: Boolean,
        val requiresUnknownSourcesPermission: Boolean = false
    ) : UpdateUiState()

    data class Error(
        val errorType: UpdateErrorType,
        val message: String,
        val canRetry: Boolean = true,
        val retryAfterSeconds: Long? = null
    ) : UpdateUiState()
}

enum class UpdateErrorType {
    NETWORK_OFFLINE,
    RATE_LIMITED,
    SERVER_ERROR,
    NOT_FOUND,
    NO_COMPATIBLE_APK,
    STORAGE_FULL,
    DOWNLOAD_FAILED,
    CHECKSUM_MISMATCH,
    PACKAGE_NAME_MISMATCH,
    VERSION_DOWNGRADE_OR_MISMATCH,
    CERTIFICATE_MISMATCH,
    INSTALLATION_FAILED,
    UNKNOWN
}
