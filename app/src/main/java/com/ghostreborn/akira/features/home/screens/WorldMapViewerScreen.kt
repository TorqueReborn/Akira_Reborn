package com.ghostreborn.akira.features.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ghostreborn.akira.features.home.models.WorldMap
import com.ghostreborn.akira.features.home.models.WorldMapLocation
import com.ghostreborn.akira.features.home.services.GuideRepository
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils

@Composable
fun WorldMapViewerScreen(
    mapId: String,
    titleHint: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var mapDetail by remember { mutableStateOf<WorldMap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedArcId by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<WorldMapLocation?>(null) }

    // Zoom & pan transformations
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.7f, 5f)
        offset += offsetChange
    }

    LaunchedEffect(mapId, selectedArcId) {
        isLoading = true
        mapDetail = GuideRepository.fetchWorldMapDetail(context, mapId, selectedArcId)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Map Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
        ) {
            val map = mapDetail
            if (map != null && !map.baseMapImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(map.baseMapImage)
                        .apply {
                            ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) }
                        }
                        .crossfade(true)
                        .build(),
                    contentDescription = map.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else if (!isLoading && map == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Could not load world map", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AkiraPrimary)
            }
        }

        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .align(Alignment.TopCenter)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = mapDetail?.title ?: titleHint ?: "World Map",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!mapDetail?.scopeTitle.isNullOrBlank()) {
                    Text(
                        text = mapDetail?.scopeTitle!!,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Zoom Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { scale = (scale * 1.25f).coerceIn(0.7f, 5f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Zoom In", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { scale = (scale / 1.25f).coerceIn(0.7f, 5f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
        }

        // Bottom Arc Selector and Location Drawer
        val map = mapDetail
        if (map != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(bottom = 12.dp)
            ) {
                // Arc Filter Chips if multiple arcs exist
                if (map.arcs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isSelected = selectedArcId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                        else Modifier.background(Color.White.copy(alpha = 0.15f))
                                    )
                                    .clickable { selectedArcId = null }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All Arcs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        items(map.arcs) { arc ->
                            val isSelected = selectedArcId == arc.arcId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                        else Modifier.background(Color.White.copy(alpha = 0.15f))
                                    )
                                    .clickable { selectedArcId = arc.arcId }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = arc.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Locations list horizontal chips
                if (map.locations.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(map.locations) { loc ->
                            val isSelected = selectedLocation?.locationId == loc.locationId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) AkiraPrimary.copy(alpha = 0.35f)
                                        else Color.White.copy(alpha = 0.1f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AkiraPrimary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedLocation = loc }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) AkiraPrimary else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = loc.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Location Detail Popup Card
        if (selectedLocation != null) {
            val loc = selectedLocation!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = loc.displayName,
                            style = TextStyle(
                                brush = AkiraLogoGradientBrush,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { selectedLocation = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (!loc.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loc.description,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
