package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AddMoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoreScreen(
    type: String, // "watchlist" or "collection"
    onBack: () -> Unit,
    viewModel: AddMoreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.initialize(type)
    }

    // Load more when reaching end
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= listState.layoutInfo.totalItemsCount - 5
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState.hasMore && !uiState.isLoading) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
            .statusBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = CineVaultTheme.colors.textPrimary)
            }
            Text(
                text = if (type == "watchlist") "Add to Watchlist" else "Add to Collection",
                style = CineVaultTheme.typography.sectionTitle,
                color = CineVaultTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = {
                Text("Search content...", color = CineVaultTheme.colors.textSecondary)
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = CineVaultTheme.colors.textSecondary)
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = CineVaultTheme.colors.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CineVaultTheme.colors.accentGold,
                unfocusedBorderColor = CineVaultTheme.colors.border,
                cursorColor = CineVaultTheme.colors.accentGold,
                focusedTextColor = CineVaultTheme.colors.textPrimary,
                unfocusedTextColor = CineVaultTheme.colors.textPrimary,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Content list
        if (uiState.isLoading && uiState.allContent.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CineVaultTheme.colors.accentGold)
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val filtered = uiState.filteredContent
                items(filtered, key = { it.id }) { movie ->
                    val isAdded = uiState.addedIds.contains(movie.id)
                    AddMoreContentItem(
                        movie = movie,
                        isAdded = isAdded,
                        onAdd = { viewModel.addContent(movie.id) },
                        onRemove = { viewModel.removeContent(movie.id) },
                    )
                }

                if (uiState.isLoading && uiState.allContent.isNotEmpty()) {
                    item {
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
    }
}

@Composable
private fun AddMoreContentItem(
    movie: MovieDto,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CineVaultTheme.colors.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Poster
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 85.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CineVaultTheme.colors.surface),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.width(12.dp))

        // Title + info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.title,
                style = CineVaultTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = CineVaultTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                movie.contentType.let { type ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CineVaultTheme.colors.accentGold.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = type.replaceFirstChar { it.uppercase() },
                            style = CineVaultTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = CineVaultTheme.colors.accentGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                movie.releaseYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = CineVaultTheme.typography.labelSmall,
                        color = CineVaultTheme.colors.textSecondary,
                    )
                }
                if ((movie.rating ?: 0.0) > 0.0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = CineVaultTheme.colors.accentGold,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", movie.rating),
                            style = CineVaultTheme.typography.labelSmall,
                            color = CineVaultTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Add/Remove button
        IconButton(
            onClick = if (isAdded) onRemove else onAdd,
        ) {
            Icon(
                imageVector = if (isAdded) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                contentDescription = if (isAdded) "Remove" else "Add",
                tint = if (isAdded) Color(0xFF4CAF50) else CineVaultTheme.colors.accentGold,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
