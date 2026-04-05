# CineVault ProGuard Rules
-keep class com.cinevault.app.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Keep JavaScript interface methods for YouTube WebView player
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
