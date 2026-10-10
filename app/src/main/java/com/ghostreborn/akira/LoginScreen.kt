package com.ghostreborn.akira

import android.util.Patterns
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import com.ghostreborn.akira.ui.components.AkiraButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import com.ghostreborn.akira.ui.components.AkiraTextField

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    isLoading: Boolean = false
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var emailTouched by rememberSaveable {
        mutableStateOf(false)
    }

    var loginAttempted by rememberSaveable {
        mutableStateOf(false)
    }

    val emailIsValid = Patterns.EMAIL_ADDRESS
        .matcher(email.trim())
        .matches()

    val showEmailError =
        (emailTouched || loginAttempted) &&
                email.isNotBlank() &&
                !emailIsValid

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AKIRA",
                style = MaterialTheme.typography.displayLarge,
                color = AkiraPrimary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your anime universe awaits.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Log in to continue watching your favorites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            AkiraTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailTouched = true
                },
                label = "Email",
                keyboardType = KeyboardType.Email,
                isError = showEmailError,
                enabled = !isLoading
            )

            if (showEmailError) {
                Text(
                    text = "Enter a valid email address.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AkiraTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                keyboardType = KeyboardType.Password
            )

            Spacer(modifier = Modifier.height(24.dp))

            AkiraButton(
                text = "Login",
                onClick = {
                    loginAttempted = true

                    if (emailIsValid && password.isNotBlank()) {
                        onLoginClick(email.trim(), password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() &&
                        password.isNotBlank(),
                loading = isLoading
            )
        }
    }
}