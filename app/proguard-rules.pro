# Verhindert den IllegalAccessError bei Kotlin-Standard-Bibliotheken
-keep class kotlin.** { *; }
-keep class kotlin.text.** { *; }
-dontwarn kotlin.**