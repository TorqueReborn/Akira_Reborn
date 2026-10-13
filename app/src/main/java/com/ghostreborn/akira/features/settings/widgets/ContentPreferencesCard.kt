package com.ghostreborn.akira.features.settings.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush

@Composable
fun ContentPreferencesCard(
    allowAdult: Boolean,
    onAllowAdultChange: (Boolean) -> Unit,
    denyEcchi: Boolean,
    onDenyEcchiChange: (Boolean) -> Unit,
    allowUnknown: Boolean,
    onAllowUnknownChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                    onCheckedChange = onAllowAdultChange
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
                    onCheckedChange = onDenyEcchiChange
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                FilterSwitchTile(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = "Allow Unrated Content",
                    subtitle = "Include anime without age certification",
                    checked = allowUnknown,
                    onCheckedChange = onAllowUnknownChange
                )
            }
        }
    }
}
