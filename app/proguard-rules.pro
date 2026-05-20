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

-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Necessary?

# Keep the data model classes
-keep class dev.msuhr.dominionkingdoms.model.** { *; }

# Keep the TypeAdapters so Gson can use them
-keep class * extends com.google.gson.TypeAdapter
-keepnames class dev.msuhr.dominionkingdoms.model.** { *; }

# Keep Enums (Set, Type, Category) from being renamed
-keepclassmembers enum dev.msuhr.dominionkingdoms.model.** { *; }

# Keep TypeAdapters to ensure they work properly
-keep class dev.msuhr.dominionkingdoms.model.CategoryTypeAdapter { *; }
-keep class dev.msuhr.dominionkingdoms.model.SetTypeAdapter { *; }
-keep class dev.msuhr.dominionkingdoms.model.TypeTypeAdapter { *; }