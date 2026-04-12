package com.cinevault.app.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.data.model.WatchProgressDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.ProfileViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    onBack: () -> Unit,
    /** Navigate to content from history. For movies: episodeId = null. For episodes: episodeId = the episode's id, contentId = seriesId. */
    onHistoryItemClick: (contentId: String, episodeId: String?) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Watch History",
                    style = CineVaultTheme.typography.sectionTitle,
                    color = CineVaultTheme.colors.textPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CineVaultTheme.colors.textPrimary
                    )
                }
            },
            actions = {
                if (uiState.watchHistory.isNotEmpty()) {
                    IconButton(onClick = {
                        uiState.watchHistory.forEach { item ->
                            item.id?.let { viewModel.deleteHistoryItem(it) }
                        }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = CineVaultTheme.colors.textSecondary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CineVaultTheme.colors.background
            ),
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CineVaultTheme.colors.accentGold,
                    strokeWidth = LocalAppDimens.current.strokeWidth
                )
            }
        } else if (uiState.watchHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16),
                    modifier = Modifier.padding(40.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = CineVaultTheme.colors.surface,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = CineVaultTheme.colors.textMuted,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        "No Watch History",
                        fontSize = LocalAppDimens.current.font20,
                        fontWeight = FontWeight.Bold,
                        color = CineVaultTheme.colors.textPrimary,
                    )
                    Text(
                        "Movies and shows you've watched\nwill appear here.",
                        fontSize = LocalAppDimens.current.font14,
                        color = CineVaultTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = LocalAppDimens.current.lineHeight22
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad12),
                verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10)
            ) {
                items(uiState.watchHistory) { progress ->
                    WatchHistoryItem(
                        progress = progress,
                        onClick = {
                            if (progress.contentType == "episode" && !progress.seriesId.isNullOrBlank()) {
                                onHistoryItemClick(progress.seriesId, progress.contentId)
                            } else {
                                onHistoryItemClick(progress.contentId, null)
                            }
                        },
                        onDelete = { progress.id?.let { viewModel.deleteHistoryItem(it) } },
                    )
                }
                item { Spacer(modifier = Modifier.height(LocalAppDimens.current.pad20)) }
            }
        }
    }
}

@Composable
private fun WatchHistoryItem(
    progress: WatchProgressDto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CineVaultTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(LocalAppDimens.current.pad10),
        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with progress bar
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius10))
                .background(CineVaultTheme.colors.background)
        ) {
            AsyncImage(
                model = progress.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play icon overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(30.dp)
                )
            }

            // Progress bar at bottom
            val fraction = if (progress.duration > 0) progress.position.toFloat() / progress.duration else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .background(CineVaultTheme.colors.accentGold)
                )
            }
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad4)
        ) {
            Text(
                progress.contentTitle ?: progress.episodeTitle ?: "Unknown",
                fontSize = LocalAppDimens.current.font14,
                fontWeight = FontWeight.SemiBold,
                color = CineVaultTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val watchedMin = (progress.position / 60).coerceAtLeast(0)
            val totalMin = (progress.duration / 60).coerceAtLeast(1)
            val remainingMin = ((progress.duration - progress.position) / 60).coerceAtLeast(0)
            Text(
                "${watchedMin}m / ${totalMin}m watched",
                fontSize = LocalAppDimens.current.font12,
                color = CineVaultTheme.colors.textSecondary,
            )
            if (remainingMin > 0) {
                Text(
                    "${remainingMin}m remaining",
                    fontSize = LocalAppDimens.current.font11,
                    color = CineVaultTheme.colors.accentGold,
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CineVaultTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
