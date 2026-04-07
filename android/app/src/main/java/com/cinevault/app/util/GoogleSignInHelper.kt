package com.cinevault.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.cinevault.app.BuildConfig

private const val TAG = "GoogleSignInHelper"

/**
 * Opens a Chrome Custom Tab pointing to the backend's Google web auth page.
 * The backend handles Google Sign-In via Firebase Web SDK, then redirects
 * back to velora://auth-callback with tokens.
 */
fun launchGoogleWebSignIn(context: Context, mode: String = "login") {
    val rootUrl = BuildConfig.BASE_URL.replace("api/v1/", "").replace("api/v1", "").trimEnd('/')
    val url = "$rootUrl/auth/google-web?mode=$mode"
    Log.d(TAG, "Opening Google web sign-in: $url")

    val colorScheme = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(0xFF050505.toInt())
        .setNavigationBarColor(0xFF050505.toInt())
        .build()

    val customTabsIntent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorScheme)
        .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
        .setShowTitle(false)
        .build()

    customTabsIntent.launchUrl(context, Uri.parse(url))
}
