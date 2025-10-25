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

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
# Chaquopy & Python runtime — don't shrink/obfuscate any of it
-keep class com.chaquo.** { *; }
-keep class org.python.** { *; }
# YoutubeDL Android wrappers (both namespaces are used across forks)
-keep class com.yausername.youtubedl_android.** { *; }
-keep class io.github.junkfood02.youtubedl_android.** { *; }
# Common Python/Jython core bits sometimes hit via reflection
-keep class org.python.core.** { *; }
-keep class org.python.modules.** { *; }
# --- Apache Commons Compress (used by ZipUtils.unzip etc.)
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.**
