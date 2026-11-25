# SHADOW 3.0 ProGuard Rules
-keep class com.project.shadow.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.mlkit.** { *; }
-keepclassmembers class * implements android.os.Parcelable { *; }
-keepattributes Signature
-keepattributes *Annotation*
