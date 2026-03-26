package com.cinevault.app.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Force landscape in full screen
    val activity = context as? Activity
    DisposableEffect(uiState.isFullscreen) {
        activity?.requestedOrientation = if (uiState.isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Update media when URL changes
    LaunchedEffect(uiState.streamingUrl) {
        uiState.streamingUrl?.let { url ->
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
        }
    }

    // Sync playback speed
    LaunchedEffect(uiState.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(uiState.playbackSpeed)
    }

    // Track position
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000)
            if (exoPlayer.isPlaying) {
                viewModel.onPositionChange(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0))
            }
            viewModel.onPlaybackStateChange(exoPlayer.isPlaying)
        }
    }

    // Save progress on leave
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgressNow()
            exoPlayer.release()
        }
    }

    // Auto-hide controls
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls && uiState.isPlaying) {
            delay(4000)
            viewModel.toggleControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { viewModel.toggleControls() },
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CineVaultTheme.colors.accentGold,
            )
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(uiState.error!!, color = CineVaultTheme.colors.textSecondary, style = CineVaultTheme.typography.body)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onBack) {
                    Text("Go Back", color = CineVaultTheme.colors.accentGold)
                }
            }
        } else {
            // Player view
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Custom controls overlay
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
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            viewModel.saveProgressNow()
                            onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiState.movie?.title ?: "",
                                style = CineVaultTheme.typography.body,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Settings dropdown
                        var showSettings by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSettings,
                                onDismissRequest = { showSettings = false },
                            ) {
                                // Quality
                                Text("Quality", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = CineVaultTheme.typography.label)
                                uiState.availableQualities.forEach { quality ->
                                    DropdownMenuItem(
                                        text = { Text(quality) },
                                        onClick = {
                                            viewModel.setQuality(quality)
                                            showSettings = false
                                        },
                                        leadingIcon = {
                                            if (uiState.selectedQuality == quality) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                    )
                                }
                                @Suppress("DEPRECATION")
                                Divider()
                                // Speed
                                Text("Speed", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = CineVaultTheme.typography.label)
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            viewModel.setPlaybackSpeed(speed)
                                            showSettings = false
                                        },
                                        leadingIcon = {
                                            if (uiState.playbackSpeed == speed) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Center controls (play/pause, skip)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0)) }) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CineVaultTheme.colors.accentGold.copy(alpha = 0.9f)),
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000) }) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    // Bottom controls (progress bar, time, fullscreen)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Slider(
                            value = if (uiState.totalDuration > 0) uiState.currentPosition.toFloat() / uiState.totalDuration else 0f,
                            onValueChange = { fraction ->
                                val seekPos = (fraction * uiState.totalDuration).toLong()
                                exoPlayer.seekTo(seekPos)
                                viewModel.seekTo(seekPos)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = CineVaultTheme.colors.accentGold,
                                activeTrackColor = CineVaultTheme.colors.accentGold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatDuration(uiState.currentPosition),
                                style = CineVaultTheme.typography.mono,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                formatDuration(uiState.totalDuration),
                                style = CineVaultTheme.typography.mono,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            IconButton(onClick = { viewModel.toggleFullscreen() }) {
                                Icon(
                                    if (uiState.isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
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
