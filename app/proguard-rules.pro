# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# androidx.security.crypto -> com.google.crypto.tink references these
# compile-time-only (source/class retention) annotations that aren't
# shipped on the runtime classpath; safe to silence per AGP's own
# generated missing_rules.txt.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# kotlinx.serialization keeps the serializer for each @Serializable class,
# which R8 can't discover through reflection alone.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.rephrasegenie.**$$serializer { *; }
-keepclassmembers class com.example.rephrasegenie.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.rephrasegenie.** {
    kotlinx.serialization.KSerializer serializer(...);
}
