package com.ghostreborn.akira.features.player.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.player.models.SkipInterval
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary

/**
 * Dynamic Floating Pill for Skipping Intro / Outro / Recap.
 * Displays smart crowd-sourced interval labels (e.g. "Skip Opening", "Skip Outro")
 * or falls back to standard "+85s" skip when no metadata exists.
 */
@Composable
fun SkipSegmentButton(
    activeInterval: SkipInterval?,
    onSkip: (Long) -> Unit,
    currentPositionMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    // Show button strictly when actively inside a verified AniSkip interval (OP, ED, Recap)
    val isVisible = activeInterval != null
    val interval = activeInterval ?: return

    val targetPos = interval.endTimeMs.coerceAtMost(totalDurationMs)
    val buttonLabel = interval.label

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(end = 24.dp, bottom = 72.dp)
                .shadow(12.dp, CircleShape, spotColor = AkiraPrimary)
                .clip(CircleShape)
                .background(Color(0xDD121020))
                .border(1.2.dp, AkiraLogoGradientBrush, CircleShape)
                .clickable { onSkip(targetPos) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = buttonLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
