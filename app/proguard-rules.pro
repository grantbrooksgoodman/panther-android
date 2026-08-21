# Panther ("Hello") — R8/ProGuard keep rules for the release build.
#
# The app uses manual encode/decode for all wire models (no reflection),
# so most classes shrink safely. The rules below protect the few paths
# that are reached reflectively or from JavaScript.

# Preserve generic signatures, annotations, and enclosing-method info so
# reflective/coroutine machinery keeps working after shrinking.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# --- WebView translation harness ---------------------------------------
# The translator drives a hidden WebView via androidx.webkit and receives
# results through WebMessageListener callbacks invoked from injected
# JavaScript. Keep the harness services and the webkit callback surface so
# R8 cannot strip the JS-facing methods.
-keep class us.neotechnica.panther.translator.services.** { *; }
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# --- Firebase ----------------------------------------------------------
# Firebase ships its own consumer rules; suppress warnings for optional,
# absent transitive classes referenced by reflection inside the SDK.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- Kotlin / coroutines ----------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Enums crossing the wire (valueOf/values used in decode paths) -----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
