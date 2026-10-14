# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room database and DAO classes
-keep class com.hardlinecoding.checkthebox.data.** { *; }
-keep class com.hardlinecoding.checkthebox.model.** { *; }

# Google API Client rules
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.services.calendar.** { *; }
-keep interface com.google.api.client.** { *; }

# Keep GSON related classes
-keep class com.google.gson.** { *; }
-keep class com.google.api.client.json.gson.** { *; }

# Necessary for Google Play Services / Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Prevent obfuscation of generic types for GSON
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes *Annotation*

# Handle potential warnings from Google HTTP client
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
