package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.ui.theme.CineVaultTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    sectionTitle: String,
    movies: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = CineVaultTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            sectionTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineVaultTheme.colors.textPrimary,
                        )
                        // Gold underline
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CineVaultTheme.colors.accentGold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CineVaultTheme.colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CineVaultTheme.colors.background,
                ),
            )
        }
    ) { paddingValues ->
        if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No content available",
                    color = CineVaultTheme.colors.textSecondary,
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(movies) { movie ->
                    GridMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridMovieCard(
    movie: MovieDto,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(CineVaultTheme.colors.surface)
        ) {
            AsyncImage(
                model = movie.posterUrl ?: movie.bannerUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                            )
                        )
                    )
            )

            // Language label — top-right
            val langLabel = movie.languageLabel
            if (!langLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.15f),
                ) {
                    Text(
                        langLabel,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.3.sp,
                    )
                }
            }

            // Content rating — top-left, white faded
            if (!movie.contentRating.isNullOrEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.2f),
                ) {
                    Text(
                        movie.contentRating!!,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            // Video quality — bottom-left
            if (!movie.videoQuality.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.2f),
                ) {
                    Text(
                        movie.videoQuality!!,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            // Star rating — bottom-right
            val displayRating = movie.starRating ?: movie.rating
            if (displayRating != null && displayRating > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = CineVaultTheme.colors.background.copy(alpha = 0.85f),
                ) {
                    Text(
                        String.format("%.1f", displayRating),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineVaultTheme.colors.ratingGold,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            movie.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
