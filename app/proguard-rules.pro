# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/lys/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#如果引用了v4或者v7包
-dontwarn android.support.**
-keep class android.support.v7.** { *; }
-keep class android.support.v4.** { *; }
#bugly不混淆
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

-dontwarn org.androidpn.**
-keep class org.androidpn.** { *; }
-dontwarn org.eclipse.**
-keep class org.eclipse.** { *; }
-dontwarn org.MediaPlayer.PlayM4.**
-keep class org.MediaPlayer.PlayM4.** { *; }
-dontwarn com.google.android.**
-keep class com.google.android.** {*;}
#-keep class android.** {*;}
-keep class android.util.** {*;}

-keep class com.judian.jdsmart.common.entity.** {*;}
-keep class com.judian.jdsmart.entity.** {*;}
-dontwarn com.judian.fastjson.**
-keep class com.judian.fastjson.** {*;}
-keep class com.judian.support.config.** {*;}
-keep class com.judian.support.jdbase.** { *;}

-keep class com.alibaba.** {*;}

#//抛出异常时保留代码行号，在异常分析中可以方便定位
-keepattributes SourceFile,LineNumberTable
