package com.cinevault.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvDarkColorScheme = darkColorScheme(
    primary = TvPrimary,
    onPrimary = TvOnPrimary,
    surface = TvSurface,
    onSurface = TvOnSurface,
    background = TvBackground,
    onBackground = TvOnBackground,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvOnSurfaceVariant,
    border = TvBorderSubtle,
    error = TvError,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CineVaultTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content,
    )
}

val CardShape = RoundedCornerShape(10.dp)
val BannerShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(8.dp)
val PillShape = RoundedCornerShape(22.dp)
