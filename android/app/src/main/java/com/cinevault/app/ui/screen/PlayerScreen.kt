package com.cinevault.app.ui.screen

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private val GoldAccent = Color(0xFFD4AF37)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Force landscape always
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Immersive mode — hide status bar and navigation bar
    DisposableEffect(Unit) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ── ExoPlayer Setup ──
    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setUserAgent("Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .setTrackSelector(trackSelector)
            .build()
            .apply { playWhenReady = true }
    }

    var autoQualityLabel by remember { mutableStateOf("Auto") }
    var playerError by remember { mutableStateOf<String?>(null) }

    // Error & state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("CineVaultPlayer", "Playback error: ${error.errorCodeName} - ${error.message}", error)
                viewModel.onPlaybackError("Playback error: ${error.errorCodeName}\n${error.message ?: "Unknown error"}")
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val dur = exoPlayer.duration.coerceAtLeast(1)
                    viewModel.saveExplicitProgress(dur, dur)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // System back — save progress then navigate
    BackHandler {
        viewModel.saveExplicitProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0))
        onBack()
    }

    // Quality track selector
    LaunchedEffect(uiState.selectedQuality) {
        val quality = uiState.selectedQuality
        if (quality == "auto") {
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                .setForceHighestSupportedBitrate(false)
                .build()
        } else {
            val maxHeight = when (quality) {
                "1080p" -> 1080; "720p" -> 720; "480p" -> 480; "360p" -> 360; else -> Int.MAX_VALUE
            }
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
                .setForceHighestSupportedBitrate(false)
                .setForceLowestBitrate(quality == "360p")
                .build()
        }
    }

    // Auto quality label
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(2000)
            val videoHeight = exoPlayer.videoFormat?.height ?: 0
            autoQualityLabel = when {
                videoHeight >= 1080 -> "Auto (1080p)"
                videoHeight >= 720 -> "Auto (720p)"
                videoHeight >= 480 -> "Auto (480p)"
                videoHeight > 0 -> "Auto (${videoHeight}p)"
                else -> "Auto"
            }
        }
    }

    // Media URL loading
    LaunchedEffect(uiState.streamingUrl) {
        uiState.streamingUrl?.let { url ->
            Log.d("CineVaultPlayer", "Loading URL: $url")
            playerError = null
            exoPlayer.stop()
            val resumePos = uiState.currentPosition
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            if (resumePos > 0) {
                var waitCount = 0
                while (exoPlayer.playbackState != Player.STATE_READY && exoPlayer.playerError == null && waitCount < 300) {
                    delay(100); waitCount++
                }
                if (exoPlayer.playbackState == Player.STATE_READY) exoPlayer.seekTo(resumePos)
            }
        }
    }

    // Playback speed sync
    LaunchedEffect(uiState.playbackSpeed, uiState.isSpeedOverride) {
        exoPlayer.setPlaybackSpeed(if (uiState.isSpeedOverride) 2.0f else uiState.playbackSpeed)
    }

    // Position tracking & audio track discovery
    var audioTracks by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var selectedAudioIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000)
            val dur = exoPlayer.duration.coerceAtLeast(0)
            val pos = exoPlayer.currentPosition
            if (dur > 0) viewModel.onPositionChange(pos, dur)
            viewModel.onPlaybackStateChange(exoPlayer.isPlaying)

            val discovered = mutableListOf<Pair<String, Int>>()
            for (group in exoPlayer.currentTracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language
                        val label = format.label ?: if (lang != null) {
                            val locale = Locale.forLanguageTag(lang)
                            val displayName = locale.getDisplayLanguage(Locale.ENGLISH)
                            if (displayName.isNotBlank() && displayName != lang) displayName else lang.uppercase()
                        } else "Track ${discovered.size + 1}"
                        discovered.add(Pair(label, i))
                    }
                }
            }
            if (discovered.isNotEmpty() && discovered.map { it.first } != audioTracks.map { it.first }) {
                audioTracks = discovered
            }
        }
    }

    // Save progress on leave
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveExplicitProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0))
            exoPlayer.release()
        }
    }

    // Auto-hide controls
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls && uiState.isPlaying) {
            delay(5000)
            viewModel.toggleControls()
        }
    }

    // Double-tap seek feedback
    var doubleTapLabel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(doubleTapLabel) {
        if (doubleTapLabel != null) { delay(800); doubleTapLabel = null }
    }

    // ── UI State ──
    var isLocked by remember { mutableStateOf(false) }
    var showQualityPopup by remember { mutableStateOf(false) }
    var showSpeedPopup by remember { mutableStateOf(false) }
    var showAudioPopup by remember { mutableStateOf(false) }
    var showEpisodePopup by remember { mutableStateOf(false) }

    // Brightness & Volume
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var currentBrightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.let { if (it < 0) 0.5f else it } ?: 0.5f
        )
    }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var isDraggingVolume by remember { mutableStateOf(false) }

    // Auto-hide gesture indicators
    LaunchedEffect(showVolumeIndicator) {
        if (showVolumeIndicator) { delay(1500); showVolumeIndicator = false }
    }
    LaunchedEffect(showBrightnessIndicator) {
        if (showBrightnessIndicator) { delay(1500); showBrightnessIndicator = false }
    }

    // ── Main Layout ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                val screenWidth = size.width
                detectTapGestures(
                    onTap = { viewModel.toggleControls() },
                    onDoubleTap = { offset ->
                        if (offset.x < screenWidth / 2) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                            doubleTapLabel = "-10s"
                        } else {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000)
                            doubleTapLabel = "+10s"
                        }
                    },
                    onLongPress = { viewModel.setSpeedOverride(true) },
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) viewModel.setSpeedOverride(false)
                    }
                }
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                val screenWidth = size.width
                val screenHeight = size.height.toFloat()
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDraggingVolume = offset.x < screenWidth / 2
                        if (isDraggingVolume) showVolumeIndicator = true
                        else showBrightnessIndicator = true
                    },
                    onDragEnd = { },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / screenHeight * 1.5f
                        if (isDraggingVolume) {
                            currentVolume = (currentVolume + delta).coerceIn(0f, 1f)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (currentVolume * maxVolume).toInt(),
                                0,
                            )
                            showVolumeIndicator = true
                        } else {
                            currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                            activity?.window?.let { w ->
                                val params = w.attributes
                                params.screenBrightness = currentBrightness
                                w.attributes = params
                            }
                            showBrightnessIndicator = true
                        }
                    },
                )
            },
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = GoldAccent,
            )
        } else if (uiState.error != null || playerError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    playerError ?: uiState.error ?: "Unknown error",
                    color = Color.White,
                    style = CineVaultTheme.typography.body,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "URL: ${uiState.streamingUrl?.take(80) ?: "none"}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack) {
                    Text("Go Back", color = GoldAccent)
                }
            }
        } else {
            // ── Player View ──
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // ── Volume Indicator (left side) ──
            AnimatedVisibility(
                visible = showVolumeIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp),
            ) {
                GestureIndicator(
                    icon = when {
                        currentVolume == 0f -> Icons.Filled.VolumeOff
                        currentVolume < 0.5f -> Icons.Filled.VolumeDown
                        else -> Icons.Filled.VolumeUp
                    },
                    value = currentVolume,
                )
            }

            // ── Brightness Indicator (right side) ──
            AnimatedVisibility(
                visible = showBrightnessIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp),
            ) {
                GestureIndicator(
                    icon = if (currentBrightness < 0.3f) Icons.Filled.BrightnessLow
                    else Icons.Filled.BrightnessHigh,
                    value = currentBrightness,
                )
            }

            // ── Double-tap feedback ──
            AnimatedVisibility(
                visible = doubleTapLabel != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    doubleTapLabel ?: "",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }

            // ── 2x Speed Indicator ──
            if (uiState.isSpeedOverride) {
                Text(
                    "2x Speed ▸▸",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GoldAccent.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }

            // ── Controls Overlay ──
            if (!isLocked) {
                AnimatedVisibility(
                    visible = uiState.showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        // ── Top Bar ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                viewModel.saveExplicitProgress(
                                    exoPlayer.currentPosition,
                                    exoPlayer.duration.coerceAtLeast(0),
                                )
                                onBack()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    uiState.movie?.title ?: "",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!uiState.currentEpisodeTitle.isNullOrBlank()) {
                                    Text(
                                        uiState.currentEpisodeTitle ?: "",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        // ── Center Controls ──
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp),
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.9f)),
                            ) {
                                Icon(
                                    if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp),
                                )
                            }

                            IconButton(
                                onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000) },
                            ) {
                                Icon(
                                    Icons.Filled.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp),
                                )
                            }
                        }

                        // ── Bottom Section ──
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f),
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 6.dp),
                        ) {
                            // Progress bar
                            Slider(
                                value = if (uiState.totalDuration > 0)
                                    uiState.currentPosition.toFloat() / uiState.totalDuration
                                else 0f,
                                onValueChange = { fraction ->
                                    val seekPos = (fraction * uiState.totalDuration).toLong()
                                    exoPlayer.seekTo(seekPos)
                                    viewModel.seekTo(seekPos)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                ),
                            )

                            // Time labels
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    formatDuration(uiState.currentPosition),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    formatDuration(uiState.totalDuration),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            // ── Toolbar Row ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Speed
                                PlayerToolbarButton(
                                    icon = Icons.Filled.Speed,
                                    label = if (uiState.playbackSpeed == 1.0f) "Speed"
                                    else "${uiState.playbackSpeed}x",
                                    onClick = { showSpeedPopup = true },
                                )

                                // Quality
                                val qualityLabel = when (uiState.selectedQuality) {
                                    "auto" -> {
                                        val resolved = autoQualityLabel
                                            .replace("Auto", "").trim()
                                            .removePrefix("(").removeSuffix(")")
                                        if (resolved.isNotEmpty()) "HD $resolved" else "Auto"
                                    }
                                    "1080p" -> "FHD 1080P"
                                    "720p" -> "HD 720P"
                                    "480p" -> "SD 480P"
                                    "360p" -> "360P"
                                    else -> uiState.selectedQuality
                                }
                                PlayerToolbarButton(
                                    icon = Icons.Filled.HighQuality,
                                    label = qualityLabel,
                                    onClick = { showQualityPopup = true },
                                )

                                // Lock
                                PlayerToolbarButton(
                                    icon = Icons.Filled.Lock,
                                    label = "Lock",
                                    onClick = {
                                        isLocked = true
                                        viewModel.toggleControls()
                                    },
                                )

                                // Audio
                                if (audioTracks.size > 1) {
                                    PlayerToolbarButton(
                                        icon = Icons.Filled.Audiotrack,
                                        label = "Audio",
                                        onClick = { showAudioPopup = true },
                                    )
                                }

                                // Episode buttons (series only)
                                if (uiState.episodes.isNotEmpty()) {
                                    PlayerToolbarButton(
                                        icon = Icons.Filled.GridView,
                                        label = "Episodes",
                                        onClick = { showEpisodePopup = true },
                                    )
                                    PlayerToolbarButton(
                                        icon = Icons.Filled.SkipNext,
                                        label = "Next",
                                        onClick = { viewModel.playNextEpisode() },
                                        enabled = uiState.currentEpisodeIndex < uiState.episodes.size - 1,
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            } else {
                // ── Locked Overlay ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isLocked = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Filled.LockOpen,
                                contentDescription = "Tap to unlock",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tap to unlock",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        // ── Quality Popup ──
        if (showQualityPopup) {
            PlayerPopupOverlay(
                title = "Quality",
                onDismiss = { showQualityPopup = false },
            ) {
                val qualities = if (uiState.isAdaptive) uiState.availableQualities
                else listOf(uiState.selectedQuality)

                qualities.forEach { quality ->
                    val displayLabel = when (quality) {
                        "auto" -> autoQualityLabel
                        "1080p" -> "FHD 1080P"
                        "720p" -> "HD 720P"
                        "480p" -> "SD 480P"
                        "360p" -> "360P"
                        else -> quality
                    }
                    PopupOptionCard(
                        label = displayLabel,
                        isSelected = uiState.selectedQuality == quality,
                        onClick = {
                            viewModel.setQuality(quality)
                            showQualityPopup = false
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Speed Popup ──
        if (showSpeedPopup) {
            PlayerPopupOverlay(
                title = "Playback Speed",
                onDismiss = { showSpeedPopup = false },
            ) {
                listOf(2.0f, 1.5f, 1.25f, 1.0f, 0.75f, 0.5f).forEach { speed ->
                    PopupOptionCard(
                        label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                        isSelected = uiState.playbackSpeed == speed,
                        onClick = {
                            viewModel.setPlaybackSpeed(speed)
                            showSpeedPopup = false
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Audio Popup ──
        if (showAudioPopup && audioTracks.size > 1) {
            PlayerPopupOverlay(
                title = "Audio",
                onDismiss = { showAudioPopup = false },
            ) {
                audioTracks.forEachIndexed { index, (trackName, trackIndex) ->
                    val displayLabel = if (index == 0) "$trackName [Original]" else trackName
                    PopupOptionCard(
                        label = displayLabel,
                        isSelected = selectedAudioIndex == index,
                        onClick = {
                            selectedAudioIndex = index
                            for (group in exoPlayer.currentTracks.groups) {
                                if (group.type == C.TRACK_TYPE_AUDIO) {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(
                                            TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                                        )
                                        .build()
                                    break
                                }
                            }
                            showAudioPopup = false
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Episode Selector Popup ──
        if (showEpisodePopup && uiState.episodes.isNotEmpty()) {
            PlayerPopupOverlay(
                title = "Select Episode",
                onDismiss = { showEpisodePopup = false },
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 300.dp),
                ) {
                    itemsIndexed(uiState.episodes) { index, episode ->
                        val isCurrent = index == uiState.currentEpisodeIndex
                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    viewModel.playEpisode(episode)
                                    showEpisodePopup = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) GoldAccent.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(
                                width = if (isCurrent) 2.dp else 0.5.dp,
                                color = if (isCurrent) GoldAccent
                                else Color.White.copy(alpha = 0.12f),
                            ),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${episode.episodeNumber}",
                                        color = if (isCurrent) GoldAccent else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(14.dp),
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
}

// ── Helper Composables ──

@Composable
private fun GestureIndicator(icon: ImageVector, value: Float) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.8f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoldAccent),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${(value * 100).toInt()}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PlayerToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (enabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerPopupOverlay(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1226),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
            shadowElevation = 24.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
private fun PopupOptionCard(
    label: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GoldAccent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.85f),
                )
                if (subtitle != null) {
                    Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}
