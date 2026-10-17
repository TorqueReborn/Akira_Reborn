package com.ghostreborn.akira.features.player.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary

/**
 * Bottom playback scrubber bar showing elapsed time, buffered track,
 * interactive seek slider, and remaining duration.
 */
@Composable
fun PlayerScrubberBar(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    totalDurationMs: Long,
    onSeekStart: () -> Unit,
    onSeekProgress: (Long) -> Unit,
    onSeekComplete: (Long) -> Unit,
    formatDuration: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xF0080610))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x55000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatDuration(currentPositionMs),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            val progressFraction = remember(currentPositionMs, totalDurationMs) {
                if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
            }

            val bufferedFraction = remember(bufferedPositionMs, totalDurationMs) {
                if (totalDurationMs > 0) (bufferedPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .pointerInput(totalDurationMs) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width).coerceIn(0f, 1f)
                            val newPos = (frac * totalDurationMs).toLong()
                            onSeekComplete(newPos)
                        }
                    }
                    .pointerInput(totalDurationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                onSeekStart()
                                val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeekProgress((frac * totalDurationMs).toLong())
                            },
                            onDragEnd = {
                                onSeekComplete(currentPositionMs)
                            },
                            onDragCancel = {
                                onSeekComplete(currentPositionMs)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeekProgress((frac * totalDurationMs).toLong())
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val trackWidth = maxWidth
                val activeWidth = trackWidth * progressFraction
                val bufferedWidth = trackWidth * bufferedFraction

                // 1. Background full track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x35FFFFFF))
                )

                // 2. Buffered progress track
                Box(
                    modifier = Modifier
                        .width(bufferedWidth)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x80FFFFFF))
                )

                // 3. Played active progress track
                Box(
                    modifier = Modifier
                        .width(activeWidth)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(AkiraLogoGradientBrush)
                )

                Box(
                    modifier = Modifier
                        .padding(start = (activeWidth - 8.dp).coerceAtLeast(0.dp))
                        .size(18.dp)
                        .shadow(10.dp, CircleShape, spotColor = AkiraPrimary)
                        .clip(CircleShape)
                        .background(AkiraLogoGradientBrush)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x55000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val remaining = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)
                Text(
                    text = "-${formatDuration(remaining)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
