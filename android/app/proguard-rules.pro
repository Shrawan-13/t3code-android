# Keep the JNI entry points and serialization metadata.
-keep class com.t3codes.core.Native { *; }
-keep class com.t3codes.core.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
