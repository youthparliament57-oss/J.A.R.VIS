# ProGuard / R8 Rules for NOUS Autonomous Architecture

# Keep Room database schemas and entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Keep Moshi & JSON serialization models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }

# Keep CameraX internal implementations
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Coroutines and Flow optimization retention
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Line number preservation for clean crash logging
-keepattributes SourceFile,LineNumberTable
