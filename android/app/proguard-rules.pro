# CineVault ProGuard Rules
-keep class com.cinevault.app.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Start.io Ads SDK
-keep class com.startapp.** { *; }
-dontwarn com.startapp.**

# Keep JavaScript interface methods for YouTube WebView player
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
