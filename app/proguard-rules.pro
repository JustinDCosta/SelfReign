# ProGuard / R8 rules for the release build.
# The default optimizations come from proguard-android-optimize.txt (see build.gradle.kts).

# --- Jetpack Compose ---
# Keep Compose runtime metadata so composition/recomposition reflection works.
-keep class androidx.compose.** { *; }

# --- Ambient audio (MediaSession compat) ---
# The media service uses the support-media-compat session classes via reflection.
-keep class android.support.v4.media.** { *; }

# --- Glance app widget ---
# Glance + its RemoteViews translation layer rely on reflection over these classes.
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# --- WorkManager ---
# Keep CoroutineWorker subclasses so WorkManager can instantiate them by name.
-keep class * extends androidx.work.ListenableWorker { *; }

# --- Jetpack Security (Tink under the hood) ---
# Tink uses reflection for its key managers; keep it to avoid keystore init failures.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --- Kotlin coroutines (debug metadata is safe to drop, but keep internals) ---
-dontwarn kotlinx.coroutines.**
