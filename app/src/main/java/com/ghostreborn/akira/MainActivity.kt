package com.ghostreborn.akira

import android.os.Bundle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.core.view.WindowCompat
import androidx.compose.runtime.remember
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import com.ghostreborn.akira.ui.theme.AkiraTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import com.ghostreborn.akira.ui.components.AkiraButton
import com.ghostreborn.akira.ui.components.AkiraTextField


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        enableEdgeToEdge()
        setContent {
            AkiraTheme {
                LoginScreen(
                    onLoginClick = { email, password ->
                    }
                )
            }
        }
    }
}

@Composable
fun AkiraApp() {
    val (sampleText, setSampleText) = remember {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AKIRA",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "COMPONENT PREVIEW",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            AkiraTextField(
                value = sampleText,
                onValueChange = setSampleText,
                label = "Try typing something"
            )

            Spacer(modifier = Modifier.height(20.dp))

            AkiraButton(
                text = "Continue",
                onClick = {
                    // Preview only; no navigation yet.
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = sampleText.isNotBlank()
            )
        }
    }
}