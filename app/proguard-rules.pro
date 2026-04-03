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
-renamesourcefileattribute SourceFile

# ========================================
# SONICWAVE MUSIC PLAYER - CUSTOM PROGUARD RULES
# ========================================

# Keep all model classes for JSON serialization
-keepclassmembers class com.example.madproject.models.** {
    public <fields>;
    public <methods>;
}

# Keep database classes
-keep class com.example.madproject.database.** {
    public <fields>;
    public <methods>;
}

# Keep media player related classes
-keep class com.example.madproject.utils.** {
    public <methods>;
}

# Keep all public methods in utils that might be called via reflection
-keepclassmembers class com.example.madproject.utils.** {
    public <methods>;
}

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Keep Glide classes
-keep class com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule

# Keep all custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# Keep all enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep all Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all custom exceptions
-keep public class * extends java.lang.Exception

# Keep all interface implementations that might be called via reflection
-keep class * implements com.example.madproject.interfaces.** { *; }

# Keep all activity and fragment classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment

# Keep all service classes
-keep public class * extends android.app.Service

# Keep all broadcast receiver classes
-keep public class * extends android.content.BroadcastReceiver

# Keep all content provider classes
-keep public class * extends android.content.ContentProvider

# Keep all adapter classes
-keep public class * extends androidx.recyclerview.widget.RecyclerView.Adapter
-keep public class * extends android.widget.BaseAdapter

# Keep all dialog classes
-keep public class * extends android.app.Dialog
-keep public class * extends androidx.appcompat.app.AlertDialog

# Keep all application classes
-keep public class * extends android.app.Application

# Keep all custom attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep all R classes and resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep all JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep all methods that have OnClick or other event annotations
-keepclassmembers class * {
    @android.view.OnClickListener <methods>;
    @android.view.View.OnLongClickListener <methods>;
    @android.view.View.OnTouchListener <methods>;
}

# Keep all methods with ButterKnife annotations (if used)
-keep class butterknife.** { *; }
-dontwarn butterknife.**

# Keep all methods with Dagger annotations (if used)
-keep class dagger.** { *; }
-dontwarn dagger.**

# Keep all methods with Retrofit annotations (if used)
-keep class retrofit.** { *; }
-dontwarn retrofit.**

# Keep all methods with Gson annotations (if used)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep all methods that might be called via reflection
-keepclassmembers class * {
    @com.example.madproject.utils.KeepForReflection <methods>;
}

# Keep all callback interfaces
-keep class * extends java.lang.reflect.InvocationHandler

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.support.audio.** { *; }
-keep class org.tensorflow.lite.support.label.** { *; }
-keep class org.tensorflow.lite.support.image.** { *; }
-keep class org.tensorflow.lite.support.tensorbuffer.** { *; }

# Keep AutoValue classes for TensorFlow
-keep class com.google.auto.value.** { *; }
-keep class * extends com.google.auto.value.AutoValue { *; }
-keep class * implements com.google.auto.value.AutoValue$Builder { *; }

# Additional security: Remove debug information in release
-keepattributes !DebugInfo,!SourceFile,LineNumberTable

# Obfuscation mapping file output
-printmapping mapping.txt

# Keep all methods in test classes (for testing)
-keep class **Test { *; }
-keep class ***Test { *; }