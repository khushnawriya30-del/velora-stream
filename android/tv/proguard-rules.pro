# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# Gson
-keep class com.cinevault.tv.data.model.** { *; }
-keepattributes SerializedName

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ZXing
-keep class com.google.zxing.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
