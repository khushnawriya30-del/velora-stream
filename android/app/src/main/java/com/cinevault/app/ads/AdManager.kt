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

        // ┌──────────────────────────────────────────────────────────────┐
        // │  TODO: Replace these TEST IDs with your real AdMob IDs      │
        // │  before publishing to the Play Store.                        │
        // │  Test IDs are safe for development — they won't charge.      │
        // └──────────────────────────────────────────────────────────────┘
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test ID
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isInitialized = false

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
     * Calls [onAdDismissed] when the ad is closed or if no ad is available.
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
