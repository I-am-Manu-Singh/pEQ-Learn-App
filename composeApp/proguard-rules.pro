# Jetpack Compose ProGuard Rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Media3 / ExoPlayer ProGuard Rules
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# ONNX Runtime Rules
-keep class com.microsoft.onnxruntime.** { *; }
-dontwarn com.microsoft.onnxruntime.**

# jAudioTagger Rules
-keep class net.jthink.jaudiotagger.** { *; }
-dontwarn net.jthink.jaudiotagger.**

# Ktor Client Rules (OkHttp & Core)
-keep class io.ktor.client.** { *; }
-dontwarn io.ktor.**
-dontwarn io.netty.**
-dontwarn org.apache.http.**
-dontwarn org.slf4j.impl.StaticLoggerBinder


# ==============================================================================
# STRIP LOG STATEMENTS IN RELEASE BUILDS
# (Runs only in release builds where minification & R8 optimization are enabled.
#  Debug builds remain unaffected so logs continue to work normally during testing.)
# ==============================================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int println(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(java.lang.String);
    public void println(java.lang.Object);
    public void println(boolean);
    public void println(char);
    public void println(char[]);
    public void println(double);
    public void println(float);
    public void println(int);
    public void println(long);
    public void print(java.lang.String);
    public void print(java.lang.Object);
    public void print(boolean);
    public void print(char);
    public void print(char[]);
    public void print(double);
    public void print(float);
    public void print(int);
    public void print(long);
}

# Keep Kotlin Multiplatform & Compose attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep Firebase, Google Play & Play Integrity
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.integrity.**

