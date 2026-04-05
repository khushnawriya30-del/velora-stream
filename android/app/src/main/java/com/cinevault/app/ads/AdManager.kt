package com.cinevault.app.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
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
        private const val APP_ID = "203305409"

        /** Max time (ms) to wait for an ad to load when none is cached. */
        private const val AD_LOAD_TIMEOUT_MS = 10_000L
        /** Delay between retry attempts (ms). */
        private const val RETRY_DELAY_MS = 5_000L
        /** Max retry attempts on failure. */
        private const val MAX_RETRIES = 3
    }

    private var startAppAd: StartAppAd? = null
    private var isLoading = false
    private var isInitialized = false
    private var retryCount = 0
    private var adReady = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        Log.d(TAG, "Initializing Start.io SDK with App ID: $APP_ID")

        // Disable Start.io splash ad and return ad (we manage ad placement ourselves)
        StartAppSDK.init(context, APP_ID, false)
        StartAppAd.disableAutoInterstitial()

        Log.d(TAG, "Start.io SDK initialized")
        loadInterstitialAd()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun loadInterstitialAd() {
        if (isLoading || adReady) {
            Log.d(TAG, "loadInterstitialAd: skip (isLoading=$isLoading, adReady=$adReady)")
            return
        }
        if (!isNetworkAvailable()) {
            Log.w(TAG, "loadInterstitialAd: No internet — will retry later")
            return
        }

        isLoading = true
        Log.d(TAG, "Loading Start.io VIDEO interstitial ad...")

        val ad = StartAppAd(context)
        ad.loadAd(StartAppAd.AdMode.VIDEO, object : AdEventListener {
            override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad) {
                Log.d(TAG, "✅ Start.io interstitial ad LOADED successfully")
                startAppAd = ad
                adReady = true
                isLoading = false
                retryCount = 0
            }

            override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                Log.e(TAG, "❌ Start.io interstitial ad FAILED to load")
                startAppAd = null
                adReady = false
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
        })
    }

    /**
     * Shows an interstitial ad. If one is cached, shows immediately.
     * If not, calls [onAdDismissed] so video can proceed (no blocking).
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = startAppAd
        if (ad != null && adReady) {
            Log.d(TAG, "Showing Start.io interstitial ad...")
            ad.showAd(object : AdDisplayListener {
                override fun adHidden(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "✅ Ad dismissed by user")
                    startAppAd = null
                    adReady = false
                    loadInterstitialAd() // Preload next
                    onAdDismissed()
                }

                override fun adDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "✅ Ad shown full screen")
                }

                override fun adClicked(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "Ad clicked")
                }

                override fun adNotDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.e(TAG, "❌ Ad failed to display")
                    startAppAd = null
                    adReady = false
                    loadInterstitialAd()
                    onAdDismissed()
                }
            })
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
        if (adReady && startAppAd != null) {
            showInterstitialAd(activity, onAdDismissed)
            return true
        }

        Log.d(TAG, "No ad cached — waiting up to ${AD_LOAD_TIMEOUT_MS}ms for ad to load...")

        // Force a fresh load
        retryCount = 0
        startAppAd = null
        adReady = false
        isLoading = false
        loadInterstitialAd()

        // Poll until ad is loaded or timeout
        val loaded = withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val checkRunnable = object : Runnable {
                    override fun run() {
                        if (adReady && startAppAd != null) {
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

        if (loaded && adReady && startAppAd != null) {
            Log.d(TAG, "✅ Ad loaded after waiting — showing now")
            showInterstitialAd(activity, onAdDismissed)
            return true
        } else {
            Log.w(TAG, "⚠️ Ad did not load within timeout — skipping")
            onAdDismissed()
            return false
        }
    }

    fun isAdReady(): Boolean = adReady && startAppAd != null
}
