package com.cinevault.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "CineVaultAds"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5582201158743100/4012614425"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isInitialized = false

    /** Callback invoked when the user closes the ad before it finishes (clicks back / swipe). */
    var onAdClosed: (() -> Unit)? = null

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        MobileAds.initialize(context) {
            Log.d(TAG, "Mobile Ads SDK initialized")
            loadInterstitialAd()
        }
    }

    fun loadInterstitialAd() {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            },
        )
    }

    /**
     * Shows an interstitial ad if one is loaded.
     * Calls [onAdDismissed] when the ad is closed (finished or skipped).
     * Automatically preloads the next ad after showing.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad shown")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "No interstitial ad loaded — skipping")
            loadInterstitialAd()
            onAdDismissed()
        }
    }

    fun isAdReady(): Boolean = interstitialAd != null
}
