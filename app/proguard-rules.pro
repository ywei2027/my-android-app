# === Gson 模型类 — 禁止 R8 混淆字段名 ===
-keep class com.example.myandroidapp.data.remote.** { <fields>; }
-keep class com.example.myandroidapp.data.local.** { <fields>; }
-keep class com.example.myandroidapp.domain.model.** { <fields>; }

# === Repository 层 — 保持类名/方法名，防止 ApiException/ArticleNotFoundException 被重命名 ===
-keep class com.example.myandroidapp.data.repository.** { *; }

# === 保留枚举 values() 和 valueOf() — R8-full 模式会移除这些方法 ===
-keepclassmembers enum com.example.myandroidapp.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Retrofit 接口 — 禁止混淆 ===
-keep,allowobfuscation interface com.example.myandroidapp.data.remote.NewsApiService

# === Gson 通用规则 ===
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# === OkHttp / Retrofit ===
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# === Hilt / DI ===
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.**

# === 保留行号（Crash 日志可读） ===
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
