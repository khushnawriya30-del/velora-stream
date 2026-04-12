package com.cinevault.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.RankingViewModel
import kotlinx.coroutines.launch
import com.cinevault.app.ui.theme.LocalAppDimens

// ═══════════════════════════════════════════════════════════════
// RANKING SCREEN — Download Rank / Rating Rank
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankingScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: RankingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Tab for rank type: Download Rank / Rating Rank
    val rankTypePager = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalAppDimens.current.pad8, vertical = LocalAppDimens.current.pad12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = CineVaultTheme.colors.textPrimary)
            }

            // Download Rank / Rating Rank tabs
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
            ) {
                RankTypeTab(
                    label = "Download Rank",
                    selected = rankTypePager.currentPage == 0,
                    onClick = { scope.launch { rankTypePager.animateScrollToPage(0) } },
                )
                Spacer(Modifier.width(LocalAppDimens.current.pad16))
                RankTypeTab(
                    label = "Rating Rank",
                    selected = rankTypePager.currentPage == 1,
                    onClick = { scope.launch { rankTypePager.animateScrollToPage(1) } },
                )
            }

            Spacer(Modifier.width(48.dp)) // Balance the back button
        }

        // ── Content Type Tabs ──
        val contentTypes = listOf("Movies" to null, "Tv shows" to "web_series", "Reality Shows" to "tv_show", "Anime" to "anime")
        ScrollableTabRow(
            selectedTabIndex = contentTypes.indexOfFirst { it.second == uiState.selectedContentType }.coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = CineVaultTheme.colors.textPrimary,
            edgePadding = 12.dp,
            divider = {},
            indicator = {},
        ) {
            contentTypes.forEach { (label, type) ->
                Tab(
                    selected = uiState.selectedContentType == type,
                    onClick = { viewModel.setContentType(type) },
                    text = {
                        Text(
                            label,
                            fontWeight = if (uiState.selectedContentType == type) FontWeight.Bold else FontWeight.Normal,
                            fontSize = LocalAppDimens.current.font13,
                        )
                    },
                    selectedContentColor = CineVaultTheme.colors.textPrimary,
                    unselectedContentColor = CineVaultTheme.colors.textSecondary,
                )
            }
        }

        // ── Genre Chips ──
        val genres = listOf("Action", "Comedy", "Mystery", "Drama", "Erotic", "Thriller", "Horror", "Romance", "Documentary", "Cartoon", "Science fiction", "Others")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
            contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad12, vertical = LocalAppDimens.current.pad8),
        ) {
            items(genres) { genre ->
                val selected = uiState.selectedGenre == genre
                Text(
                    text = genre,
                    fontSize = LocalAppDimens.current.font12,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.Black else CineVaultTheme.colors.textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) CineVaultTheme.colors.accentGold else Color.Transparent)
                        .border(1.dp, if (selected) Color.Transparent else Color(0xFF3A3A3A), RoundedCornerShape(20.dp))
                        .clickable { viewModel.setGenre(genre) }
                        .padding(horizontal = LocalAppDimens.current.pad14, vertical = LocalAppDimens.current.pad8),
                )
            }
        }

        // ── Rank Type Page Content ──
        LaunchedEffect(rankTypePager.currentPage) {
            viewModel.setRankType(if (rankTypePager.currentPage == 0) "download" else "rating")
        }

        HorizontalPager(
            state = rankTypePager,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val items = uiState.items
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CineVaultTheme.colors.accentGold)
                }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No rankings available", color = CineVaultTheme.colors.textSecondary)
                }
            } else {
                RankingContent(items = items, onMovieClick = onMovieClick)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RANK TYPE TAB
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RankTypeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val emoji = if (label.contains("Download")) "🔥" else "🔥"
    Text(
        text = "$emoji $label $emoji",
        fontSize = LocalAppDimens.current.font14,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textMuted,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = LocalAppDimens.current.pad4, vertical = LocalAppDimens.current.pad4),
    )
}

// ═══════════════════════════════════════════════════════════════
// RANKING CONTENT — Top 3 Podium + List
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RankingContent(
    items: List<MovieDto>,
    onMovieClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalAppDimens.current.pad24),
    ) {
        // Top 3 Podium
        if (items.size >= 3) {
            item {
                PodiumSection(
                    first = items[0],
                    second = items.getOrNull(1),
                    third = items.getOrNull(2),
                    onMovieClick = onMovieClick,
                )
            }
        }

        // Remaining items (4+)
        val remaining = if (items.size > 3) items.subList(3, items.size) else emptyList()
        itemsIndexed(remaining) { index, movie ->
            RankingListItem(
                rank = index + 4,
                movie = movie,
                onClick = { onMovieClick(movie.id) },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PODIUM SECTION — Top 3 with elevated center
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PodiumSection(
    first: MovieDto,
    second: MovieDto?,
    third: MovieDto?,
    onMovieClick: (String) -> Unit,
) {
    // Shimmer animation for gold glow
    val infiniteTransition = rememberInfiniteTransition(label = "podiumGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalAppDimens.current.pad8, vertical = LocalAppDimens.current.pad12)
            .drawBehind {
                // Subtle gold glow behind podium
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD4AF37).copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2, size.height * 0.4f),
                        radius = size.width * 0.5f,
                    ),
                )
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            // #2 - Left (shorter)
            if (second != null) {
                PodiumCard(
                    movie = second,
                    rank = 2,
                    posterHeight = 150.dp,
                    onClick = { onMovieClick(second.id) },
                )
            }

            // #1 - Center (tallest)
            PodiumCard(
                movie = first,
                rank = 1,
                posterHeight = 190.dp,
                onClick = { onMovieClick(first.id) },
            )

            // #3 - Right (shortest)
            if (third != null) {
                PodiumCard(
                    movie = third,
                    rank = 3,
                    posterHeight = 140.dp,
                    onClick = { onMovieClick(third.id) },
                )
            }
        }
    }

    // Decorative hands/fire emoji row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("2", fontSize = LocalAppDimens.current.font28, fontWeight = FontWeight.Bold, color = CineVaultTheme.colors.textMuted.copy(alpha = 0.4f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("👏", fontSize = LocalAppDimens.current.font16)
            Text("1", fontSize = LocalAppDimens.current.font28, fontWeight = FontWeight.Bold, color = CineVaultTheme.colors.accentGold.copy(alpha = 0.4f))
            Text("👏", fontSize = LocalAppDimens.current.font16)
        }
        Text("3", fontSize = LocalAppDimens.current.font28, fontWeight = FontWeight.Bold, color = CineVaultTheme.colors.textMuted.copy(alpha = 0.4f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumCard(
    movie: MovieDto,
    rank: Int,
    posterHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val isFirst = rank == 1
    val borderColor = when (rank) {
        1 -> CineVaultTheme.colors.accentGold
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32) // Bronze
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (isFirst) 130.dp else 110.dp)
            .clickable(onClick = onClick),
    ) {
        Box {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(posterHeight)
                    .clip(RoundedCornerShape(LocalAppDimens.current.radius8))
                    .border(
                        width = if (isFirst) 2.dp else 1.dp,
                        color = borderColor.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(LocalAppDimens.current.radius8),
                    ),
                contentScale = ContentScale.Crop,
            )

            // TOP badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isFirst) listOf(Color(0xFFD4AF37), Color(0xFFA8892C))
                            else listOf(Color(0xFF3A3A3A), Color(0xFF2A2A2A)),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = LocalAppDimens.current.pad12, vertical = LocalAppDimens.current.padTiny),
            ) {
                Text(
                    "TOP $rank",
                    fontSize = LocalAppDimens.current.font10,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isFirst) Color.Black else CineVaultTheme.colors.textPrimary,
                )
            }
        }

        Spacer(Modifier.height(LocalAppDimens.current.pad16))

        Text(
            text = movie.title,
            fontSize = LocalAppDimens.current.font11,
            fontWeight = FontWeight.Medium,
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// RANKING LIST ITEM (4th onward)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RankingListItem(
    rank: Int,
    movie: MovieDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank number with bar chart icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp),
        ) {
            Text(
                "$rank",
                fontSize = LocalAppDimens.current.font16,
                fontWeight = FontWeight.Bold,
                color = CineVaultTheme.colors.textMuted,
            )
            Icon(
                Icons.Filled.Equalizer,
                contentDescription = null,
                tint = CineVaultTheme.colors.textMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp),
            )
        }

        Spacer(Modifier.width(LocalAppDimens.current.pad12))

        // Poster
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier
                .width(80.dp)
                .height(110.dp)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius6)),
            contentScale = ContentScale.Crop,
        )

        Spacer(Modifier.width(LocalAppDimens.current.pad12))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                movie.title,
                style = CineVaultTheme.typography.body,
                fontWeight = FontWeight.SemiBold,
                color = CineVaultTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(LocalAppDimens.current.pad4))

            val infoLine = buildList {
                movie.starRating?.takeIf { it > 0 }?.let { add(String.format("%.1f", it)) }
                    ?: movie.rating?.takeIf { it > 0 }?.let { add(String.format("%.1f", it)) }
                movie.country?.let { add(it) }
                movie.releaseYear?.let { add(it.toString()) }
                movie.genres?.firstOrNull()?.let { add(it) }
            }.joinToString(" / ")

            if (infoLine.isNotEmpty()) {
                Text(
                    infoLine,
                    style = CineVaultTheme.typography.bodySmall,
                    color = CineVaultTheme.colors.accentGold,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(LocalAppDimens.current.pad4))

            movie.synopsis?.let { desc ->
                Text(
                    desc,
                    style = CineVaultTheme.typography.bodySmall,
                    color = CineVaultTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = LocalAppDimens.current.lineHeight16,
                )
            }
        }
    }
}
