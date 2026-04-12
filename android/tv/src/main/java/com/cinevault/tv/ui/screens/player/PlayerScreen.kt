package com.cinevault.tv.ui.screens.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cinevault.tv.ui.theme.*
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var showControls by remember { mutableStateOf(true) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // Create player
    DisposableEffect(Unit) {
        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        })
        onDispose {
            // Save final progress
            val p = player
            if (p != null && p.duration > 0) {
                viewModel.saveProgress(p.currentPosition, p.duration)
            }
            exoPlayer.release()
            player = null
        }
    }

    // Periodic progress save
    LaunchedEffect(player, state.streamUrl) {
        while (true) {
            delay(10_000) // Update every 10 seconds
            val p = player ?: continue
            if (p.isPlaying && p.duration > 0) {
                currentPositionMs = p.currentPosition
                totalDurationMs = p.duration
                viewModel.saveProgress(p.currentPosition, p.duration)
            }
        }
    }

    // Load stream URL
    LaunchedEffect(state.streamUrl) {
        val currentPlayer = player ?: return@LaunchedEffect
        val url = state.streamUrl ?: return@LaunchedEffect

        if (state.isHls) {
            val dataSourceFactory = OkHttpDataSource.Factory(OkHttpClient())
            val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
            currentPlayer.setMediaSource(hlsSource)
        } else {
            currentPlayer.setMediaItem(MediaItem.fromUri(url))
        }
        currentPlayer.prepare()

        // Resume from last position
        if (state.resumePositionMs > 0) {
            currentPlayer.seekTo(state.resumePositionMs)
        }

        currentPlayer.playWhenReady = true
    }

    // Position tracking for UI
    LaunchedEffect(player) {
        while (true) {
            delay(1000)
            val p = player ?: continue
            currentPositionMs = p.currentPosition
            totalDurationMs = if (p.duration > 0) p.duration else 0
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Back -> {
                            onBack()
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            val p = player
                            if (p != null) {
                                if (p.isPlaying) p.pause() else p.play()
                                showControls = true
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            player?.let { it.seekTo(it.currentPosition + 10_000) }
                            showControls = true
                            true
                        }
                        Key.DirectionLeft -> {
                            player?.let { it.seekTo(maxOf(0, it.currentPosition - 10_000)) }
                            showControls = true
                            true
                        }
                        Key.DirectionUp -> {
                            if (state.episodes.isNotEmpty()) {
                                viewModel.playPreviousEpisode()
                            }
                            showControls = true
                            true
                        }
                        Key.DirectionDown -> {
                            if (state.episodes.isNotEmpty()) {
                                viewModel.playNextEpisode()
                            }
                            showControls = true
                            true
                        }
                        else -> {
                            showControls = true
                            false
                        }
                    }
                } else false
            },
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading player...", color = TvTextMuted, fontSize = 20.sp)
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = TvError, fontSize = 18.sp)
            }
        } else {
            // Player view
            player?.let { exoPlayer ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Overlay controls
            if (showControls) {
                // Top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    Column {
                        Text(
                            text = state.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        state.episodeTitle?.let {
                            Text(text = it, fontSize = 14.sp, color = TvOnSurfaceVariant)
                        }
                        if (state.episodeTitle == null && state.movie != null) {
                            Text(
                                text = state.movie?.title ?: "",
                                fontSize = 14.sp,
                                color = TvOnSurfaceVariant,
                            )
                        }
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                ) {
                    // Progress bar
                    if (totalDurationMs > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                fontSize = 12.sp,
                                color = TvOnSurfaceVariant,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(TvSurfaceVariant)
                            ) {
                                val progress = (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(TvPrimary)
                                )
                            }
                            Text(
                                text = formatTime(totalDurationMs),
                                fontSize = 12.sp,
                                color = TvOnSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Control hints
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ControlHint("\u25C0\u25C0", "Rewind 10s")
                        ControlHint(if (isPlaying) "\u23F8" else "\u25B6", "Play/Pause")
                        ControlHint("\u25B6\u25B6", "Forward 10s")
                        if (state.episodes.isNotEmpty()) {
                            ControlHint("\u25B2", "Prev Episode")
                            ControlHint("\u25BC", "Next Episode")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlHint(icon: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(TvSurfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(text = icon, fontSize = 14.sp, color = Color.White)
        }
        Text(text = label, fontSize = 12.sp, color = TvOnSurfaceVariant)
    }
}
