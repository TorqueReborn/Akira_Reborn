package com.ghostreborn.akira.features.updater.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed class InstallIntentResult {
    data class LaunchIntent(val intent: Intent) : InstallIntentResult()
    data class PermissionRequired(val settingsIntent: Intent) : InstallIntentResult()
    data class Error(val message: String) : InstallIntentResult()
}

/**
 * Handles package installer intent creation and install permission verification.
 */
class PackageInstallerHelper(private val context: Context) {

    /**
     * Checks if the app has permission to request installing unknown apps (Android 8.0+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Creates an Intent to prompt the user to enable the "Install unknown apps" permission for Akira.
     */
    fun createManageUnknownAppsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Constructs the standard Android package-installation Intent using FileProvider content URI.
     */
    fun createInstallIntent(apkFile: File): InstallIntentResult {
        if (!apkFile.exists()) {
            return InstallIntentResult.Error("APK file not found on disk.")
        }

        if (!canRequestPackageInstalls()) {
            return InstallIntentResult.PermissionRequired(createManageUnknownAppsIntent())
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verify there is an activity to handle this intent
            val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
            if (resolveInfo.isEmpty()) {
                InstallIntentResult.Error("No application or package installer is available to handle APK installation on this device.")
            } else {
                InstallIntentResult.LaunchIntent(intent)
            }
        } catch (e: Exception) {
            InstallIntentResult.Error("Failed to prepare installation intent: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
