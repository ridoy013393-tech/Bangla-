# ProGuard / R8 rules for Bangla Chat

# Keep Firestore data models and annotations intact for CustomClassMapper
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.model.** { *; }

# Keep Firebase Firestore & Storage serialization metadata
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
