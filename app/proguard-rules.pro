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


# 防止混淆FFmpegManger下所有方法和属性
# 保留 FFmpeg 相关类和接口
-keep class com.imcys.bilibilias.ffmpeg.** { *; }

-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* {
   <fields>;
}

# ================= R8 missing-class 忽略规则 =================
#
# 项目移除了闭源依赖（Firebase / Google Play / 百度统计）后，这些类不再出现在
# 依赖图上，但仍有间接引用指向它们，R8 会因此判定为 missing class 而中断构建。
#
# com.google.common（Guava）：
#   kotlinx-coroutines 的 guava 互操作产物自带 ListenableFutureKt.await()，
#   它引用 Guava 的 MoreExecutors / Uninterruptibles。项目从未直接依赖 Guava，
#   该函数也未被使用，属于依赖自带的死代码。
#
# com.google.firebase / com.google.android.*：
#   源码中的 Firebase / Play Core 调用现已指向本仓库内的同名 No-Op 替身；
#   被移除的第三方库若仍有残留引用，同样在此忽略。
-dontwarn com.google.common.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.play.**

