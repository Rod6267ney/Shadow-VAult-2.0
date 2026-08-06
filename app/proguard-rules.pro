# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Shadow Vault / Chaos OS Security Rules

# Preserve Room Database Entities and DAOs
-keep class com.example.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Preserve Shizuku API
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }

# Preserve JNI methods (for native stealth/hooking)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Extreme Obfuscation
-repackageclasses ''
-flattenpackagehierarchy ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Hide original source file names
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove Log calls (Sanitização)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
