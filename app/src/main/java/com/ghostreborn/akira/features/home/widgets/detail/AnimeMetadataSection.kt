package com.ghostreborn.akira.features.home.widgets.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.auth.services.WatchStatus
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.models.GuideThread
import com.ghostreborn.akira.features.home.models.WorldMap
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeMetadataSection(
    animeDetail: AnimeDetail,
    watchStateId: String?,
    watchStatus: WatchStatus?,
    guides: List<GuideThread>,
    worldMaps: List<WorldMap>,
    userRemarks: String,
    isRemarksEditing: Boolean,
    isSynopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onRemarksChange: (String) -> Unit,
    onSaveRemarks: () -> Unit,
    onToggleRemarksEditing: () -> Unit,
    onSetWatchStatus: (WatchStatus) -> Unit,
    onFilterByGenre: ((String) -> Unit)?,
    onOpenGuide: ((String, String?) -> Unit)?,
    onOpenMap: ((String, String?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val d = animeDetail

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Metadata Pills Row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (d.score != null && d.score > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B2D14))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                        .clickable {
                            val malUrl = if (d.malId != null && d.malId > 0) {
                                "https://myanimelist.net/anime/${d.malId}"
                            } else {
                                "https://myanimelist.net/anime.php?q=${Uri.encode(d.name)}"
                            }
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(malUrl))
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", d.score),
                            color = Color(0xFFFDE68A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (d.type != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onFilterByGenre?.invoke(d.type) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = d.type.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (d.status != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onFilterByGenre?.invoke(d.status) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = d.status.uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cloud Watchlist Status Selector Pill
            if (watchStateId != null) {
                var isDropdownOpen by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (watchStatus != null) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .clickable { isDropdownOpen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = watchStatus?.label ?: "Add to Watchlist",
                                color = if (watchStatus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                tint = if (watchStatus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false }
                    ) {
                        WatchStatus.entries.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = statusOption.label,
                                        fontWeight = if (watchStatus == statusOption) FontWeight.Bold else FontWeight.Normal,
                                        color = if (watchStatus == statusOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    isDropdownOpen = false
                                    onSetWatchStatus(statusOption)
                                }
                            )
                        }
                    }
                }
            }

            // Quick Guide & Map Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AkiraLogoGradientBrush)
                    .clickable {
                        val firstGuide = guides.firstOrNull()
                        val firstMap = worldMaps.firstOrNull()
                        if (firstMap != null) {
                            onOpenMap?.invoke(firstMap.id, firstMap.title)
                        } else if (firstGuide != null) {
                            onOpenGuide?.invoke(firstGuide.id, firstGuide.title)
                        } else {
                            // Fall back to opening franchise guide with title
                            onOpenGuide?.invoke("franchise_guide_${d.id}", "${d.englishName ?: d.name}: Watch Order")
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GUIDES & MAPS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Genres
        if (d.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                d.genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { onFilterByGenre?.invoke(genre) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = genre,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Synopsis with Expand / Collapse
        if (!d.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text(
                    text = d.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSynopsisExpanded) "Show Less" else "Read More",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AkiraPrimary,
                    modifier = Modifier
                        .clickable { onToggleSynopsis() }
                        .padding(vertical = 2.dp)
                )
            }
        }

        // Personal Notes / Remarks Section
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PERSONAL NOTES / REMARKS",
                        style = TextStyle(
                            brush = AkiraLogoGradientBrush,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isRemarksEditing) "Done" else if (userRemarks.isBlank()) "+ Add Note" else "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AkiraPrimary,
                        modifier = Modifier
                            .clickable {
                                if (isRemarksEditing) {
                                    onSaveRemarks()
                                }
                                onToggleRemarksEditing()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isRemarksEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userRemarks,
                        onValueChange = onRemarksChange,
                        placeholder = { Text("Add private remarks or thoughts on this title...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AkiraPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (userRemarks.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = userRemarks,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No private remarks yet. Tap '+ Add Note' to write personal notes.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
