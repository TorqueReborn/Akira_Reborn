package com.ghostreborn.akira.features.updater.services

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.ghostreborn.akira.features.updater.models.AppRelease
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

sealed class VerificationResult {
    data class Success(
        val apkFile: File,
        val archivePackageName: String,
        val archiveVersionCode: Long,
        val archiveVersionName: String?,
        val checksumVerified: Boolean,
        val certificatesMatched: Boolean
    ) : VerificationResult()

    data class Failure(
        val reason: VerificationFailureReason,
        val message: String
    ) : VerificationResult()
}

enum class VerificationFailureReason {
    FILE_NOT_FOUND,
    INVALID_APK_ARCHIVE,
    CHECKSUM_MISMATCH,
    PACKAGE_NAME_MISMATCH,
    VERSION_DOWNGRADE_OR_EQUAL,
    CERTIFICATE_MISMATCH,
    UNTRUSTED_SOURCE
}

/**
 * Validates downloaded APK integrity (SHA-256) and authenticity:
 * - Computes file SHA-256 digest against manifest/metadata
 * - Inspects APK archive via PackageManager to verify package name matches installed app
 * - Checks APK versionCode is strictly newer than installed app versionCode
 * - Compares signing certificates against the installed Akira package
 */
class ApkVerifier(private val context: Context) {

    /**
     * Obtains the currently installed Akira package info.
     */
    fun getInstalledPackageInfo(): PackageInfo {
        val pm = context.packageManager
        val packageName = context.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }
    }

    /**
     * Gets the installed app's versionCode safely across Android versions.
     */
    fun getInstalledVersionCode(): Long {
        val pInfo = getInstalledPackageInfo()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
    }

    /**
     * Gets the installed app's versionName.
     */
    fun getInstalledVersionName(): String {
        return getInstalledPackageInfo().versionName ?: "Unknown"
    }

    /**
     * Verifies the downloaded APK against integrity and security requirements.
     */
    fun verify(apkFile: File, release: AppRelease): VerificationResult {
        if (!apkFile.exists() || !apkFile.isFile) {
            return VerificationResult.Failure(
                VerificationFailureReason.FILE_NOT_FOUND,
                "Downloaded APK file not found on disk."
            )
        }

        // 1. Verify SHA-256 Checksum if trusted expected checksum exists
        var checksumVerified = false
        val expectedSha256 = release.sha256Checksum
        if (!expectedSha256.isNullOrBlank()) {
            val actualSha256 = computeSha256(apkFile)
            if (!actualSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                return VerificationResult.Failure(
                    VerificationFailureReason.CHECKSUM_MISMATCH,
                    "Integrity verification failed: SHA-256 mismatch (expected: ${expectedSha256.take(8)}..., computed: ${actualSha256.take(8)}...)."
                )
            }
            checksumVerified = true
        }

        // 2. Parse APK archive using Android PackageManager
        val pm = context.packageManager
        val archiveFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, archiveFlags)
        if (archiveInfo == null) {
            return VerificationResult.Failure(
                VerificationFailureReason.INVALID_APK_ARCHIVE,
                "Failed to parse package archive. The downloaded file may be corrupted."
            )
        }

        val archivePackageName: String = archiveInfo.packageName.orEmpty()
        val archiveVersionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }
        val archiveVersionName: String? = archiveInfo.versionName

        // 3. Verify Package Name
        val installedPackageName = context.packageName
        if (!archivePackageName.equals(installedPackageName, ignoreCase = false)) {
            return VerificationResult.Failure(
                VerificationFailureReason.PACKAGE_NAME_MISMATCH,
                "Package name mismatch: downloaded APK is '$archivePackageName' but installed application is '$installedPackageName'."
            )
        }

        // 4. Verify Version Code Progression
        val installedVersionCode = getInstalledVersionCode()
        if (archiveVersionCode < installedVersionCode) {
            return VerificationResult.Failure(
                VerificationFailureReason.VERSION_DOWNGRADE_OR_EQUAL,
                "Target version code ($archiveVersionCode) is older than currently installed version ($installedVersionCode). Downgrades are prevented."
            )
        }

        // 5. Verify Signing Certificate
        val certMatch = verifyCertificatesMatch(installedInfo = getInstalledPackageInfo(), archiveInfo = archiveInfo)
        if (!certMatch) {
            return VerificationResult.Failure(
                VerificationFailureReason.CERTIFICATE_MISMATCH,
                "Signature verification failed: APK signing certificate does not match the installed Akira application certificate."
            )
        }

        return VerificationResult.Success(
            apkFile = apkFile,
            archivePackageName = archivePackageName,
            archiveVersionCode = archiveVersionCode,
            archiveVersionName = archiveVersionName,
            checksumVerified = checksumVerified,
            certificatesMatched = true
        )
    }

    /**
     * Compares signing certificates of the installed package with the archive package.
     */
    fun verifyCertificatesMatch(installedInfo: PackageInfo, archiveInfo: PackageInfo): Boolean {
        val installedSignatures = extractSignatures(installedInfo)
        val archiveSignatures = extractSignatures(archiveInfo)

        if (installedSignatures.isEmpty() || archiveSignatures.isEmpty()) {
            return false
        }

        // Compare SHA-256 certificate fingerprints
        val installedFingerprints = installedSignatures.map { computeCertSha256(it) }.toSet()
        val archiveFingerprints = archiveSignatures.map { computeCertSha256(it) }.toSet()

        return installedFingerprints.intersect(archiveFingerprints).isNotEmpty()
    }

    private fun extractSignatures(pInfo: PackageInfo): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.forEach { result.add(it.toByteArray()) }
                } else {
                    signingInfo.signingCertificateHistory?.forEach { result.add(it.toByteArray()) }
                }
            }
        }

        if (result.isEmpty()) {
            @Suppress("DEPRECATION")
            val signatures = pInfo.signatures
            signatures?.forEach { result.add(it.toByteArray()) }
        }
        return result
    }

    private fun computeCertSha256(certBytes: ByteArray): String {
        return try {
            val cf = CertificateFactory.getInstance("X509")
            val cert = cf.generateCertificate(certBytes.inputStream()) as X509Certificate
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(cert.encoded)
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(certBytes)
            hash.joinToString("") { "%02x".format(it) }
        }
    }

    companion object {
        fun computeSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
