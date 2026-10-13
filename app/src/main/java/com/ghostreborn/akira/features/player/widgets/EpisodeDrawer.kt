package com.ghostreborn.akira.features.player.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraTheme

@Composable
fun EpisodeDrawer(
    episodes: List<String>,
    currentEpisodeNumber: String,
    isFetching: Boolean,
    onEpisodeSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .clickable(enabled = false) {}, // consume clicks
            color = Color(0xFF1E1E2C)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Episodes",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isFetching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AkiraPrimary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(episodes) { ep ->
                            val isSelected = ep == currentEpisodeNumber
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(
                                        color = if (isSelected) AkiraPrimary else Color(0xFF2E2E3E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onEpisodeSelect(ep) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ep,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EpisodeDrawerPreview() {
    AkiraTheme {
        EpisodeDrawer(
            episodes = listOf("1", "2", "3", "4", "5", "6"),
            currentEpisodeNumber = "1",
            isFetching = false,
            onEpisodeSelect = {},
            onDismiss = {}
        )
    }
}
