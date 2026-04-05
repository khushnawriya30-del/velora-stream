package com.cinevault.app

import android.app.Application
import android.util.Log
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CineVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Start.io SDK as early as possible for ad fill
        Log.d("CineVaultAds", "App onCreate — initializing Start.io SDK early")
        StartAppSDK.init(this, "203305409", false)
        StartAppSDK.setTestAdsEnabled(false) // Production mode
        StartAppAd.disableAutoInterstitial()
        Log.d("CineVaultAds", "Start.io SDK initialized in Application.onCreate (production mode)")
    }
}
