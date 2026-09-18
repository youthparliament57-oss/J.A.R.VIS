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
-keep class com.example.cognition.llm.** { *; }

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep CameraX internal implementations
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Coroutines and Flow optimization retention
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Line number preservation for clean crash logging
-keepattributes SourceFile,LineNumberTable
