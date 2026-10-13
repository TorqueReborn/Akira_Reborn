package com.ghostreborn.akira.features.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun EpisodeGrid(
    episodes: List<String>,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Episodes (${episodes.size})",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (episodes.isEmpty()) {
            Text(
                text = "No episodes available.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(episodes) { ep ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                color = Color(0xFF2E2E3E),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onEpisodeClick(ep) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ep,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EpisodeGridPreview() {
    AkiraTheme {
        EpisodeGrid(
            episodes = listOf("1", "2", "3", "4", "5", "6", "7", "8"),
            onEpisodeClick = {}
        )
    }
}
