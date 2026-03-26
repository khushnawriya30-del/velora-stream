package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.*
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CineVaultTheme.colors.accentGold,
                    unfocusedBorderColor = CineVaultTheme.colors.border,
                    cursorColor = CineVaultTheme.colors.accentGold,
                    focusedTextColor = CineVaultTheme.colors.textPrimary,
                    unfocusedTextColor = CineVaultTheme.colors.textPrimary,
                ),
            )
            IconButton(onClick = { viewModel.toggleFilters() }) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Filters",
                    tint = if (uiState.showFilters) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textSecondary,
                )
            }
        }

        // Filter chips
        if (uiState.showFilters) {
            FilterSection(uiState, viewModel)
        }

        // Autocomplete overlay
        if (uiState.autocomplete.isNotEmpty() && uiState.query.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = CineVaultTheme.colors.textSecondary)
                            Spacer(Modifier.width(12.dp))
                            Text(item.title, style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textPrimary)
                        }
                    }
                }
            }
        }

        // Content area
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CineVaultTheme.colors.accentGold)
                }
            }
            uiState.results.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.results, key = { it.id }) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) },
                            width = 120.dp,
                        )
                    }
                    if (uiState.hasMore) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMore() }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            uiState.query.isEmpty() && uiState.trendingSearches.isNotEmpty() -> {
                // Trending searches
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Trending Searches")
                    Spacer(Modifier.height(8.dp))
                    uiState.trendingSearches.forEachIndexed { index, query ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectTrendingSearch(query) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}",
                                style = CineVaultTheme.typography.sectionTitle,
                                color = CineVaultTheme.colors.accentGold,
                                modifier = Modifier.width(32.dp),
                            )
                            Text(
                                query,
                                style = CineVaultTheme.typography.body,
                                color = CineVaultTheme.colors.textPrimary,
                            )
                        }
                    }
                }
            }
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

@Composable
private fun FilterSection(
    uiState: com.cinevault.app.ui.viewmodel.SearchUiState,
    viewModel: SearchViewModel,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Content type
        Text("Type", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val types = listOf("movie", "series", "documentary")
            items(types) { type ->
                GenreChip(
                    label = type.replaceFirstChar { it.uppercase() },
                    selected = uiState.selectedContentType == type,
                    onClick = { viewModel.setContentTypeFilter(if (uiState.selectedContentType == type) null else type) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Genres
        if (uiState.genres.isNotEmpty()) {
            Text("Genre", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.genres) { genre ->
                    GenreChip(
                        label = genre,
                        selected = uiState.selectedGenre == genre,
                        onClick = { viewModel.setGenreFilter(if (uiState.selectedGenre == genre) null else genre) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Languages
        if (uiState.languages.isNotEmpty()) {
            Text("Language", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.languages) { lang ->
                    GenreChip(
                        label = lang,
                        selected = uiState.selectedLanguage == lang,
                        onClick = { viewModel.setLanguageFilter(if (uiState.selectedLanguage == lang) null else lang) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        @Suppress("DEPRECATION")
        Divider(color = CineVaultTheme.colors.border)
        Spacer(Modifier.height(8.dp))
    }
}
