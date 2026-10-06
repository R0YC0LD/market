# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.fuse9.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
