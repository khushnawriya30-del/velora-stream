package com.cinevault.app

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CineVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("CineVaultAds", "App onCreate — initializing AdMob SDK early")
        MobileAds.initialize(this) { initStatus ->
            Log.d("CineVaultAds", "AdMob SDK initialized: ${initStatus.adapterStatusMap.entries.joinToString { "${it.key}=${it.value.initializationState}" }}")
        }
        Log.d("CineVaultAds", "AdMob SDK init dispatched (production mode)")
    }
}
