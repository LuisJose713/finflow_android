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
# FinFlow API payloads are serialized by Gson using reflected field names.
# A debug build can pass while a minified release silently breaks JSON fields.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class com.softdevcode.finflow_android.domain.model.** { *; }
-keep interface com.softdevcode.finflow_android.data.remote.api.FinFlowApi { *; }
-keepattributes SourceFile,LineNumberTable
