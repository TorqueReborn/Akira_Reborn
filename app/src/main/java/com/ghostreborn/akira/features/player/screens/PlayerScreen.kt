package com.ghostreborn.akira.features.player.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.viewmodels.PlayerViewModel
import com.ghostreborn.akira.features.player.widgets.EpisodeDrawer
import com.ghostreborn.akira.features.player.widgets.SourceQualityPicker
import com.ghostreborn.akira.features.player.widgets.VideoSurface
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    animeId: String,
    animeTitle: String,
    episodeNumber: String,
    sources: List<StreamSource>,
    thumbnail: String? = null,
    initialPositionMs: Long = 0L,
    episodes: List<String> = emptyList(),
    translationType: String = "sub",
    onNavigateBack: () -> Unit,
    onChangeEpisode: ((String, List<StreamSource>) -> Unit)? = null,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()

    var isLocked by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    LaunchedEffect(animeId, episodeNumber, sources) {
        viewModel.initWithArgs(
            showId = animeId,
            title = animeTitle,
            episodeNumber = episodeNumber,
            sources = sources,
            thumbnailUrl = thumbnail,
            initialPositionMs = initialPositionMs,
            episodes = episodes,
            translationType = translationType
        )
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to MkissaConfigProvider.currentEndpoints.authReferer,
                    "Origin" to MkissaConfigProvider.currentEndpoints.authOrigin
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    fun playCurrentSource(seekMs: Long = 0L) {
        val source = uiState.currentSource ?: return
        isLoading = true
        val mediaItem = MediaItem.fromUri(Uri.parse(source.url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (seekMs > 0) exoPlayer.seekTo(seekMs)
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(uiState.selectedSourceIndex, uiState.sources) {
        if (uiState.sources.isNotEmpty()) {
            playCurrentSource(initialPositionMs)
        }
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> isLoading = false
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                viewModel.setError("Playback error: ${error.message}")
            }
        })
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000L)
            if (exoPlayer.isPlaying && !isSeeking) {
                currentPositionMs = exoPlayer.currentPosition
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (currentPositionMs > 0 && totalDurationMs > 0) {
                    viewModel.saveWatchProgress(context, currentPositionMs, totalDurationMs)
                }
            }
        }
    }

    BackHandler { onNavigateBack() }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    fun formatDuration(ms: Long): String {
        val totalSecs = (ms / 1000).toInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.toggleControlsVisibility() }
    ) {
        VideoSurface(exoPlayer = exoPlayer, resizeMode = resizeMode)

        if (isLoading || uiState.isFetchingEpisode) {
            CircularProgressIndicator(
                color = AkiraPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${uiState.title} - Ep ${uiState.episodeNumber}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.setQualityPickerOpen(true) }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Quality Settings",
                            tint = Color.White
                        )
                    }
                    if (uiState.episodes.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setEpisodeDrawerOpen(true) }) {
                            Icon(
                                imageVector = Icons.Rounded.List,
                                contentDescription = "Episodes",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Play/Pause & Skip Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(target)
                        currentPositionMs = target
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(AkiraPrimary, CircleShape)
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = {
                        val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(totalDurationMs)
                        exoPlayer.seekTo(target)
                        currentPositionMs = target
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Seekbar & Time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatDuration(totalDurationMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs else 0f,
                        onValueChange = { frac ->
                            isSeeking = true
                            currentPositionMs = (frac * totalDurationMs).toLong()
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(currentPositionMs)
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = AkiraPrimary,
                            activeTrackColor = AkiraPrimary,
                            inactiveTrackColor = Color.Gray
                        )
                    )
                }
            }
        }

        if (uiState.isQualityPickerOpen) {
            SourceQualityPicker(
                sources = uiState.sources,
                selectedSourceIndex = uiState.selectedSourceIndex,
                onSourceSelect = { idx -> viewModel.selectSourceIndex(idx) },
                onDismiss = { viewModel.setQualityPickerOpen(false) }
            )
        }

        if (uiState.isEpisodeDrawerOpen) {
            EpisodeDrawer(
                episodes = uiState.episodes,
                currentEpisodeNumber = uiState.episodeNumber,
                isFetching = uiState.isFetchingEpisode,
                onEpisodeSelect = { ep ->
                    viewModel.fetchNextEpisodeStreams(context, ep) { newStreams ->
                        onChangeEpisode?.invoke(ep, newStreams)
                    }
                    viewModel.setEpisodeDrawerOpen(false)
                },
                onDismiss = { viewModel.setEpisodeDrawerOpen(false) }
            )
        }
    }
}
