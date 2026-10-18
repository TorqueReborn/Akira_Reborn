package com.ghostreborn.akira.features.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ghostreborn.akira.features.home.models.GuideRelatedMedia
import com.ghostreborn.akira.features.home.models.GuideThread
import com.ghostreborn.akira.features.home.services.GuideRepository
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils

@Composable
fun GuideDetailScreen(
    threadId: String,
    titleHint: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToMedia: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var guideDetail by remember { mutableStateOf<GuideThread?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(threadId) {
        isLoading = true
        if (threadId.startsWith("franchise_guide_")) {
            val showId = threadId.removePrefix("franchise_guide_")
            val cached = com.ghostreborn.akira.features.home.services.AnimeRepository.getCachedAnimeDetail(showId)
            val detail = cached ?: try {
                com.ghostreborn.akira.features.home.services.AnimeRepository.fetchAnimeDetail(showId, context)
            } catch (_: Exception) { null }
            guideDetail = if (detail != null) {
                GuideRepository.buildFranchiseWatchGuide(context, detail)
            } else null
        } else {
            guideDetail = GuideRepository.fetchGuideThreadDetail(context, threadId)
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = guideDetail?.title ?: titleHint ?: "Watch Guide",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AkiraPrimary)
                }
            } else if (guideDetail == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Could not load guide details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val guide = guideDetail!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    // Banner Image if available
                    if (!guide.cardImageUrl.isNullOrBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(guide.cardImageUrl)
                                        .apply {
                                            ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) }
                                        }
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = guide.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Guide Badge & Title
                    item {
                        val kindBadge = when (guide.kind.uppercase()) {
                            "WATCH_ORDER", "ORDER" -> "WATCH ORDER GUIDE"
                            "FILLER" -> "FILLER EPISODE GUIDE"
                            "CANON" -> "CANON GUIDE"
                            else -> "COMMUNITY GUIDE"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AkiraLogoGradientBrush)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = kindBadge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = guide.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // Author and meta
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!guide.creatorAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(guide.creatorAvatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = guide.creatorName ?: "Community Contributor",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${guide.views}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Related Media list (Shows/Movies/OVAs in this watch order)
                    if (guide.relatedMedia.isNotEmpty()) {
                        item {
                            Text(
                                text = "RECOMMENDED WATCH ORDER",
                                style = TextStyle(
                                    brush = AkiraLogoGradientBrush,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        items(guide.relatedMedia) { media ->
                            RelatedMediaOrderRow(
                                media = media,
                                onClick = { onNavigateToMedia(media.id, media.displayTitle) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Content Markdown / Body
                    if (!guide.content.isNullOrBlank()) {
                        item {
                            Text(
                                text = "GUIDE DETAILS & NOTES",
                                style = TextStyle(
                                    brush = AkiraLogoGradientBrush,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = guide.content,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedMediaOrderRow(
    media: GuideRelatedMedia,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!media.cover.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.cover)
                        .apply {
                            ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) }
                        }
                        .crossfade(true)
                        .build(),
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AkiraPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.Movie, contentDescription = null, tint = AkiraPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!media.format.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = media.format.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AkiraPrimary
                    )
                }
            }
        }
    }
}
