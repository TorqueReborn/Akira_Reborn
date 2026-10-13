# Keep models parsed via reflection or JSON serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Akira models
-keep class com.ghostreborn.akira.features.**.models.** { *; }
-keep class com.ghostreborn.akira.core.config.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }

# Bouncy Castle
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }
