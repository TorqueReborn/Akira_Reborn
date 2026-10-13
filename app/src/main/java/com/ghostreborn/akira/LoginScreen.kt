package com.ghostreborn.akira

import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.auth.widgets.CloudflareVerificationDialog
import com.ghostreborn.akira.ui.components.AkiraBranding
import com.ghostreborn.akira.ui.components.AkiraButton
import com.ghostreborn.akira.ui.components.AkiraTextField
import com.ghostreborn.akira.ui.theme.AkiraTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCaptchaDialog by remember { mutableStateOf(false) }

    fun performLogin(turnstileToken: String) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val authResult = AuthRepository.authenticate(
                    usernameOrEmail = email.trim(),
                    rawPassword = password,
                    recaptchCode = turnstileToken,
                    context = context
                )

                TokenManager.saveAuthData(
                    context = context,
                    accessToken = authResult.accessToken,
                    refreshToken = authResult.refreshToken,
                    sessionId = authResult.sessionId,
                    username = authResult.username,
                    displayName = authResult.displayName,
                    picture = authResult.picture,
                    userId = authResult.userId,
                    email = if (email.contains("@")) email.trim() else authResult.email,
                    isEmailVerified = authResult.isEmailVerified
                )

                Log.d("LoginScreen", "Auth success! Saving tokens and navigating to home...")
                onLoginSuccess()
            } catch (e: Exception) {
                Log.e("LoginScreen", "Auth error: ${e.message}", e)
                errorMessage = e.message ?: "Authentication failed"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    if (showCaptchaDialog) {
        CloudflareVerificationDialog(
            onTokenExtracted = { token ->
                showCaptchaDialog = false
                performLogin(token)
            },
            onDismiss = { showCaptchaDialog = false }
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

            Spacer(modifier = Modifier.height(32.dp))

            AkiraTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email or Username",
                keyboardType = KeyboardType.Email,
                enabled = !isLoading
            )

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
                text = "Login",
                onClick = {
                    showCaptchaDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                loading = isLoading
            )
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
