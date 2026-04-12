package com.cinevault.tv.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    isPremium: Boolean = false,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBack()
                    true
                } else false
            },
    ) {
        // Search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.colors(
                    containerColor = TvSurfaceVariant,
                    contentColor = TvOnSurface,
                    focusedContainerColor = TvPrimary,
                ),
            ) {
                Text("← Back", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvSurfaceVariant)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.query.isEmpty()) {
                    Text("Search movies, series, anime...", color = TvTextMuted, fontSize = 16.sp)
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    textStyle = TextStyle(color = TvOnSurface, fontSize = 16.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(TvPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips row
        FilterSection(
            label = "Type",
            options = viewModel.contentTypes,
            selected = state.selectedContentType ?: "All",
            onSelect = { viewModel.onContentTypeChange(it) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilterSection(
            label = "Genre",
            options = viewModel.genres,
            selected = state.selectedGenre ?: "All",
            onSelect = { viewModel.onGenreChange(it) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilterSection(
            label = "Language",
            options = viewModel.languages,
            selected = state.selectedLanguage ?: "All",
            onSelect = { viewModel.onLanguageChange(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Searching...", color = TvTextMuted, fontSize = 18.sp)
            }
        } else if (state.results.isEmpty() && state.hasSearched) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", color = TvTextMuted, fontSize = 18.sp)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(state.results, key = { it.id }) { movie ->
                    SearchResultCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterSection(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = TvTextMuted,
            modifier = Modifier.width(80.dp),
        )
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options) { option ->
                val isSelected = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) TvPrimary else TvSurfaceVariant,
                        focusedContainerColor = TvPrimary,
                        contentColor = if (isSelected) TvOnPrimary else TvOnSurfaceVariant,
                        focusedContentColor = TvOnPrimary,
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultCard(movie: MovieDto, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(240.dp),
        shape = ClickableSurfaceDefaults.shape(CardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvSurface,
            focusedContainerColor = TvCardFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = TvBorderFocused),
                shape = CardShape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            ) {
                Text(
                    text = movie.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = movie.contentType.replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    color = TvTextMuted,
                )
            }
            if (movie.isEffectivelyPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvPremiumBadge)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("PREMIUM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
