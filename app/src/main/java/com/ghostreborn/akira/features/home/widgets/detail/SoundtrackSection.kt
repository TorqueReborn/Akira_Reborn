package com.ghostreborn.akira.features.home.widgets.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ghostreborn.akira.features.home.audio.AudioPlayerManager
import com.ghostreborn.akira.features.home.models.MusicTrack
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary

@Composable
fun SoundtrackSection(
    tracks: List<MusicTrack>,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return

    val context = LocalContext.current
    val playbackState by AudioPlayerManager.playbackState.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Text(
                text = "SOUNDTRACK & THEME SONGS",
                style = TextStyle(
                    brush = AkiraLogoGradientBrush,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AkiraPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${tracks.size}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AkiraPrimary
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tracks, key = { it.id }) { track ->
                val isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                val isCurrent = playbackState.currentTrack?.id == track.id
                val isLoadingThis = isCurrent && playbackState.isLoading

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .shadow(if (isCurrent) 6.dp else 2.dp, RoundedCornerShape(16.dp), spotColor = AkiraPrimary)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .then(
                            if (isCurrent) Modifier.border(1.5.dp, AkiraLogoGradientBrush, RoundedCornerShape(16.dp))
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        )
                        .clickable { AudioPlayerManager.playTrack(context, track) }
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Album art thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!track.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = AkiraPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Play / Pause Indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) AkiraPrimary else Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingThis) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (!track.type.isNullOrBlank()) {
                                Text(
                                    text = track.type.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AkiraPrimary
                                )
                            }
                            Text(
                                text = track.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = track.artist ?: "Unknown Artist",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
