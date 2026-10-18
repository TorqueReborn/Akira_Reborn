package com.ghostreborn.akira.features.player.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.ghostreborn.akira.core.config.MkissaConfigProvider
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.PlaybackFallbackPolicy
import com.ghostreborn.akira.features.player.services.VideoCacheManager
import com.ghostreborn.akira.features.player.viewmodels.PlayerViewModel
import com.ghostreborn.akira.features.player.widgets.CenterPlaybackControls
import com.ghostreborn.akira.features.player.widgets.DoubleTapSeekOverlay
import com.ghostreborn.akira.features.player.widgets.EpisodeDrawer
import com.ghostreborn.akira.features.player.widgets.FastForwardBanner
import com.ghostreborn.akira.features.player.widgets.GestureLevelHud
import com.ghostreborn.akira.features.player.widgets.PlaybackSpeedDialog
import com.ghostreborn.akira.features.player.widgets.PlayerErrorBanner
import com.ghostreborn.akira.features.player.widgets.PlayerLockButton
import com.ghostreborn.akira.features.player.widgets.PlayerScrubberBar
import com.ghostreborn.akira.features.player.widgets.PlayerTopBar
import com.ghostreborn.akira.features.player.widgets.SkipSegmentButton
import com.ghostreborn.akira.features.player.widgets.SourceQualityPicker
import com.ghostreborn.akira.features.player.widgets.VideoSurface
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    malId: Long? = null,
    onNavigateBack: () -> Unit,
    onChangeEpisode: ((String, List<StreamSource>) -> Unit)? = null,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val uiState by viewModel.uiState.collectAsState()
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnChangeEpisode by rememberUpdatedState(onChangeEpisode)
    val latestAnimeId by rememberUpdatedState(animeId)
    val latestAnimeTitle by rememberUpdatedState(animeTitle)
    val latestRouteEpisodeNumber by rememberUpdatedState(episodeNumber)
    val latestThumbnail by rememberUpdatedState(thumbnail)

    var isLocked by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    // Double-tap visual ripples
    var doubleTapSeekLeft by remember { mutableStateOf(false) }
    var doubleTapSeekRight by remember { mutableStateOf(false) }

    // Volume & Brightness swipe indicators
    var volumeLevel by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        )
    }
    var brightnessLevel by remember {
        val windowAttr = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (windowAttr in 0f..1f) windowAttr else 0.5f)
    }
    var showGestureIndicator by remember { mutableStateOf<String?>(null) } // "VOLUME", "BRIGHTNESS"

    // Resize Mode: Fit vs Zoom
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Playback Speed
    val speedOptions = remember { listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var isFastForwarding2x by remember { mutableStateOf(false) }

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
        viewModel.loadSkipTimes(
            title = animeTitle,
            episodeNumber = episodeNumber,
            explicitMalId = malId
        )
    }

    // Reset the local timeline on episode changes so the previous episode's position
    // cannot be applied to the newly selected episode during a navigation transition.
    LaunchedEffect(animeId, episodeNumber, uiState.showId, uiState.episodeNumber, initialPositionMs) {
        val routeMatchesState =
            (uiState.showId.isBlank() || uiState.showId == animeId) &&
                (uiState.episodeNumber.isBlank() || uiState.episodeNumber == episodeNumber)
        currentPositionMs = if (routeMatchesState) initialPositionMs.coerceAtLeast(0L) else 0L
        bufferedPositionMs = 0L
        totalDurationMs = 0L
        isLoading = true
    }

    // Immersive landscape orientation lock & window insets restoration on exit
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
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    // System bars visibility sync
    LaunchedEffect(uiState.isControlsVisible) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (uiState.isControlsVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Keep screen awake while playing
    DisposableEffect(isPlaying) {
        val window = activity?.window
        if (window != null) {
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose { }
    }

    // Initialize ExoPlayer with bounded buffering and a reusable disk cache
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to MkissaConfigProvider.currentEndpoints.authReferer,
                    "Origin" to MkissaConfigProvider.currentEndpoints.authOrigin
                )
            )

        // Cache chunks to disk (prevents RAM OOM and enables instant seek back)
        val cache = VideoCacheManager.getInstance(context)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        // Keep in-memory buffering bounded. The disk cache remains responsible for reusable chunks.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 10_000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
    }

    fun playCurrentSource(seekMs: Long = 0L) {
        val source = uiState.currentSource ?: return
        isLoading = true
        viewModel.setError(null)

        val mediaUri = if (source.url.startsWith("/") || source.url.startsWith("file://")) {
            Uri.fromFile(java.io.File(source.url.removePrefix("file://")))
        } else {
            Uri.parse(source.url)
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(mediaUri)
        when {
            source.isHls || source.fileExtension.equals("m3u8", ignoreCase = true) ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            source.fileExtension.equals("mp4", ignoreCase = true) || source.url.endsWith(".mp4", ignoreCase = true) ->
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
        exoPlayer.prepare()
        if (seekMs > 0L) exoPlayer.seekTo(seekMs)
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(
        uiState.showId,
        uiState.episodeNumber,
        uiState.selectedSourceIndex,
        uiState.sources,
        initialPositionMs
    ) {
        if (uiState.sources.isNotEmpty() && uiState.currentSource != null) {
            playCurrentSource(currentPositionMs)
        }
    }

    val currentEpIdx = episodes.indexOf(uiState.episodeNumber)
    val hasNextEpisode = currentEpIdx != -1 && currentEpIdx + 1 < episodes.size
    val nextEpisodeNumber = if (hasNextEpisode) episodes[currentEpIdx + 1] else null
    val hasPrevEpisode = currentEpIdx > 0
    val prevEpisodeNumber = if (hasPrevEpisode) episodes[currentEpIdx - 1] else null

    val latestHasNextEpisode by rememberUpdatedState(hasNextEpisode)
    val latestNextEpisodeNumber by rememberUpdatedState(nextEpisodeNumber)
    val latestIsSeeking by rememberUpdatedState(isSeeking)
    val latestCurrentPositionMs by rememberUpdatedState(currentPositionMs)
    val latestTotalDurationMs by rememberUpdatedState(totalDurationMs)

    // Reset failed-source tracking when the show, episode, or source list changes.
    val failedSourceIndices = remember(uiState.showId, uiState.episodeNumber, uiState.sources) {
        mutableSetOf<Int>()
    }
    val latestFailedSourceIndices by rememberUpdatedState(failedSourceIndices)
    val lastAutoAdvancedEpisode = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.episodeNumber) {
        // Permit automatic advancement again if the user later revisits this episode.
        lastAutoAdvancedEpisode.value = null
    }

    // Keep one listener attached, but have it read the latest Compose state on every callback.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                        viewModel.setError(null)
                    }
                    Player.STATE_ENDED -> {
                        isLoading = false
                        val state = latestUiState
                        val nextEpisode = latestNextEpisodeNumber
                        if (
                            latestHasNextEpisode &&
                            nextEpisode != null &&
                            !state.isFetchingEpisode &&
                            lastAutoAdvancedEpisode.value != state.episodeNumber
                        ) {
                            lastAutoAdvancedEpisode.value = state.episodeNumber
                            val duration = exoPlayer.duration.coerceAtLeast(0L)
                            if (duration > 0L) {
                                viewModel.saveWatchProgress(
                                    context = context,
                                    positionMs = duration,
                                    durationMs = duration,
                                    showIdOverride = latestAnimeId,
                                    titleOverride = latestAnimeTitle,
                                    episodeNumberOverride = latestRouteEpisodeNumber,
                                    thumbnailOverride = latestThumbnail
                                )
                            }
                            viewModel.fetchNextEpisodeStreams(context, nextEpisode) { newStreams ->
                                latestOnChangeEpisode?.invoke(nextEpisode, newStreams)
                            }
                        }
                    }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    currentPositionMs = position
                    totalDurationMs = duration
                    if (position > 0L && duration > 0L) {
                        viewModel.saveWatchProgress(
                            context = context,
                            positionMs = position,
                            durationMs = duration,
                            showIdOverride = latestAnimeId,
                            titleOverride = latestAnimeTitle,
                            episodeNumberOverride = latestRouteEpisodeNumber,
                            thumbnailOverride = latestThumbnail
                        )
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                val state = latestUiState
                val failedIndex = state.selectedSourceIndex
                val failedIndices = latestFailedSourceIndices
                failedIndices.add(failedIndex)

                val nextIndex = PlaybackFallbackPolicy.nextUntriedDirectSourceIndex(
                    sources = state.sources,
                    failedSourceIndices = failedIndices
                )

                if (nextIndex != null) {
                    val failedName = state.sources.getOrNull(failedIndex)?.sourceName
                        ?.takeIf { it.isNotBlank() } ?: "Current source"
                    val nextName = state.sources.getOrNull(nextIndex)?.sourceName
                        ?.takeIf { it.isNotBlank() } ?: "Server ${nextIndex + 1}"
                    Toast.makeText(
                        context,
                        "$failedName failed. Trying $nextName…",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.selectSourceIndex(nextIndex)
                } else {
                    viewModel.setError(
                        "All directly playable streams failed. Try Retry Stream, choose another server, or check your connection."
                    )
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            val latestPosition = exoPlayer.currentPosition.takeIf { it > 0L } ?: latestCurrentPositionMs
            val latestDuration = exoPlayer.duration.takeIf { it > 0L } ?: latestTotalDurationMs
            if (latestPosition > 0L && latestDuration > 0L) {
                viewModel.saveWatchProgress(
                    context = context,
                    positionMs = latestPosition,
                    durationMs = latestDuration,
                    showIdOverride = latestAnimeId,
                    titleOverride = latestAnimeTitle,
                    episodeNumberOverride = latestRouteEpisodeNumber,
                    thumbnailOverride = latestThumbnail
                )
            }
            exoPlayer.release()
        }
    }

    // Update the seek-bar smoothly, but persist progress only every five seconds.
    // Frequent SharedPreferences writes on every half-second poll add avoidable I/O.
    LaunchedEffect(exoPlayer) {
        var lastSavedAtMs = SystemClock.elapsedRealtime()
        while (true) {
            delay(500L)
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            if (exoPlayer.isPlaying && !latestIsSeeking) {
                val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                currentPositionMs = position
                totalDurationMs = duration
                viewModel.updatePlaybackProgress(
                    currentPositionMs = position,
                    durationMs = duration,
                    bufferedPercentage = exoPlayer.bufferedPercentage,
                    isPlaying = exoPlayer.isPlaying
                )

                val now = SystemClock.elapsedRealtime()
                if (position > 0L && duration > 0L && now - lastSavedAtMs >= 5_000L) {
                    viewModel.saveWatchProgress(
                        context = context,
                        positionMs = position,
                        durationMs = duration,
                        showIdOverride = latestAnimeId,
                        titleOverride = latestAnimeTitle,
                        episodeNumberOverride = latestRouteEpisodeNumber,
                        thumbnailOverride = latestThumbnail
                    )
                    lastSavedAtMs = now
                }
            }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(uiState.isControlsVisible, isPlaying, isSeeking, showSpeedDialog, uiState.isQualityPickerOpen, uiState.isEpisodeDrawerOpen, isLocked) {
        if (uiState.isControlsVisible && isPlaying && !isSeeking && !showSpeedDialog && !uiState.isQualityPickerOpen && !uiState.isEpisodeDrawerOpen && !isLocked) {
            delay(4500L)
            viewModel.toggleControlsVisibility(false)
        }
    }

    BackHandler { onNavigateBack() }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        viewModel.toggleControlsVisibility()
                    },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val isRightSide = offset.x > size.width / 2
                        if (isRightSide) {
                            val nextPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(nextPos)
                            currentPositionMs = nextPos
                            doubleTapSeekRight = true
                        } else {
                            val prevPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                            exoPlayer.seekTo(prevPos)
                            currentPositionMs = prevPos
                            doubleTapSeekLeft = true
                        }
                    },
                    onLongPress = {
                        if (isLocked) return@detectTapGestures
                        isFastForwarding2x = true
                        exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                    }
                )
            }
            .pointerInput(isLocked) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (isLocked) return@detectVerticalDragGestures
                        val isRightSide = offset.x > size.width / 2
                        showGestureIndicator = if (isRightSide) "VOLUME" else "BRIGHTNESS"
                    },
                    onDragEnd = { showGestureIndicator = null },
                    onDragCancel = { showGestureIndicator = null },
                    onVerticalDrag = { change, dragAmount ->
                        if (isLocked) return@detectVerticalDragGestures
                        val isRightSide = change.position.x > size.width / 2
                        val delta = -dragAmount / 350f

                        if (isRightSide) {
                            val newVol = (volumeLevel + delta).coerceIn(0f, 1f)
                            volumeLevel = newVol
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (newVol * maxVol).roundToInt(),
                                0
                            )
                        } else {
                            val newBrt = (brightnessLevel + delta).coerceIn(0.01f, 1f)
                            brightnessLevel = newBrt
                            activity?.window?.let { win ->
                                val lp = win.attributes
                                lp.screenBrightness = newBrt
                                win.attributes = lp
                            }
                        }
                    }
                )
            }
    ) {
        VideoSurface(exoPlayer = exoPlayer, resizeMode = resizeMode)

        FastForwardBanner(
            visible = isFastForwarding2x,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Double Tap Seek Ripple Overlays
        LaunchedEffect(doubleTapSeekLeft) {
            if (doubleTapSeekLeft) {
                delay(650L)
                doubleTapSeekLeft = false
            }
        }
        LaunchedEffect(doubleTapSeekRight) {
            if (doubleTapSeekRight) {
                delay(650L)
                doubleTapSeekRight = false
            }
        }

        DoubleTapSeekOverlay(
            isSeekLeft = true,
            visible = doubleTapSeekLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 60.dp)
        )

        DoubleTapSeekOverlay(
            isSeekLeft = false,
            visible = doubleTapSeekRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 60.dp)
        )

        // Swipe HUD (Volume & Brightness)
        GestureLevelHud(
            indicatorType = showGestureIndicator,
            volumeLevel = volumeLevel,
            brightnessLevel = brightnessLevel,
            modifier = Modifier.align(Alignment.Center)
        )

        // Loading spinner
        if (isLoading || uiState.isFetchingEpisode) {
            CircularProgressIndicator(
                color = AkiraPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Error banner
        if (uiState.errorMessage != null) {
            PlayerErrorBanner(
                errorMessage = uiState.errorMessage ?: "",
                onRetry = {
                    latestFailedSourceIndices.clear()
                    viewModel.setError(null)
                    playCurrentSource(currentPositionMs)
                },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Lock Toggle Button
        if (uiState.isControlsVisible) {
            PlayerLockButton(
                isLocked = isLocked,
                onToggleLock = { isLocked = !isLocked },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // Full Controls Overlay
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                PlayerTopBar(
                    title = uiState.title,
                    episodeNumber = uiState.episodeNumber,
                    translationType = uiState.translationType,
                    onNavigateBack = onNavigateBack,
                    onToggleResizeMode = {
                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    onOpenSpeedDialog = { showSpeedDialog = true },
                    onOpenSourcePicker = { viewModel.setQualityPickerOpen(true) },
                    onOpenEpisodeDrawer = { viewModel.setEpisodeDrawerOpen(true) },
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Centered Play / Pause & Skip Controls
                CenterPlaybackControls(
                    isPlaying = isPlaying,
                    hasPrevEpisode = hasPrevEpisode,
                    prevEpisodeNumber = prevEpisodeNumber,
                    hasNextEpisode = hasNextEpisode,
                    nextEpisodeNumber = nextEpisodeNumber,
                    onPlayPauseToggle = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onRewind10s = {
                        val pos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                        exoPlayer.seekTo(pos)
                        currentPositionMs = pos
                    },
                    onForward10s = {
                        val pos = (exoPlayer.currentPosition + 10000).coerceAtMost(totalDurationMs)
                        exoPlayer.seekTo(pos)
                        currentPositionMs = pos
                    },
                    onPrevEpisode = {
                        if (prevEpisodeNumber != null) {
                            viewModel.fetchNextEpisodeStreams(context, prevEpisodeNumber) { newStreams ->
                                onChangeEpisode?.invoke(prevEpisodeNumber, newStreams)
                            }
                        }
                    },
                    onNextEpisode = {
                        if (nextEpisodeNumber != null) {
                            viewModel.fetchNextEpisodeStreams(context, nextEpisodeNumber) { newStreams ->
                                onChangeEpisode?.invoke(nextEpisodeNumber, newStreams)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                // Skip Intro / Outro Pill
                SkipSegmentButton(
                    activeInterval = uiState.activeSkipInterval,
                    onSkip = { targetPos ->
                        exoPlayer.seekTo(targetPos)
                        currentPositionMs = targetPos
                    },
                    currentPositionMs = currentPositionMs,
                    totalDurationMs = totalDurationMs,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )

                // Bottom Scrubber Bar & Timers
                PlayerScrubberBar(
                    currentPositionMs = currentPositionMs,
                    bufferedPositionMs = bufferedPositionMs,
                    totalDurationMs = totalDurationMs,
                    onSeekStart = { isSeeking = true },
                    onSeekProgress = { newPos: Long -> currentPositionMs = newPos },
                    onSeekComplete = { newPos: Long ->
                        isSeeking = false
                        currentPositionMs = newPos
                        exoPlayer.seekTo(newPos)
                    },
                    formatDuration = ::formatDuration,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        // Speed Modal Sheet
        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                speedOptions = speedOptions,
                currentSpeed = currentSpeed,
                onSelectSpeed = { spd ->
                    currentSpeed = spd
                    exoPlayer.playbackParameters = PlaybackParameters(spd)
                    showSpeedDialog = false
                },
                onDismiss = { showSpeedDialog = false }
            )
        }

        // Quality / Server Picker Dialog
        if (uiState.isQualityPickerOpen) {
            SourceQualityPicker(
                sources = uiState.sources,
                selectedSourceIndex = uiState.selectedSourceIndex,
                onSourceSelect = { idx ->
                    latestFailedSourceIndices.clear()
                    viewModel.selectSourceIndex(idx)
                },
                onDismiss = { viewModel.setQualityPickerOpen(false) }
            )
        }

        // Semi-transparent Episodes Side Drawer
        if (uiState.isEpisodeDrawerOpen) {
            EpisodeDrawer(
                episodes = uiState.episodes,
                currentEpisodeNumber = uiState.episodeNumber,
                isFetching = uiState.isFetchingEpisode,
                onEpisodeSelect = { ep ->
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    if (position > 0L && duration > 0L) {
                        viewModel.saveWatchProgress(
                            context = context,
                            positionMs = position,
                            durationMs = duration,
                            showIdOverride = animeId,
                            titleOverride = animeTitle,
                            episodeNumberOverride = episodeNumber,
                            thumbnailOverride = thumbnail
                        )
                    }
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
