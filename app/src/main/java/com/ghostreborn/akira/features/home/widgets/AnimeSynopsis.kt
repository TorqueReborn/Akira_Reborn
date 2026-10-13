package com.ghostreborn.akira.features.home.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.theme.AkiraTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeSynopsis(
    synopsis: String?,
    genres: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Synopsis",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!synopsis.isNullOrBlank()) {
            Text(
                text = synopsis,
                color = Color.LightGray,
                fontSize = 14.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
        } else {
            Text(
                text = "No synopsis available.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        if (genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow {
                genres.forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E2E3E),
                        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimeSynopsisPreview() {
    AkiraTheme {
        AnimeSynopsis(
            synopsis = "Ninja village hero journey text example.",
            genres = listOf("Action", "Adventure", "Fantasy")
        )
    }
}
