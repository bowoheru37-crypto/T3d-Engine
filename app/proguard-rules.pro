# T3D Engine - ProGuard / R8 Optimization Rules (Low-RAM & itel A70 Profile)

# 1. General Optimizations & Aggressive Shrinking
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-optimizationpasses 5
-overloadaggressively
-verbose

# 2. Keep Compose & UI Foundations
-keep class androidx.compose.** { *; }
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**

# 3. Keep Room Database & SQLite
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
}

# 4. Keep Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 5. Keep T3D Engine ECS & Data Models
-keep class com.example.engine.core.** { *; }
-keep class com.example.engine.ecs.** { *; }
-keep class com.example.engine.terrain.** { *; }
-keep class com.example.engine.generators.** { *; }
-keep class com.example.engine.particles.** { *; }
-keep class com.example.engine.animation.** { *; }
-keep class com.example.engine.spatial.** { *; }
-keep class com.example.engine.data.** { *; }

# 6. Strip Unnecessary Logging in Release Builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
