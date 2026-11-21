# Add project specific ProGuard rules here.
# Keep Firestore / Firebase / MLKit / Credential Manager models & reflection targets intact for release builds.

-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep @Keep annotated classes and members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep TournaX Firestore models intact for DocumentSnapshot deserialization
-keepclassmembers class com.example.data.model.** {
    <fields>;
    <init>(...);
    public *;
}

# Google Identity / Credential Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ML Kit Face Detection
-keep class com.google.mlkit.vision.face.** { *; }
-dontwarn com.google.mlkit.vision.face.**
