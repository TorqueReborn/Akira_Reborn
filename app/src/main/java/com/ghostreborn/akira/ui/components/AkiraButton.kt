package com.ghostreborn.akira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.theme.AkiraPrimary

@Composable
fun AkiraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val isActionable = enabled && !loading

    val shadowModifier = if (isActionable) {
        Modifier.shadow(elevation = 6.dp, shape = shape, spotColor = AkiraPrimary.copy(alpha = 0.5f))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .then(shadowModifier)
            .clip(shape)
            .background(
                if (isActionable) {
                    AkiraLogoGradientBrush
                } else {
                    androidx.compose.ui.graphics.SolidColor(Color(0xFFE5E7EB))
                }
            )
            .clickable(
                enabled = isActionable,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White)
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = if (loading) "Please wait..." else text,
                color = if (isActionable) Color.White else Color(0xFF9CA3AF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

