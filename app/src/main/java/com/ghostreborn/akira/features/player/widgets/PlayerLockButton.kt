package com.ghostreborn.akira.features.player.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary

@Composable
fun PlayerLockButton(
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lockModifier = if (isLocked) {
        Modifier.background(AkiraLogoGradientBrush)
    } else {
        Modifier
            .background(Color(0x99110E1C))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
    }

    IconButton(
        onClick = onToggleLock,
        modifier = modifier
            .padding(start = 24.dp)
            .size(40.dp)
            .shadow(if (isLocked) 8.dp else 0.dp, CircleShape, spotColor = AkiraPrimary)
            .clip(CircleShape)
            .then(lockModifier)
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            contentDescription = "Lock Screen",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
