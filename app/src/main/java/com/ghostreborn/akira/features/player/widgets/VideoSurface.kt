package com.ghostreborn.akira.features.player.widgets

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(
    exoPlayer: ExoPlayer,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                setResizeMode(resizeMode)
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.setResizeMode(resizeMode)
        },
        modifier = modifier.fillMaxSize()
    )
}
