package com.ghostreborn.akira.features.settings.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.core.backup.BackupManager
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.settings.widgets.AppUpdateCard
import com.ghostreborn.akira.features.settings.widgets.ContentPreferencesCard
import com.ghostreborn.akira.features.settings.widgets.DataBackupCard
import com.ghostreborn.akira.features.settings.widgets.ProfileHeaderCard
import com.ghostreborn.akira.features.updater.services.UpdateManager
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager.getInstance(context) }
    val updateState by updateManager.uiState.collectAsState()

    var allowAdult by remember { mutableStateOf(AnimeRepository.allowAdult) }
    var denyEcchi by remember { mutableStateOf(AnimeRepository.denyEcchi) }
    var allowUnknown by remember { mutableStateOf(AnimeRepository.allowUnknown) }
    var isLoggingOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AnimeRepository.loadContentPreferences(context)
        allowAdult = AnimeRepository.allowAdult
        denyEcchi = AnimeRepository.denyEcchi
        allowUnknown = AnimeRepository.allowUnknown
    }

    val username = remember { TokenManager.getUsername(context) ?: "Otaku Member" }
    val email = remember { TokenManager.getEmail(context) }
    val picture = remember { TokenManager.getPicture(context) }
    val isVerified = remember { TokenManager.isEmailVerified(context) }

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                val result = BackupManager.exportToFile(context, uri)
                isExporting = false
                if (result.isSuccess) {
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch {
                val result = BackupManager.importFromFile(context, uri)
                isImporting = false
                if (result.isSuccess) {
                    AnimeRepository.loadContentPreferences(context)
                    allowAdult = AnimeRepository.allowAdult
                    denyEcchi = AnimeRepository.denyEcchi
                    allowUnknown = AnimeRepository.allowUnknown
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "SETTINGS",
                    style = TextStyle(
                        brush = AkiraLogoGradientBrush,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                )
            }

            // User Profile Hero Card
            ProfileHeaderCard(
                username = username,
                email = email,
                picture = picture,
                isVerified = isVerified
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Content Preferences Card
            ContentPreferencesCard(
                allowAdult = allowAdult,
                onAllowAdultChange = {
                    allowAdult = it
                    AnimeRepository.updateContentPreferences(context, newAllowAdult = it)
                },
                denyEcchi = denyEcchi,
                onDenyEcchiChange = {
                    denyEcchi = it
                    AnimeRepository.updateContentPreferences(context, newDenyEcchi = it)
                },
                allowUnknown = allowUnknown,
                onAllowUnknownChange = {
                    allowUnknown = it
                    AnimeRepository.updateContentPreferences(context, newAllowUnknown = it)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Application & Updates Card
            AppUpdateCard(
                versionName = updateManager.currentVersionName,
                versionCode = updateManager.currentVersionCode,
                updateState = updateState,
                onCheckUpdate = { updateManager.checkForUpdates(isManual = true) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Data Backup & Restore Card
            DataBackupCard(
                isExporting = isExporting,
                isImporting = isImporting,
                onExportClick = {
                    val fileName = "akira_backup_${System.currentTimeMillis() / 1000}.json"
                    exportLauncher.launch(fileName)
                },
                onImportClick = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(AkiraLogoGradientBrush)
                    .clickable(enabled = !isLoggingOut) {
                        isLoggingOut = true
                        scope.launch {
                            val token = TokenManager.getAccessToken(context)
                            AuthRepository.logout(token, context = context)
                            TokenManager.clear(context)
                            onLogoutSuccess()
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Log Out",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    AkiraTheme {
        SettingsScreen(
            onNavigateBack = {},
            onLogoutSuccess = {}
        )
    }
}
