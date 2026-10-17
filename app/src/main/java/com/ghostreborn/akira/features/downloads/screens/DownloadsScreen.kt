package com.ghostreborn.akira.features.downloads.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownloadDone
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.downloads.models.DownloadedEpisode
import com.ghostreborn.akira.features.downloads.services.EpisodeDownloadManager
import com.ghostreborn.akira.features.downloads.widgets.DownloadedEpisodeItem
import com.ghostreborn.akira.ui.theme.AkiraPrimary

/**
 * Downloads Screen listing all active and completed offline anime episodes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    onPlayOffline: (DownloadedEpisode) -> Unit
) {
    val downloadsMap by EpisodeDownloadManager.downloads.collectAsState()
    val episodes = downloadsMap.values.toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Offline Downloads",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No downloaded episodes yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download episodes from an anime page to watch offline.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                items(episodes, key = { "${it.animeId}:${it.episodeNumber}" }) { ep ->
                    DownloadedEpisodeItem(
                        episode = ep,
                        onPlay = { onPlayOffline(ep) },
                        onDelete = {
                            EpisodeDownloadManager.deleteDownloadedEpisode(ep.animeId, ep.episodeNumber)
                        },
                        onCancel = {
                            EpisodeDownloadManager.cancelDownload(ep.animeId, ep.episodeNumber)
                        },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
