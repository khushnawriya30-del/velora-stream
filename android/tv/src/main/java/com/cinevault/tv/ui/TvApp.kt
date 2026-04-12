package com.cinevault.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.cinevault.tv.ui.screens.splash.SplashScreen
import com.cinevault.tv.ui.theme.CineVaultTvTheme
import kotlinx.coroutines.delay

object TvRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val PREMIUM_GATE = "premium_gate"
    const val HOME = "home/{tab}"
    const val DETAIL = "detail/{movieId}"
    const val PLAYER = "player/{movieId}"
    const val PLAYER_EPISODE = "player/{movieId}?episodeId={episodeId}&seasonId={seasonId}"
    const val SEARCH = "search"

    fun home(tab: String = "home") = "home/$tab"
    fun detail(movieId: String) = "detail/$movieId"
    fun player(movieId: String) = "player/$movieId"
    fun playerEpisode(movieId: String, episodeId: String, seasonId: String) =
        "player/$movieId?episodeId=$episodeId&seasonId=$seasonId"
}

@Composable
fun TvApp() {
    CineVaultTvTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = hiltViewModel()
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val isPremium by authViewModel.isPremium.collectAsState()

        NavHost(navController = navController, startDestination = TvRoutes.SPLASH) {

            composable(TvRoutes.SPLASH) {
                SplashScreen(
                    onComplete = {
                        val dest = when {
                            !isLoggedIn -> TvRoutes.LOGIN
                            else -> TvRoutes.home()
                        }
                        navController.navigate(dest) {
                            popUpTo(TvRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(TvRoutes.LOGIN) {
                QrLoginScreen(
                    onLoginSuccess = { premium ->
                        navController.navigate(TvRoutes.home()) {
                            popUpTo(TvRoutes.LOGIN) { inclusive = true }
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

            composable(
                route = TvRoutes.HOME,
                arguments = listOf(navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "home"
                })
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getString("tab") ?: "home"
                HomeScreen(
                    initialTab = tab,
                    isPremium = isPremium,
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
                    },
                    onTabChange = { newTab ->
                        navController.navigate(TvRoutes.home(newTab)) {
                            popUpTo(TvRoutes.home()) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(TvRoutes.SEARCH) {
                SearchScreen(
                    isPremium = isPremium,
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
                    isPremium = isPremium,
                    onPlayClick = { movieId, episodeId, seasonId ->
                        if (!isPremium) {
                            // Show premium popup handled inside screen
                        } else if (episodeId != null && seasonId != null) {
                            navController.navigate(TvRoutes.playerEpisode(movieId, episodeId, seasonId))
                        } else {
                            navController.navigate(TvRoutes.player(movieId))
                        }
                    },
                    onMovieClick = { movieId ->
                        navController.navigate(TvRoutes.detail(movieId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = TvRoutes.PLAYER_EPISODE,
                arguments = listOf(
                    navArgument("movieId") { type = NavType.StringType },
                    navArgument("episodeId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("seasonId") { type = NavType.StringType; defaultValue = "" },
                )
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "player/{movieId}",
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
