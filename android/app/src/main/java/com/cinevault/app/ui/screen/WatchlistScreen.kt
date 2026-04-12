package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.data.model.Result
import com.cinevault.app.data.repository.WatchlistRepository
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.ui.components.*
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.ProfileViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onMovieClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    "My Watchlist",
                    style = CineVaultTheme.typography.sectionTitle,
                    color = CineVaultTheme.colors.textPrimary,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CineVaultTheme.colors.accentGold)
                }
            }
            uiState.watchlist.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Your watchlist is empty",
                            style = CineVaultTheme.typography.body,
                            color = CineVaultTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))
                        Text(
                            "Save movies and series to watch later",
                            style = CineVaultTheme.typography.bodySmall,
                            color = CineVaultTheme.colors.textSecondary,
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad8),
                    horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10),
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16),
                ) {
                    items(uiState.watchlist, key = { it.id }) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) },
                            width = 120.dp,
                        )
                    }
                }
            }
        }
    }
}
