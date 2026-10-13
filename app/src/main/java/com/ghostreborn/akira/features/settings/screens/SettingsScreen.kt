package com.ghostreborn.akira.features.settings.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allowAdult by remember { mutableStateOf(AnimeRepository.allowAdult) }
    var denyEcchi by remember { mutableStateOf(AnimeRepository.denyEcchi) }
    var allowUnknown by remember { mutableStateOf(AnimeRepository.allowUnknown) }
    var isLoggingOut by remember { mutableStateOf(false) }

    val username = remember { TokenManager.getUsername(context) ?: "Otaku Member" }
    val email = remember { TokenManager.getEmail(context) }
    val picture = remember { TokenManager.getPicture(context) }
    val isVerified = remember { TokenManager.isEmailVerified(context) }

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
                        imageVector = Icons.Rounded.ArrowBack,
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

            // User Profile Hero Card (Flat, no elevation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar with Neon Akira Gradient Ring
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(AkiraLogoGradientBrush)
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!picture.isNullOrBlank()) {
                            AsyncImage(
                                model = picture,
                                contentDescription = username,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .graphicsLayer(alpha = 0.99f)
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContent()
                                                drawRect(AkiraLogoGradientBrush, blendMode = BlendMode.SrcIn)
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = username,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!email.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = email,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Content Filters Header
            Text(
                text = "CONTENT PREFERENCES",
                style = TextStyle(
                    brush = AkiraLogoGradientBrush,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
            )

            // Content Filters Card (Flat, no elevation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            ) {
                Column {
                    FilterSwitchTile(
                        icon = Icons.Rounded.Explicit,
                        title = "Allow Adult Content (18+)",
                        subtitle = "Include mature and adult catalog entries",
                        checked = allowAdult,
                        onCheckedChange = {
                            allowAdult = it
                            AnimeRepository.updateContentPreferences(context, newAllowAdult = it)
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FilterSwitchTile(
                        icon = Icons.Rounded.FilterAlt,
                        title = "Filter Out Ecchi Content",
                        subtitle = "Hide Ecchi titles from home and searches",
                        checked = denyEcchi,
                        onCheckedChange = {
                            denyEcchi = it
                            AnimeRepository.updateContentPreferences(context, newDenyEcchi = it)
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FilterSwitchTile(
                        icon = Icons.Rounded.HelpOutline,
                        title = "Allow Unrated Content",
                        subtitle = "Include anime without age certification",
                        checked = allowUnknown,
                        onCheckedChange = {
                            allowUnknown = it
                            AnimeRepository.updateContentPreferences(context, newAllowUnknown = it)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Updates & Version Section
            Text(
                text = "APPLICATION & UPDATES",
                style = TextStyle(
                    brush = AkiraLogoGradientBrush,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
            )

            val updateManager = remember { com.ghostreborn.akira.features.updater.services.UpdateManager.getInstance(context) }
            val updateState by updateManager.uiState.collectAsState()
            val installedVersionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                }
            }

            // Updates Card (Flat, no elevation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(AkiraLogoGradientBrush, blendMode = BlendMode.SrcIn)
                                    }
                                }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Akira v$installedVersionName",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (updateState) {
                                    is com.ghostreborn.akira.features.updater.models.UpdateUiState.Checking -> "Checking GitHub for updates..."
                                    is com.ghostreborn.akira.features.updater.models.UpdateUiState.UpToDate -> "You are on the latest release"
                                    is com.ghostreborn.akira.features.updater.models.UpdateUiState.Available -> "Update available: ${(updateState as com.ghostreborn.akira.features.updater.models.UpdateUiState.Available).targetVersionName}"
                                    else -> "Tap to verify latest version"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (updateState is com.ghostreborn.akira.features.updater.models.UpdateUiState.Checking) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = AkiraPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AkiraLogoGradientBrush)
                                    .clickable { updateManager.checkForUpdates(isManual = true) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Check",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (updateState is com.ghostreborn.akira.features.updater.models.UpdateUiState.UpToDate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF143821))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Akira is up to date",
                                fontSize = 12.sp,
                                color = Color(0xFF4ADE80),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Action Button (Flat, no elevation, gradientized with AkiraLogoGradientBrush)
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
                            imageVector = Icons.Rounded.Logout,
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

@Composable
fun FilterSwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer(alpha = 0.99f)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(AkiraLogoGradientBrush, blendMode = BlendMode.SrcIn)
                    }
                }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Custom Akira Pill Switch:
        // Inactive: Surface/glass background with Akira gradient inner circle thumb
        // Active: Akira gradient background with white inner circle thumb
        val animatedThumbBias by animateFloatAsState(
            targetValue = if (checked) 1f else -1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "switch_thumb_bias"
        )

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(CircleShape)
                .then(
                    if (checked) Modifier.background(AkiraLogoGradientBrush)
                    else Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                )
                .clickable { onCheckedChange(!checked) }
                .padding(3.dp),
            contentAlignment = androidx.compose.ui.BiasAlignment(animatedThumbBias, 0f)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .then(
                        if (checked) Modifier.background(Color.White)
                        else Modifier.background(AkiraLogoGradientBrush)
                    )
            )
        }
    }
}
