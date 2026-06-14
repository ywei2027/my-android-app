# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.example.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 不混淆敏感数据类（便于安全审计）
-keep class com.example.app.data.remote.** { *; }
-keep class com.example.app.data.local.TokenStorage { *; }
