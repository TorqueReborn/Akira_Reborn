package com.ghostreborn.akira.features.auth.widgets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.ui.components.AkiraButton
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.components.AkiraTextField
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationDialog(
    email: String,
    onDismiss: () -> Unit,
    onVerifiedSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCaptchaForResend by remember { mutableStateOf(false) }
    var preloadedTurnstileToken by remember { mutableStateOf<String?>(null) }

    fun verifyCode() {
        if (code.trim().isEmpty()) {
            errorMessage = "Please enter the verification code"
            return
        }
        isVerifying = true
        errorMessage = null
        scope.launch {
            try {
                AuthRepository.verifyEmailCode(
                    email = email.trim(),
                    code = code.trim(),
                    context = context
                )
                Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                onVerifiedSuccess()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Verification failed"
            } finally {
                isVerifying = false
            }
        }
    }

    fun resendCode(turnstileToken: String) {
        isResending = true
        errorMessage = null
        scope.launch {
            try {
                AuthRepository.resendVerificationCode(
                    email = email.trim(),
                    recaptchCode = turnstileToken,
                    context = context
                )
                Toast.makeText(context, "Verification code sent to $email", Toast.LENGTH_SHORT).show()
                preloadedTurnstileToken = null
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to resend code"
                preloadedTurnstileToken = null
            } finally {
                isResending = false
            }
        }
    }

    // Preload turnstile in background for quick resend
    if (preloadedTurnstileToken == null) {
        Box(modifier = Modifier.size(1.dp).alpha(0.001f)) {
            TurnstileWebView(
                onTokenExtracted = { token ->
                    preloadedTurnstileToken = token
                    if (showCaptchaForResend) {
                        showCaptchaForResend = false
                        resendCode(token)
                    }
                }
            )
        }
    }

    if (showCaptchaForResend) {
        CloudflareVerificationDialog(
            onTokenExtracted = { token ->
                showCaptchaForResend = false
                resendCode(token)
            },
            onDismiss = { showCaptchaForResend = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .shadow(elevation = 28.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(AkiraLogoGradientBrush)
                .padding(1.5.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.5.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mail,
                                contentDescription = null,
                                tint = AkiraPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verify Your Email",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Code sent to $email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AkiraTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = "6-Digit Verification Code",
                        keyboardType = KeyboardType.Number,
                        enabled = !isVerifying
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AkiraButton(
                        text = "Verify Email",
                        onClick = { verifyCode() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isVerifying && !isResending,
                        loading = isVerifying
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Didn't receive a code?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val token = preloadedTurnstileToken
                                if (!token.isNullOrBlank()) {
                                    resendCode(token)
                                } else {
                                    showCaptchaForResend = true
                                }
                            },
                            enabled = !isVerifying && !isResending
                        ) {
                            if (isResending) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = AkiraPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = AkiraPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "Resend",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = AkiraPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
