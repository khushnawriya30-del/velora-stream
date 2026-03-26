package com.cinevault.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object ProfileSelector : Screen("profile_selector")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Watchlist : Screen("watchlist")
    data object Me : Screen("me")

    data object MovieDetail : Screen("movie/{movieId}") {
        fun createRoute(movieId: String) = "movie/$movieId"
    }

    data object Player : Screen("player/{contentId}?episodeId={episodeId}") {
        fun createRoute(contentId: String, episodeId: String? = null): String {
            return if (episodeId != null) "player/$contentId?episodeId=$episodeId"
            else "player/$contentId"
        }
    }
}
