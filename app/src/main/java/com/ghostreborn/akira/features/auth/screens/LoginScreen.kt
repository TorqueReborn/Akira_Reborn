package com.ghostreborn.akira.features.auth.screens

import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.core.backup.BackupManager
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.auth.widgets.CloudflareVerificationDialog
import com.ghostreborn.akira.features.auth.widgets.TurnstileWebView
import com.ghostreborn.akira.ui.components.AkiraBranding
import com.ghostreborn.akira.ui.components.AkiraButton
import com.ghostreborn.akira.ui.components.AkiraTextField
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignUp by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCaptchaDialog by remember { mutableStateOf(false) }
    var registeredEmailForVerification by remember { mutableStateOf<String?>(null) }

    var preloadedToken by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch {
                val result = BackupManager.importFromFile(context, uri)
                isImporting = false
                if (result.isSuccess) {
                    val authRestored = result.getOrDefault(false)
                    Toast.makeText(context, "Backup imported successfully!", Toast.LENGTH_SHORT).show()
                    if (authRestored) {
                        onLoginSuccess()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to import file"
                    Toast.makeText(context, "Import failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun performAuth(turnstileToken: String) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val authResult = if (isSignUp) {
                    AuthRepository.register(
                        username = username.trim(),
                        email = email.trim(),
                        rawPassword = password,
                        recaptchCode = turnstileToken,
                        context = context
                    )
                } else {
                    AuthRepository.authenticate(
                        usernameOrEmail = email.trim(),
                        rawPassword = password,
                        recaptchCode = turnstileToken,
                        context = context
                    )
                }

                TokenManager.saveAuthData(
                    context = context,
                    accessToken = authResult.accessToken,
                    refreshToken = authResult.refreshToken,
                    sessionId = authResult.sessionId,
                    username = authResult.username,
                    displayName = authResult.displayName,
                    picture = authResult.picture,
                    userId = authResult.userId,
                    email = if (isSignUp) email.trim() else if (email.contains("@")) email.trim() else authResult.email,
                    isEmailVerified = authResult.isEmailVerified
                )

                Log.d("LoginScreen", "Auth success! Saving tokens and navigating to home...")
                if (isSignUp && !authResult.isEmailVerified && !email.trim().isEmpty()) {
                    registeredEmailForVerification = email.trim()
                } else {
                    onLoginSuccess()
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "Auth error: ${e.message}", e)
                errorMessage = e.message ?: if (isSignUp) "Registration failed" else "Authentication failed"
                preloadedToken = null // Reset token on failure so next attempt gets a fresh one
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Invisible background preloader to solve Turnstile while user enters credentials
    if (preloadedToken == null) {
        Box(modifier = Modifier.size(1.dp).alpha(0.001f)) {
            TurnstileWebView(
                onTokenExtracted = { token ->
                    Log.d("LoginScreen", "Turnstile preloaded in background")
                    preloadedToken = token
                    if (showCaptchaDialog) {
                        showCaptchaDialog = false
                        performAuth(token)
                    }
                }
            )
        }
    }

    if (showCaptchaDialog) {
        CloudflareVerificationDialog(
            onTokenExtracted = { token ->
                showCaptchaDialog = false
                performAuth(token)
            },
            onDismiss = { showCaptchaDialog = false }
        )
    }

    if (registeredEmailForVerification != null) {
        com.ghostreborn.akira.features.auth.widgets.EmailVerificationDialog(
            email = registeredEmailForVerification!!,
            onDismiss = {
                registeredEmailForVerification = null
                onLoginSuccess()
            },
            onVerifiedSuccess = {
                registeredEmailForVerification = null
                onLoginSuccess()
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AkiraBranding()

            Spacer(modifier = Modifier.height(28.dp))

            // Switch between Login and Sign Up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isSignUp) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(enabled = !isLoading) {
                            if (isSignUp) {
                                isSignUp = false
                                errorMessage = null
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isSignUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSignUp) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(enabled = !isLoading) {
                            if (!isSignUp) {
                                isSignUp = true
                                errorMessage = null
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Register",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSignUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isSignUp) {
                AkiraTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                AkiraTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    keyboardType = KeyboardType.Email,
                    enabled = !isLoading
                )
            } else {
                AkiraTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email or Username",
                    keyboardType = KeyboardType.Email,
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AkiraTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                keyboardType = KeyboardType.Password,
                enabled = !isLoading
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage ?: "",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AkiraButton(
                text = if (isSignUp) "Create Account" else "Login",
                onClick = {
                    val token = preloadedToken
                    if (!token.isNullOrBlank()) {
                        performAuth(token)
                    } else {
                        showCaptchaDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isImporting,
                loading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Restore from Backup Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading && !isImporting) {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = AkiraPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import Data Backup",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginScreenPreview() {
    AkiraTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
