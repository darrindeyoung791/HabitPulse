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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Compose Preview functions (for debugging only, can be removed in production)
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep data classes and entities if you have any
# -keep class io.github.darrindeyoung791.habitpulse.bean.** { *; }

# Gson - keep LLM request/response data classes used by fromJson()
-keep class io.github.darrindeyoung791.habitpulse.ai.llm.** { *; }

# Gson - keep conversation state data classes
-keep class io.github.darrindeyoung791.habitpulse.ai.conversation.** { *; }

# Gson - keep tool data classes
-keep class io.github.darrindeyoung791.habitpulse.ai.tools.** { *; }

# Room entities, enums, and type converters — Enum.valueOf(name) requires constant names to be preserved
-keep class io.github.darrindeyoung791.habitpulse.data.model.** { *; }
-keep class io.github.darrindeyoung791.habitpulse.data.database.converter.** { *; }

# Gson - keep annotations and generic signatures
-keepattributes Signature
-keepattributes *Annotation*
