package com.cinevault.app.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "CineVaultAds"

        // Google AdMob production interstitial ad unit ID
        private const val AD_UNIT_ID = "ca-app-pub-5582201158743100/4012614425"

        /** Max time (ms) to wait for an ad to load when none is cached. */
        private const val AD_LOAD_TIMEOUT_MS = 15_000L
        /** Delay between retry attempts (ms). */
        private const val RETRY_DELAY_MS = 3_000L
        /** Max retry attempts on failure. */
        private const val MAX_RETRIES = 3
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isInitialized = false
    private var retryCount = 0

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        // AdMob SDK already initialized in CineVaultApp.onCreate()
        Log.d(TAG, "AdManager.initialize() — preloading interstitial ad")
        Log.d(TAG, "  Ad Unit: $AD_UNIT_ID")
        loadInterstitialAd()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun loadInterstitialAd() {
        if (isLoading || interstitialAd != null) {
            Log.d(TAG, "loadInterstitialAd: skip (isLoading=$isLoading, adCached=${interstitialAd != null})")
            return
        }
        doLoadAd()
    }

    /**
     * Force-load a new ad even if one is currently loading.
     * Used during pre-roll loop to ensure next ad is ready ASAP.
     */
    fun forceLoadInterstitialAd() {
        if (interstitialAd != null) return // already cached
        isLoading = false
        doLoadAd()
    }

    private fun doLoadAd() {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "loadInterstitialAd: No internet — scheduling retry in ${RETRY_DELAY_MS}ms")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loadInterstitialAd() }, RETRY_DELAY_MS)
            return
        }

        isLoading = true
        Log.d(TAG, "⏳ Loading AdMob interstitial ad (unit: $AD_UNIT_ID, retry=$retryCount)...")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ AdMob interstitial ad LOADED successfully")
                    interstitialAd = ad
                    isLoading = false
                    retryCount = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ AdMob interstitial FAILED: code=${error.code}, msg=${error.message}, domain=${error.domain}")
                    interstitialAd = null
                    isLoading = false

                    if (retryCount < MAX_RETRIES) {
                        retryCount++
                        Log.d(TAG, "  Retrying in ${RETRY_DELAY_MS}ms (attempt $retryCount/$MAX_RETRIES)")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            loadInterstitialAd()
                        }, RETRY_DELAY_MS)
                    } else {
                        Log.w(TAG, "  Max retries ($MAX_RETRIES) exhausted. Will load on next trigger.")
                        retryCount = 0
                    }
                }
            },
        )
    }

    // Alias for compatibility
    fun loadVideoAd() = loadInterstitialAd()

    /**
     * Shows interstitial ad instantly if cached.
     * If not cached, calls [onAdDismissed] so video can proceed.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "▶️ SHOWING interstitial ad now")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Ad DISMISSED by user")
                    interstitialAd = null
                    loadInterstitialAd() // Preload next immediately
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Ad FAILED to show: code=${adError.code}, msg=${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Ad DISPLAYED full screen")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "👆 Ad CLICKED")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "📊 Ad IMPRESSION recorded")
                }
            }
            ad.show(activity)
        } else {
            Log.w(TAG, "⚠️ No ad cached — skipping, preloading for next time")
            loadInterstitialAd()
            onAdDismissed()
        }
    }

    /**
     * Suspend version: waits up to [AD_LOAD_TIMEOUT_MS] for an ad to load,
     * then shows it instantly. Returns true if shown, false if timed out.
     */
    suspend fun showAdWithWait(activity: Activity, onAdDismissed: () -> Unit): Boolean {
        if (interstitialAd != null) {
            Log.d(TAG, "showAdWithWait: ad cached — showing INSTANTLY")
            showInterstitialAd(activity, onAdDismissed)
            return true
        }

        Log.d(TAG, "showAdWithWait: no ad cached — loading, waiting up to ${AD_LOAD_TIMEOUT_MS}ms...")

        // Force a fresh load
        retryCount = 0
        interstitialAd = null
        isLoading = false
        loadInterstitialAd()

        // Poll until ad is loaded or timeout
        val loaded = withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val checkRunnable = object : Runnable {
                    override fun run() {
                        if (interstitialAd != null) {
                            if (cont.isActive) cont.resume(true)
                        } else if (isLoading || retryCount > 0) {
                            handler.postDelayed(this, 250)
                        } else {
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                }
                handler.post(checkRunnable)
                cont.invokeOnCancellation { handler.removeCallbacks(checkRunnable) }
            }
        } ?: false

        if (loaded && interstitialAd != null) {
            Log.d(TAG, "✅ showAdWithWait: Ad loaded — showing now")
            showInterstitialAd(activity, onAdDismissed)
            return true
        } else {
            Log.w(TAG, "⚠️ showAdWithWait: Timed out — skipping ad")
            onAdDismissed()
            return false
        }
    }

    fun isAdReady(): Boolean = interstitialAd != null

    /**
     * Fully suspend version: loads (if needed), shows, and waits until ad is DISMISSED.
     * Returns true if ad was shown and dismissed, false if load/show failed.
     * Useful for playing multiple ads consecutively in a coroutine.
     */
    suspend fun showAdSuspend(activity: Activity): Boolean {
        // Wait for ad to load if not cached
        if (interstitialAd == null) {
            retryCount = 0
            isLoading = false
            loadInterstitialAd()

            val loaded = withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val checkRunnable = object : Runnable {
                        override fun run() {
                            if (interstitialAd != null) {
                                if (cont.isActive) cont.resume(true)
                            } else if (isLoading || retryCount > 0) {
                                handler.postDelayed(this, 250)
                            } else {
                                if (cont.isActive) cont.resume(false)
                            }
                        }
                    }
                    handler.post(checkRunnable)
                    cont.invokeOnCancellation { handler.removeCallbacks(checkRunnable) }
                }
            } ?: false

            if (!loaded || interstitialAd == null) {
                Log.w(TAG, "showAdSuspend: Timed out loading ad")
                return false
            }
        }

        // Show ad and suspend until dismissed
        return suspendCancellableCoroutine { cont ->
            val ad = interstitialAd
            if (ad == null) {
                if (cont.isActive) cont.resume(false)
                return@suspendCancellableCoroutine
            }
            Log.d(TAG, "▶️ showAdSuspend: SHOWING ad, waiting for dismissal...")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ showAdSuspend: Ad DISMISSED")
                    interstitialAd = null
                    loadInterstitialAd()
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ showAdSuspend: Ad FAILED to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    if (cont.isActive) cont.resume(false)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ showAdSuspend: Ad DISPLAYED full screen")
                }
            }
            ad.show(activity)
        }
    }
}
