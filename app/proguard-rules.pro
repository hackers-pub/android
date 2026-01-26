# Add project specific ProGuard rules here.

# Apollo GraphQL
-keep class pub.hackers.android.graphql.** { *; }

# Keep data classes
-keepclassmembers class pub.hackers.android.domain.model.** {
    <fields>;
    <init>(...);
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coil
-dontwarn coil.**
