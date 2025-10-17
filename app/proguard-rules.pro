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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep generic type information for Gson TypeToken
-keepattributes Signature
-keepattributes *Annotation*

# Keep TypeToken class and its generic signatures
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Keep generic signatures for Gson
-keep class com.ddy.habitpulse.db.HabitTypeConverters { *; }

# Keep generic types for Gson deserialization
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * extends com.google.gson.TypeAdapter { *; }

# Keep Room database related classes
-keep class * extends androidx.room.TypeConverter { *; }

# Keep specific generic types used in TypeConverters
-keep class com.google.gson.reflect.TypeToken$*