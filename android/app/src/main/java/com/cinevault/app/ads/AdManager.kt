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
        private const val AD_LOAD_TIMEOUT_MS = 15_000L
        /** Delay between retry attempts (ms). */
        private const val RETRY_DELAY_MS = 3_000L
        /** Max retry attempts for VIDEO mode before falling back. */
        private const val MAX_VIDEO_RETRIES = 2
        /** Max retry attempts for FULLPAGE fallback. */
        private const val MAX_FALLBACK_RETRIES = 2
    }

    private var startAppAd: StartAppAd? = null
    private var isLoading = false
    private var isInitialized = false
    private var videoRetryCount = 0
    private var fallbackRetryCount = 0
    private var adReady = false
    /** True when we're trying FULLPAGE as fallback after VIDEO failed. */
    private var usingFallback = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        // SDK already initialized in CineVaultApp.onCreate() — just start preloading
        Log.d(TAG, "AdManager.initialize() — SDK already init in Application, preloading VIDEO ad...")
        Log.d(TAG, "  App ID: $APP_ID")
        Log.d(TAG, "  Test ads: DISABLED (production mode)")
        Log.d(TAG, "  Ad mode: VIDEO (fullscreen video interstitial)")
        loadVideoAd()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Load a VIDEO interstitial ad. If VIDEO fails after retries,
     * falls back to FULLPAGE (still fullscreen, but may be static/rich media).
     */
    fun loadVideoAd() {
        if (isLoading || adReady) {
            Log.d(TAG, "loadVideoAd: skip (isLoading=$isLoading, adReady=$adReady)")
            return
        }
        if (!isNetworkAvailable()) {
            Log.w(TAG, "loadVideoAd: No internet — scheduling retry in ${RETRY_DELAY_MS}ms")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loadVideoAd() }, RETRY_DELAY_MS)
            return
        }

        val mode = if (usingFallback) StartAppAd.AdMode.AUTOMATIC else StartAppAd.AdMode.VIDEO
        val modeLabel = if (usingFallback) "AUTOMATIC (fallback)" else "VIDEO"

        isLoading = true
        Log.d(TAG, "⏳ Loading Start.io ad — mode=$modeLabel, videoRetry=$videoRetryCount, fallbackRetry=$fallbackRetryCount")

        val ad = StartAppAd(context)
        ad.loadAd(mode, object : AdEventListener {
            override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad) {
                Log.d(TAG, "✅ Ad LOADED — mode=$modeLabel")
                startAppAd = ad
                adReady = true
                isLoading = false
                videoRetryCount = 0
                fallbackRetryCount = 0
                usingFallback = false
            }

            override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                Log.e(TAG, "❌ Ad FAILED to load — mode=$modeLabel, errorInfo=${p0?.errorMessage ?: "unknown"}")
                startAppAd = null
                adReady = false
                isLoading = false

                if (!usingFallback) {
                    // VIDEO mode failed
                    if (videoRetryCount < MAX_VIDEO_RETRIES) {
                        videoRetryCount++
                        Log.d(TAG, "  Retrying VIDEO in ${RETRY_DELAY_MS}ms (attempt $videoRetryCount/$MAX_VIDEO_RETRIES)")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loadVideoAd() }, RETRY_DELAY_MS)
                    } else {
                        // Switch to fallback
                        Log.w(TAG, "  VIDEO failed $MAX_VIDEO_RETRIES times — switching to AUTOMATIC fallback")
                        usingFallback = true
                        videoRetryCount = 0
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loadVideoAd() }, RETRY_DELAY_MS)
                    }
                } else {
                    // Fallback mode also failed
                    if (fallbackRetryCount < MAX_FALLBACK_RETRIES) {
                        fallbackRetryCount++
                        Log.d(TAG, "  Retrying AUTOMATIC in ${RETRY_DELAY_MS}ms (attempt $fallbackRetryCount/$MAX_FALLBACK_RETRIES)")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loadVideoAd() }, RETRY_DELAY_MS)
                    } else {
                        Log.w(TAG, "  All retries exhausted (VIDEO + AUTOMATIC). Will retry on next trigger.")
                        videoRetryCount = 0
                        fallbackRetryCount = 0
                        usingFallback = false
                    }
                }
            }
        })
    }

    // Keep old name as alias so PlayerViewModel/PlayerScreen don't need changes
    fun loadInterstitialAd() = loadVideoAd()

    /**
     * Shows ad if cached. Otherwise calls [onAdDismissed] immediately.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = startAppAd
        if (ad != null && adReady) {
            Log.d(TAG, "▶️ SHOWING ad now (adReady=$adReady)")
            ad.showAd(object : AdDisplayListener {
                override fun adHidden(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "✅ Ad DISMISSED by user")
                    startAppAd = null
                    adReady = false
                    usingFallback = false // Reset — try VIDEO first next time
                    loadVideoAd() // Preload next
                    onAdDismissed()
                }

                override fun adDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "✅ Ad DISPLAYED full screen")
                }

                override fun adClicked(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.d(TAG, "👆 Ad CLICKED by user")
                }

                override fun adNotDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                    Log.e(TAG, "❌ Ad FAILED to display — errorInfo=${p0?.errorMessage ?: "unknown"}")
                    startAppAd = null
                    adReady = false
                    loadVideoAd()
                    onAdDismissed()
                }
            })
        } else {
            Log.w(TAG, "⚠️ No ad cached (adReady=$adReady, ad=${startAppAd != null}) — skipping, preloading next")
            loadVideoAd()
            onAdDismissed()
        }
    }

    /**
     * Suspend version: waits up to [AD_LOAD_TIMEOUT_MS] for an ad to load,
     * then shows it. Returns true if shown, false if timed out.
     */
    suspend fun showAdWithWait(activity: Activity, onAdDismissed: () -> Unit): Boolean {
        if (adReady && startAppAd != null) {
            Log.d(TAG, "showAdWithWait: ad cached — showing immediately")
            showInterstitialAd(activity, onAdDismissed)
            return true
        }

        Log.d(TAG, "showAdWithWait: no ad cached — force loading, waiting up to ${AD_LOAD_TIMEOUT_MS}ms...")

        // Force a fresh load
        videoRetryCount = 0
        fallbackRetryCount = 0
        usingFallback = false
        startAppAd = null
        adReady = false
        isLoading = false
        loadVideoAd()

        // Poll until ad is loaded or timeout
        val loaded = withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val checkRunnable = object : Runnable {
                    override fun run() {
                        if (adReady && startAppAd != null) {
                            if (cont.isActive) cont.resume(true)
                        } else if (isLoading || videoRetryCount > 0 || fallbackRetryCount > 0 || usingFallback) {
                            // Still trying — keep polling
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

        if (loaded && adReady && startAppAd != null) {
            Log.d(TAG, "✅ showAdWithWait: Ad loaded after waiting — showing now")
            showInterstitialAd(activity, onAdDismissed)
            return true
        } else {
            Log.w(TAG, "⚠️ showAdWithWait: Ad did not load in time — skipping")
            onAdDismissed()
            return false
        }
    }

    fun isAdReady(): Boolean = adReady && startAppAd != null
}
