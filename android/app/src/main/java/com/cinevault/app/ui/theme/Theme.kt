package com.cinevault.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = BackgroundDark,
    secondary = AccentGoldMuted,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed,
)

@Composable
fun CineVaultTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = BackgroundDark,
            darkIcons = false,
        )
        systemUiController.setNavigationBarColor(
            color = BackgroundDark,
            darkIcons = false,
        )
    }

    CompositionLocalProvider(
        LocalCineVaultColors provides CineVaultColors(),
        LocalCineVaultTypography provides CineVaultTypography(),
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content,
        )
    }
}

object CineVaultTheme {
    val colors: CineVaultColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCineVaultColors.current

    val typography: CineVaultTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCineVaultTypography.current
}
