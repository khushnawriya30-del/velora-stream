package com.cinevault.app.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
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

        // ── Toggle: set to true for testing, false for production ──
        private const val USE_TEST_ADS = false

        // Google's official test interstitial ID (always shows test ads)
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        // Production ad unit ID
        private const val PROD_AD_UNIT_ID = "ca-app-pub-5582201158743100/4012614425"

        val INTERSTITIAL_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID

        /** Max time (ms) to wait for an ad to load when none is cached. */
        private const val AD_LOAD_TIMEOUT_MS = 10_000L
        /** Delay between retry attempts (ms). */
        private const val RETRY_DELAY_MS = 5_000L
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
        Log.d(TAG, "Initializing Mobile Ads SDK...")
        Log.d(TAG, "Using ${if (USE_TEST_ADS) "TEST" else "PRODUCTION"} ad unit: $INTERSTITIAL_AD_UNIT_ID")
        MobileAds.initialize(context) { initStatus ->
            val adapterStatus = initStatus.adapterStatusMap
            Log.d(TAG, "Mobile Ads SDK initialized. Adapters: ${adapterStatus.entries.joinToString { "${it.key}=${it.value.initializationState}" }}")
            loadInterstitialAd()
        }
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
        if (!isNetworkAvailable()) {
            Log.w(TAG, "loadInterstitialAd: No internet — will retry later")
            return
        }

        isLoading = true
        Log.d(TAG, "Loading interstitial ad (unit: $INTERSTITIAL_AD_UNIT_ID)...")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ Interstitial ad LOADED successfully")
                    interstitialAd = ad
                    isLoading = false
                    retryCount = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Interstitial ad FAILED to load: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    interstitialAd = null
                    isLoading = false

                    // Retry with backoff
                    if (retryCount < MAX_RETRIES) {
                        retryCount++
                        val delayMs = RETRY_DELAY_MS * retryCount
                        Log.d(TAG, "Will retry loading ad in ${delayMs}ms (attempt $retryCount/$MAX_RETRIES)")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            loadInterstitialAd()
                        }, delayMs)
                    } else {
                        Log.w(TAG, "Max retries reached ($MAX_RETRIES). Ad will be loaded on next trigger.")
                        retryCount = 0
                    }
                }
            },
        )
    }

    /**
     * Shows an interstitial ad. If one is cached, shows immediately.
     * If not, calls [onAdDismissed] so video can proceed (no blocking).
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "Showing interstitial ad...")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Ad dismissed by user")
                    interstitialAd = null
                    loadInterstitialAd() // Preload next
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Ad failed to show: code=${adError.code}, message=${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Ad shown full screen")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Ad clicked")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Ad impression recorded")
                }
            }
            ad.show(activity)
        } else {
            Log.w(TAG, "⚠️ No ad cached — skipping ad, proceeding to video")
            loadInterstitialAd() // Start loading for next time
            onAdDismissed()
        }
    }

    /**
     * Suspend version: waits up to [AD_LOAD_TIMEOUT_MS] for an ad to load,
     * then shows it. Returns true if an ad was shown, false if timed out / failed.
     * Must be called from a coroutine on Main dispatcher.
     */
    suspend fun showAdWithWait(activity: Activity, onAdDismissed: () -> Unit): Boolean {
        // If ad already cached, show immediately
        if (interstitialAd != null) {
            showInterstitialAd(activity, onAdDismissed)
            return true
        }

        Log.d(TAG, "No ad cached — waiting up to ${AD_LOAD_TIMEOUT_MS}ms for ad to load...")

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
                        } else if (isLoading) {
                            handler.postDelayed(this, 200)
                        } else {
                            // Not loading, no ad — load failed
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                }
                handler.post(checkRunnable)
                cont.invokeOnCancellation { handler.removeCallbacks(checkRunnable) }
            }
        } ?: false

        if (loaded && interstitialAd != null) {
            Log.d(TAG, "✅ Ad loaded after waiting — showing now")
            showInterstitialAd(activity, onAdDismissed)
            return true
        } else {
            Log.w(TAG, "⚠️ Ad did not load within timeout — skipping")
            onAdDismissed()
            return false
        }
    }

    fun isAdReady(): Boolean = interstitialAd != null
}
