# EventFinder proguard rules (prototype: shrinking disabled, rules kept for release builds)
-keepattributes *Annotation*

# Retrofit / Gson reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class com.eventfinder.app.data.remote.dto.** { *; }

# Retrofit 2.9 under R8 full mode (AGP 8 default): without these, service
# interfaces lose their generic return types and every call fails with
# "Class cannot be cast to ParameterizedType". Rules taken from Retrofit's
# own retrofit2.pro (https://github.com/square/retrofit, R8 full mode section).
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Classes Gson reads and writes by field name. Renaming their fields would
# break the API models, saved searches, collections and the route cache.
-keep class com.eventfinder.app.data.remote.** { <fields>; <init>(...); }
-keep class com.eventfinder.app.data.store.** { <fields>; <init>(...); }
-keep class com.eventfinder.app.data.repository.RouteOption { <fields>; <init>(...); }
-keep class com.eventfinder.app.data.repository.RoutePoint { <fields>; <init>(...); }
-keep class com.eventfinder.app.data.repository.RouteRepository$* { <fields>; <init>(...); }

# Firebase (Firestore / Storage / FCM) ships its own consumer rules; keep
# our mapping helpers so ProGuard never strips the Firestore document maps.
-keep class com.eventfinder.app.data.cloud.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }