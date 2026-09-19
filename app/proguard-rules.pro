# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

# MuPDF (Fitz)
-keep class com.artifex.mupdf.fitz.** {
    <fields>;
    <methods>;
}

# MuPDFCoreExtended
-keepclassmembers class com.artifex.mupdf.viewer.MuPDFCore {
    private int resolution;
    private com.artifex.mupdf.fitz.Document doc;
    private com.artifex.mupdf.fitz.Page page;
    private com.artifex.mupdf.fitz.DisplayList displayList;

    private void gotoPage(int);
}

# General JNI Keep
-keepclasseswithmembernames class * {
    native <methods>;
}
