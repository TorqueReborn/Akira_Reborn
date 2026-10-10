package com.ghostreborn.akira.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.theme.AkiraLogoBlue
import com.ghostreborn.akira.ui.theme.AkiraLogoPink
import com.ghostreborn.akira.ui.theme.AkiraLogoPurple
import com.ghostreborn.akira.ui.theme.AkiraTextSecondary

val AkiraLogoGradientBrush = Brush.linearGradient(
    listOf(AkiraLogoBlue, AkiraLogoPurple, AkiraLogoPink)
)

@Composable
fun AkiraBranding(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AKIRA",
            style = TextStyle(
                brush = AkiraLogoGradientBrush,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your gateway to the anime world.",
            color = AkiraTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            textAlign = TextAlign.Center
        )
    }
}
