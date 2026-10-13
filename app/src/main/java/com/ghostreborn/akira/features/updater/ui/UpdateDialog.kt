package com.ghostreborn.akira.features.updater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghostreborn.akira.features.updater.models.AppRelease
import com.ghostreborn.akira.features.updater.models.UpdateUiState
import com.ghostreborn.akira.ui.theme.AkiraAccentBackground
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraSurface
import com.ghostreborn.akira.ui.theme.AkiraSurfaceBorder
import com.ghostreborn.akira.ui.theme.AkiraTextHint
import com.ghostreborn.akira.ui.theme.AkiraTextPrimary
import com.ghostreborn.akira.ui.theme.AkiraTextSecondary
import java.io.File

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onStartDownload: (AppRelease) -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit,
    onDismissUpdateVersion: (AppRelease) -> Unit
) {
    when (state) {
        is UpdateUiState.Available -> {
            if (!state.isDismissed) {
                AvailableUpdateDialog(
                    state = state,
                    onUpdate = { onStartDownload(state.release) },
                    onLater = {
                        onDismissUpdateVersion(state.release)
                        onDismiss()
                    }
                )
            }
        }
        is UpdateUiState.Downloading -> {
            DownloadingDialog(
                state = state,
                onCancel = onCancelDownload
            )
        }
        is UpdateUiState.Verifying -> {
            VerifyingDialog()
        }
        is UpdateUiState.ReadyToInstall -> {
            ReadyToInstallDialog(
                state = state,
                onInstall = { onInstall(state.apkFile) },
                onDismiss = onDismiss
            )
        }
        is UpdateUiState.Error -> {
            UpdateErrorDialog(
                state = state,
                onDismiss = onDismiss
            )
        }
        else -> Unit
    }
}

@Composable
private fun AvailableUpdateDialog(
    state: UpdateUiState.Available,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isMandatory) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !state.isMandatory,
            dismissOnClickOutside = !state.isMandatory
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isMandatory) "Critical Update Required" else "Update Available",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Akira ${state.targetVersionName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!state.isMandatory) {
                        IconButton(
                            onClick = onLater,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Version comparison pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Version", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(state.currentVersionName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("→", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("New Version", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(state.targetVersionName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // File size info
                val apkSizeMb = if (state.release.apkAsset.size > 0) {
                    String.format("%.1f MB", state.release.apkAsset.size / (1024.0 * 1024.0))
                } else null

                if (apkSizeMb != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Download size: $apkSizeMb",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Release notes section
                if (state.release.body.isNotBlank()) {
                    Text(
                        text = "What's New:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.release.body,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!state.isMandatory) {
                        OutlinedButton(
                            onClick = onLater,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = onUpdate,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AkiraPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingDialog(
    state: UpdateUiState.Downloading,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AkiraSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AkiraSurfaceBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AkiraAccentBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = AkiraPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Downloading Update...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AkiraTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                val downloadedMb = state.bytesDownloaded / (1024.0 * 1024.0)
                val totalMb = state.totalBytes / (1024.0 * 1024.0)
                val progressText = if (state.totalBytes > 0) {
                    String.format("%.1f MB / %.1f MB", downloadedMb, totalMb)
                } else {
                    String.format("%.1f MB downloaded", downloadedMb)
                }

                Text(
                    text = progressText,
                    fontSize = 13.sp,
                    color = AkiraTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (state.progressPercent != null) {
                    LinearProgressIndicator(
                        progress = { state.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AkiraPrimary,
                        trackColor = AkiraAccentBackground
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AkiraPrimary,
                        trackColor = AkiraAccentBackground
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel Download", color = AkiraTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun VerifyingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AkiraSurface,
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = AkiraPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verifying Package...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AkiraTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Checking integrity and security signatures",
                    fontSize = 12.sp,
                    color = AkiraTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ReadyToInstallDialog(
    state: UpdateUiState.ReadyToInstall,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isMandatory) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !state.isMandatory,
            dismissOnClickOutside = !state.isMandatory
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AkiraSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AkiraSurfaceBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Verified & Ready",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AkiraTextPrimary
                        )
                        Text(
                            text = "Integrity & signature confirmed",
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.requiresUnknownSourcesPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Android requires permission to install apps from this source. Tap Install to open system settings and enable permission for Akira.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = "The update has been downloaded and verified. Ready to install version ${state.targetVersionName}.",
                        fontSize = 13.sp,
                        color = AkiraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!state.isMandatory) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later", color = AkiraTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AkiraPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.requiresUnknownSourcesPermission) "Grant & Install" else "Install Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateErrorDialog(
    state: UpdateUiState.Error,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AkiraSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AkiraSurfaceBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Update Problem",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AkiraTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = state.message,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = AkiraTextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AkiraPrimary),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("OK")
                }
            }
        }
    }
}
