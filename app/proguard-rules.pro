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
# debugging stack traces:
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve annotated classes for Butterknife
-keep class butterknife.** { *; }
-keepnames class * { @butterknife.BindString @butterknife.BindColor @butterknife.BindDimen @butterknife.BindDrawable @butterknife.BindArray <fields>; }

# Preserve enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Activity classes and their methods
-keep public class * extends android.app.Activity
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Preserve Service classes
-keep public class * extends android.app.Service

# Preserve BroadcastReceiver classes
-keep public class * extends android.content.BroadcastReceiver

# Preserve ContentProvider classes
-keep public class * extends android.content.ContentProvider

# Preserve Application classes
-keep public class * extends android.app.Application

# Preserve all fields and methods for classes in the Android support library
-keep class android.support.** { *; }
-keep interface android.support.** { *; }

# Don't warn about missing references to support classes
-dontwarn android.support.**