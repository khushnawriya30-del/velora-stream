# Add project specific ProGuard rules here.

# Keep application class
-keep class com.cinevault.tv.CineVaultTvApp { *; }
-keep class com.cinevault.tv.MainActivity { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**

# Gson / Data Models — keep ALL fields
-keep class com.cinevault.tv.data.model.** { *; }
-keep class com.cinevault.tv.data.remote.** { *; }
-keepattributes SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keepclasseswithmembers class * { @dagger.* <methods>; }
-keepclasseswithmembers class * { @javax.inject.* <fields>; }
-keepclasseswithmembers class * { @dagger.hilt.* <methods>; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# TV Libraries
-dontwarn androidx.tv.**
-keep class androidx.tv.** { *; }

# Leanback
-dontwarn androidx.leanback.**
-keep class androidx.leanback.** { *; }

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep ViewModels
-keep class com.cinevault.tv.ui.** { *; }

# Keep enums
-keepclassmembers enum * { *; }

# Accompanist
-dontwarn com.google.accompanist.**
