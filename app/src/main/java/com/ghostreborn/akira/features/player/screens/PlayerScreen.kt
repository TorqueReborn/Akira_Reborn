package com.ghostreborn.akira.features.player.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onNavigateBack: () -> Unit,
    onChangeEpisode: ((String, List<StreamSource>) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val playableSources = remember(sources) {
        val direct = sources.filter { it.isDirect }
        if (direct.isNotEmpty()) direct else sources
    }

    var currentSourceIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isChangingEpisode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    // Double-tap visual indicators
    var doubleTapSeekLeft by remember { mutableStateOf(false) }
    var doubleTapSeekRight by remember { mutableStateOf(false) }

    // Swipe Volume / Brightness overlay
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

    // Resize Mode: Fit (Zoom 0) vs Fill (Zoom 1)
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Playback Speed
    val speedOptions = remember { listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }

    var isFastForwarding2x by remember { mutableStateOf(false) }

    // Immersive landscape orientation lock
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to com.ghostreborn.akira.core.config.MkissaConfigProvider.currentEndpoints.authReferer,
                    "Origin" to com.ghostreborn.akira.core.config.MkissaConfigProvider.currentEndpoints.authOrigin
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    fun playSource(index: Int, seekMs: Long) {
        if (index < 0 || index >= playableSources.size) {
            isLoading = false
            errorMessage = "No playable video source available."
            return
        }

        val source = playableSources[index]
        currentSourceIndex = index
        isLoading = true
        errorMessage = null

        val mediaItem = MediaItem.fromUri(Uri.parse(source.url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
        exoPlayer.prepare()
        if (seekMs > 0) {
            exoPlayer.seekTo(seekMs)
        }
        exoPlayer.playWhenReady = true
    }

    fun switchEpisode(targetEp: String) {
        if (isChangingEpisode || targetEp == episodeNumber) return
        isChangingEpisode = true
        scope.launch {
            try {
                val token = TokenManager.getAccessToken(context)
                val result = AnimeRepository.fetchEpisodeStreams(
                    context = context,
                    showId = animeId,
                    episodeString = targetEp,
                    translationType = translationType,
                    authToken = token
                )
                onChangeEpisode?.invoke(targetEp, result.streams)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load episode $targetEp: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isChangingEpisode = false
            }
        }
    }

    val currentEpIdx = episodes.indexOf(episodeNumber)
    val hasNextEpisode = currentEpIdx != -1 && currentEpIdx + 1 < episodes.size
    val nextEpisodeNumber = if (hasNextEpisode) episodes[currentEpIdx + 1] else null
    val hasPrevEpisode = currentEpIdx > 0
    val prevEpisodeNumber = if (hasPrevEpisode) episodes[currentEpIdx - 1] else null

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isLoading = false
                        if (hasNextEpisode && nextEpisodeNumber != null) {
                            switchEpisode(nextEpisodeNumber)
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                val next = currentSourceIndex + 1
                if (next < playableSources.size) {
                    playSource(next, exoPlayer.currentPosition)
                } else {
                    errorMessage = "Playback failed on all available streams."
                }
            }
        })

        if (playableSources.isNotEmpty()) {
            playSource(0, initialPositionMs)
        } else {
            isLoading = false
            errorMessage = "No video streams found for this episode."
        }
    }

    // Auto-update position & save progress
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000L)
            if (exoPlayer.isPlaying && !isSeeking) {
                currentPositionMs = exoPlayer.currentPosition
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (currentPositionMs > 0 && totalDurationMs > 0) {
                    WatchHistoryManager.saveProgress(
                        context = context,
                        animeId = animeId,
                        animeTitle = animeTitle,
                        episodeNumber = episodeNumber,
                        seekPositionMs = currentPositionMs,
                        totalDurationMs = totalDurationMs,
                        thumbnail = thumbnail
                    )
                }
            }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying, isSeeking, showSpeedDialog, showSourceDialog, showEpisodesDrawer, isLocked) {
        if (showControls && isPlaying && !isSeeking && !showSpeedDialog && !showSourceDialog && !showEpisodesDrawer && !isLocked) {
            delay(4500L)
            showControls = false
        }
    }

    BackHandler {
        onNavigateBack()
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

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
                        showControls = !showControls
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
                    onDragEnd = {
                        showGestureIndicator = null
                    },
                    onDragCancel = {
                        showGestureIndicator = null
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (isLocked) return@detectVerticalDragGestures
                        val isRightSide = change.position.x > size.width / 2
                        val delta = -dragAmount / 350f

                        if (isRightSide) {
                            // Volume control
                            val newVol = (volumeLevel + delta).coerceIn(0f, 1f)
                            volumeLevel = newVol
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (newVol * maxVol).roundToInt(),
                                0
                            )
                        } else {
                            // Brightness control
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewRef = this
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            }
        )

        // Hold-to-2x speed indicator banner
        AnimatedVisibility(
            visible = isFastForwarding2x,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = null,
                        tint = AkiraPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "2X Speed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

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

        AnimatedVisibility(
            visible = doubleTapSeekLeft,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 60.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Rounded.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text(text = "-10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = doubleTapSeekRight,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 60.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Rounded.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text(text = "+10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Gesture HUD Overlay (Brightness & Volume indicator)
        if (showGestureIndicator != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC111019))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showGestureIndicator == "VOLUME") Icons.Rounded.VolumeUp else Icons.Rounded.BrightnessMedium,
                        contentDescription = null,
                        tint = AkiraPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    val pct = if (showGestureIndicator == "VOLUME") (volumeLevel * 100).toInt() else (brightnessLevel * 100).toInt()
                    Text(
                        text = "${showGestureIndicator?.lowercase()?.replaceFirstChar { it.uppercase() }}: $pct%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Loading spinner
        if (isLoading || isChangingEpisode) {
            CircularProgressIndicator(
                color = AkiraPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Error banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0F0C1B))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row {
                        Text(
                            text = "Retry Stream",
                            color = AkiraPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clickable {
                                    playSource(currentSourceIndex, currentPositionMs)
                                }
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Go Back",
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onNavigateBack() }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Lock Toggle Button (always accessible when controls toggle is tapped)
        if (showControls) {
            val lockModifier = if (isLocked) {
                Modifier
                    .background(AkiraLogoGradientBrush)
            } else {
                Modifier
                    .background(Color(0x99110E1C))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            }

            IconButton(
                onClick = { isLocked = !isLocked },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .size(40.dp)
                    .shadow(if (isLocked) 8.dp else 0.dp, CircleShape, spotColor = AkiraPrimary)
                    .clip(CircleShape)
                    .then(lockModifier)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = "Lock Screen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Full Controls Overlay (hidden when locked)
        if (showControls && !isLocked) {
            // Top Bar
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xDD000000), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = animeTitle,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Episode $episodeNumber • ${translationType.uppercase()}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }

                    // Top Action Icons: Resize Mode, Speed, Servers, Episodes
                    IconButton(
                        onClick = {
                            resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Toggle Fit/Fill",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSpeedDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Speed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (playableSources.size > 1) {
                        IconButton(
                            onClick = { showSourceDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Dns,
                                contentDescription = "Servers",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (episodes.isNotEmpty() && onChangeEpisode != null) {
                        IconButton(
                            onClick = { showEpisodesDrawer = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = "Episodes",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Center Play / Pause & Prev / Next
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Ep
                if (hasPrevEpisode && prevEpisodeNumber != null) {
                    IconButton(
                        onClick = { switchEpisode(prevEpisodeNumber) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Prev Episode",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }

                IconButton(
                    onClick = {
                        val pos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                        exoPlayer.seekTo(pos)
                        currentPositionMs = pos
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(12.dp, CircleShape, spotColor = AkiraPrimary)
                        .clip(CircleShape)
                        .background(AkiraLogoGradientBrush)
                        .clickable {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                IconButton(
                    onClick = {
                        val pos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(pos)
                        currentPositionMs = pos
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Fast Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next Ep
                if (hasNextEpisode && nextEpisodeNumber != null) {
                    Spacer(modifier = Modifier.width(20.dp))
                    IconButton(
                        onClick = { switchEpisode(nextEpisodeNumber) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Episode",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Anime "+85s Skip Intro" Floating Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 72.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD121020))
                    .border(1.2.dp, AkiraLogoGradientBrush, CircleShape)
                    .clickable {
                        val pos = (exoPlayer.currentPosition + 85000).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(pos)
                        currentPositionMs = pos
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Skip Intro (+85s)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom Progress Bar & Time with Custom Akira Anime Scrubber
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xF0080610))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Position Time Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x55000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatDuration(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Custom Scrubber Track
                    val progressFraction = remember(currentPositionMs, totalDurationMs) {
                        if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .pointerInput(totalDurationMs) {
                                detectTapGestures { offset ->
                                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                    val newPos = (frac * totalDurationMs).toLong()
                                    currentPositionMs = newPos
                                    exoPlayer.seekTo(newPos)
                                }
                            }
                            .pointerInput(totalDurationMs) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        isSeeking = true
                                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                        currentPositionMs = (frac * totalDurationMs).toLong()
                                    },
                                    onDragEnd = {
                                        isSeeking = false
                                        exoPlayer.seekTo(currentPositionMs)
                                    },
                                    onDragCancel = {
                                        isSeeking = false
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                        currentPositionMs = (frac * totalDurationMs).toLong()
                                    }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val trackWidth = maxWidth
                        val activeWidth = trackWidth * progressFraction

                        // Inactive / Base Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0x40FFFFFF))
                        )

                        // Active Glowing Gradient Track
                        Box(
                            modifier = Modifier
                                .width(activeWidth)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(AkiraLogoGradientBrush)
                        )

                        // Custom Glowing Neon Thumb with Signature Akira Gradient
                        Box(
                            modifier = Modifier
                                .padding(start = (activeWidth - 8.dp).coerceAtLeast(0.dp))
                                .size(18.dp)
                                .shadow(10.dp, CircleShape, spotColor = AkiraPrimary)
                                .clip(CircleShape)
                                .background(AkiraLogoGradientBrush)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner high-contrast bright core
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Total Duration Time Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x55000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatDuration(totalDurationMs),
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Speed Modal Sheet
        if (showSpeedDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable { showSpeedDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1B2E))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Playback Speed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            speedOptions.forEach { spd ->
                                val isSelected = currentSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                            else Modifier.background(Color(0x22FFFFFF))
                                        )
                                        .clickable {
                                            currentSpeed = spd
                                            exoPlayer.playbackParameters = PlaybackParameters(spd)
                                            showSpeedDialog = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Server / Stream Source Picker Modal
        if (showSourceDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable { showSourceDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1B2E))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Select Server", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        playableSources.forEachIndexed { idx, src ->
                            val isSelected = currentSourceIndex == idx
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                        else Modifier.background(Color(0x22FFFFFF))
                                    )
                                    .clickable {
                                        showSourceDialog = false
                                        playSource(idx, exoPlayer.currentPosition)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = src.sourceName.ifEmpty { "Server ${idx + 1}" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Semi-transparent Episodes Side Drawer
        if (showEpisodesDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable { showEpisodesDrawer = false }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .background(Color(0xEE141222))
                        .border(1.dp, Color(0x22FFFFFF))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "Episodes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(episodes) { ep ->
                                val isSelected = ep == episodeNumber
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (isSelected) Modifier.background(AkiraLogoGradientBrush)
                                            else Modifier.background(Color(0x22FFFFFF))
                                        )
                                        .clickable {
                                            showEpisodesDrawer = false
                                            switchEpisode(ep)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = "Episode $ep",
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
