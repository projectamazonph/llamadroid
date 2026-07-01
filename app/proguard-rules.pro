# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.llamadroid.**$$serializer { *; }
-keepclassmembers class com.llamadroid.** { *** Companion; }
-keepclasseswithmembers class com.llamadroid.** { kotlinx.serialization.KSerializer serializer(...); }
