package com.ghostreborn.akira.features.player.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.window.Dialog
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.ui.theme.AkiraTheme

@Composable
fun SourceQualityPicker(
    sources: List<StreamSource>,
    selectedSourceIndex: Int,
    onSourceSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E2C)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Stream Quality / Server",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                sources.forEachIndexed { index, source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSourceSelect(index)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedSourceIndex,
                            onClick = {
                                onSourceSelect(index)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = source.sourceName.ifEmpty { "Source ${index + 1}" },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!source.type.isNullOrEmpty()) {
                                Text(
                                    text = source.type.uppercase(),
                                    color = Color.Gray,
                                    fontSize = 12.sp
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
private fun SourceQualityPickerPreview() {
    AkiraTheme {
        SourceQualityPicker(
            sources = listOf(
                StreamSource(sourceName = "Server 1 (1080p)", url = "https://example.com/stream.m3u8"),
                StreamSource(sourceName = "Server 2 (720p)", url = "https://example.com/stream_720.m3u8")
            ),
            selectedSourceIndex = 0,
            onSourceSelect = {},
            onDismiss = {}
        )
    }
}
