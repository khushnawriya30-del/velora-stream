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
        onDispose {
            exoPlayer.release()
            player = null
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
        currentPlayer.playWhenReady = true
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
                            showControls = true
                            true
                        }
                        Key.DirectionDown -> {
                            // Next episode
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
                Text("Loading player...", color = TvDimText, fontSize = 20.sp)
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = TvPrimary, fontSize = 18.sp)
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
                        if (state.movie != null && state.episodes.isNotEmpty()) {
                            Text(
                                text = state.movie?.title ?: "",
                                fontSize = 14.sp,
                                color = TvDimText,
                            )
                        }
                    }
                }

                // Bottom controls hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ControlHint("◀◀", "Rewind 10s")
                        ControlHint("⏯", "Play/Pause")
                        ControlHint("▶▶", "Forward 10s")
                        if (state.episodes.isNotEmpty()) {
                            ControlHint("▼", "Next Episode")
                        }
                    }
                }
            }
        }
    }
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
        Text(text = label, fontSize = 12.sp, color = TvDimText)
    }
}
