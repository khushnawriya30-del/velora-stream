package com.cinevault.app.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.data.model.PopularSearchDto
import com.cinevault.app.ui.components.*
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.SearchViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Load search screen data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadSearchScreenData()
        viewModel.loadRecentSearches(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
            .statusBarsPadding(),
    ) {
        // ── Search Bar + Cancel ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalAppDimens.current.pad12, vertical = LocalAppDimens.current.pad8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Search movies, series...", color = CineVaultTheme.colors.textSecondary)
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = CineVaultTheme.colors.textSecondary)
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = CineVaultTheme.colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.search()
                    viewModel.saveRecentSearch(context)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CineVaultTheme.colors.accentGold,
                    unfocusedBorderColor = CineVaultTheme.colors.border,
                    cursorColor = CineVaultTheme.colors.accentGold,
                    focusedTextColor = CineVaultTheme.colors.textPrimary,
                    unfocusedTextColor = CineVaultTheme.colors.textPrimary,
                ),
            )
            TextButton(onClick = {
                viewModel.onQueryChange("")
                focusManager.clearFocus()
            }) {
                Text("Cancel", color = CineVaultTheme.colors.textSecondary)
            }
        }

        // ── Autocomplete overlay ──
        if (uiState.autocomplete.isNotEmpty() && uiState.query.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppDimens.current.pad16),
                shape = RoundedCornerShape(12.dp),
                color = CineVaultTheme.colors.surface,
                tonalElevation = 4.dp,
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(uiState.autocomplete) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onQueryChange(item.title)
                                    viewModel.search()
                                    viewModel.saveRecentSearch(context)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad12),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = CineVaultTheme.colors.textSecondary)
                            Spacer(Modifier.width(LocalAppDimens.current.pad12))
                            Text(item.title, style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textPrimary)
                        }
                    }
                }
            }
        }

        // ── Content Area ──
        when {
            // Searching / Loading
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CineVaultTheme.colors.accentGold)
                }
            }

            // Search results
            uiState.results.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad8),
                    horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10),
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16),
                ) {
                    items(uiState.results, key = { it.id }) { movie ->
                        SearchResultCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) },
                        )
                    }
                    if (uiState.hasMore) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMore() }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(LocalAppDimens.current.pad16),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = CineVaultTheme.colors.accentGold,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }

            // Default state: Recent + Popular + Recommended
            uiState.query.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── Recent Searches ──
                    if (uiState.recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Search Records",
                                style = CineVaultTheme.typography.sectionTitle,
                                color = CineVaultTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.clearAllRecentSearches(context) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Clear All",
                                    tint = CineVaultTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Recent search chips
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16),
                            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
                        ) {
                            items(uiState.recentSearches) { query ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = CineVaultTheme.colors.surface,
                                    modifier = Modifier.clickable {
                                        viewModel.onQueryChange(query)
                                        viewModel.search()
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad12, vertical = LocalAppDimens.current.pad8),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(query, style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textPrimary, fontSize = LocalAppDimens.current.font13)
                                        Spacer(Modifier.width(LocalAppDimens.current.pad6))
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            tint = CineVaultTheme.colors.textSecondary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeRecentSearch(context, query) },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(LocalAppDimens.current.pad20))
                    }

                    // ── Most Popular Searches (Top 3) ──
                    if (uiState.popularSearches.isNotEmpty()) {
                        Text(
                            "Most Popular Search",
                            style = CineVaultTheme.typography.sectionTitle,
                            color = CineVaultTheme.colors.textPrimary,
                            modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad8),
                        )
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16),
                            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
                        ) {
                            itemsIndexed(uiState.popularSearches.take(3)) { index, item ->
                                PopularSearchCard(
                                    rank = index + 1,
                                    movie = item,
                                    onClick = {
                                        onMovieClick(item.id)
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(LocalAppDimens.current.pad24))
                    }

                    // ── Recommended / Trending Grid ──
                    if (uiState.recommendedContent.isNotEmpty()) {
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))

                        // 2-column grid inline
                        val chunked = uiState.recommendedContent.chunked(2)
                        chunked.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad4),
                                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10),
                            ) {
                                row.forEach { movie ->
                                    RecommendedContentCard(
                                        movie = movie,
                                        onClick = { onMovieClick(movie.id) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size < 2) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }

            // No results
            uiState.query.isNotEmpty() && !uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No results found",
                        style = CineVaultTheme.typography.body,
                        color = CineVaultTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Popular Search Card (Top 1, 2, 3)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PopularSearchCard(
    rank: Int,
    movie: PopularSearchDto,
    onClick: () -> Unit,
) {
    val rankColors = listOf(
        Color(0xFFFFD700), // Gold - Top 1
        Color(0xFF4CAF50), // Green - Top 2
        Color(0xFFFF5722), // Orange-red - Top 3
    )
    val rankColor = rankColors.getOrElse(rank - 1) { CineVaultTheme.colors.accentGold }

    Column(
        modifier = Modifier
            .width(155.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CineVaultTheme.colors.surface),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Rank badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(LocalAppDimens.current.pad8),
                shape = RoundedCornerShape(LocalAppDimens.current.radius6),
                color = rankColor,
            ) {
                Text(
                    text = "TOP $rank",
                    style = CineVaultTheme.typography.labelSmall.copy(
                        fontSize = LocalAppDimens.current.font10,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad8, vertical = LocalAppDimens.current.pad4),
                )
            }

            // Bottom info overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xDD000000)),
                        ),
                    )
                    .padding(LocalAppDimens.current.pad8),
            ) {
                Column {
                    if (movie.rating != null && movie.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = CineVaultTheme.colors.accentGold,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                String.format("%.1f", movie.rating),
                                style = CineVaultTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(LocalAppDimens.current.pad6))
        Text(
            text = movie.title,
            style = CineVaultTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Recommended Content Card (2-column grid)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RecommendedContentCard(
    movie: MovieDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(LocalAppDimens.current.radius8))
            .background(CineVaultTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(LocalAppDimens.current.pad6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 70.dp)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius6))
                .background(CineVaultTheme.colors.surface),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(LocalAppDimens.current.pad8))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.title,
                style = CineVaultTheme.typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = LocalAppDimens.current.font12),
                color = CineVaultTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(LocalAppDimens.current.padTiny))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if ((movie.rating ?: 0.0) > 0.0) {
                    Text(
                        String.format("%.1f", movie.rating),
                        style = CineVaultTheme.typography.labelSmall,
                        color = CineVaultTheme.colors.accentGold,
                    )
                    Spacer(Modifier.width(LocalAppDimens.current.pad6))
                }
                Text(
                    movie.contentType.replaceFirstChar { it.uppercase() },
                    style = CineVaultTheme.typography.labelSmall.copy(fontSize = LocalAppDimens.current.font10),
                    color = CineVaultTheme.colors.textSecondary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Search Result Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SearchResultCard(
    movie: MovieDto,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius8))
                .background(CineVaultTheme.colors.surface),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        ),
                    ),
            )

            // Premium badge — top-left
            if (movie.isEffectivelyPremium) {
                PremiumBadgeOverlay(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(LocalAppDimens.current.pad4),
                    size = 30.dp,
                )
            }

            // Category tag
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(LocalAppDimens.current.pad4),
                shape = RoundedCornerShape(LocalAppDimens.current.radius4),
                color = CineVaultTheme.colors.accentGold.copy(alpha = 0.85f),
            ) {
                Text(
                    text = movie.contentType.replaceFirstChar { it.uppercase() },
                    style = CineVaultTheme.typography.labelSmall.copy(fontSize = LocalAppDimens.current.font8, fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad4, vertical = 1.dp),
                )
            }

            // Rating
            if ((movie.rating ?: 0.0) > 0.0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(LocalAppDimens.current.pad6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(LocalAppDimens.current.padTiny))
                    Text(
                        String.format("%.1f", movie.rating),
                        style = CineVaultTheme.typography.labelSmall.copy(fontSize = LocalAppDimens.current.font9),
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(LocalAppDimens.current.pad6))
        Text(
            text = movie.title,
            style = CineVaultTheme.typography.labelSmall,
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FilterSection(
    uiState: com.cinevault.app.ui.viewmodel.SearchUiState,
    viewModel: SearchViewModel,
) {
    Column(modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad16)) {
        // Content type
        Text("Type", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.textSecondary)
        Spacer(Modifier.height(LocalAppDimens.current.pad6))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8)) {
            val types = listOf("movie", "series", "documentary")
            items(types) { type ->
                GenreChip(
                    label = type.replaceFirstChar { it.uppercase() },
                    selected = uiState.selectedContentType == type,
                    onClick = { viewModel.setContentTypeFilter(if (uiState.selectedContentType == type) null else type) },
                )
            }
        }

        Spacer(Modifier.height(LocalAppDimens.current.pad12))

        // Genres
        if (uiState.genres.isNotEmpty()) {
            Text("Genre", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.textSecondary)
            Spacer(Modifier.height(LocalAppDimens.current.pad6))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8)) {
                items(uiState.genres) { genre ->
                    GenreChip(
                        label = genre,
                        selected = uiState.selectedGenre == genre,
                        onClick = { viewModel.setGenreFilter(if (uiState.selectedGenre == genre) null else genre) },
                    )
                }
            }
        }

        Spacer(Modifier.height(LocalAppDimens.current.pad12))

        // Languages
        if (uiState.languages.isNotEmpty()) {
            Text("Language", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.textSecondary)
            Spacer(Modifier.height(LocalAppDimens.current.pad6))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8)) {
                items(uiState.languages) { lang ->
                    GenreChip(
                        label = lang,
                        selected = uiState.selectedLanguage == lang,
                        onClick = { viewModel.setLanguageFilter(if (uiState.selectedLanguage == lang) null else lang) },
                    )
                }
            }
        }

        Spacer(Modifier.height(LocalAppDimens.current.pad8))
        @Suppress("DEPRECATION")
        Divider(color = CineVaultTheme.colors.border)
        Spacer(Modifier.height(LocalAppDimens.current.pad8))
    }
}
