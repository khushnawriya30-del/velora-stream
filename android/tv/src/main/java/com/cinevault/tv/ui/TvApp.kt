package com.cinevault.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cinevault.tv.ui.screens.detail.DetailScreen
import com.cinevault.tv.ui.screens.home.HomeScreen
import com.cinevault.tv.ui.screens.login.QrLoginScreen
import com.cinevault.tv.ui.screens.player.PlayerScreen
import com.cinevault.tv.ui.screens.premium.PremiumGateScreen
import com.cinevault.tv.ui.screens.search.SearchScreen
import com.cinevault.tv.ui.theme.CineVaultTvTheme

object TvRoutes {
    const val LOGIN = "login"
    const val PREMIUM_GATE = "premium_gate"
    const val HOME = "home"
    const val DETAIL = "detail/{movieId}"
    const val PLAYER = "player/{movieId}"
    const val SEARCH = "search"

    fun detail(movieId: String) = "detail/$movieId"
    fun player(movieId: String) = "player/$movieId"
}

@Composable
fun TvApp() {
    CineVaultTvTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = hiltViewModel()
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val isPremium by authViewModel.isPremium.collectAsState()

        val startDestination = when {
            !isLoggedIn -> TvRoutes.LOGIN
            !isPremium -> TvRoutes.PREMIUM_GATE
            else -> TvRoutes.HOME
        }

        NavHost(navController = navController, startDestination = startDestination) {
            composable(TvRoutes.LOGIN) {
                QrLoginScreen(
                    onLoginSuccess = { premium ->
                        if (premium) {
                            navController.navigate(TvRoutes.HOME) {
                                popUpTo(TvRoutes.LOGIN) { inclusive = true }
                            }
                        } else {
                            navController.navigate(TvRoutes.PREMIUM_GATE) {
                                popUpTo(TvRoutes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(TvRoutes.PREMIUM_GATE) {
                PremiumGateScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(TvRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(TvRoutes.HOME) {
                HomeScreen(
                    onMovieClick = { movieId ->
                        navController.navigate(TvRoutes.detail(movieId))
                    },
                    onSearchClick = {
                        navController.navigate(TvRoutes.SEARCH)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(TvRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(TvRoutes.SEARCH) {
                SearchScreen(
                    onMovieClick = { movieId ->
                        navController.navigate(TvRoutes.detail(movieId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = TvRoutes.DETAIL,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) {
                DetailScreen(
                    onPlayClick = { movieId ->
                        navController.navigate(TvRoutes.player(movieId))
                    },
                    onMovieClick = { movieId ->
                        navController.navigate(TvRoutes.detail(movieId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = TvRoutes.PLAYER,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
