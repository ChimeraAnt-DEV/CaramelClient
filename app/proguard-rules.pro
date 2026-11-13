# R8 rules for the release build.
#
# R8 obfuscates and shrinks the app. Anything the native layer reaches by name must survive:
# a JNI symbol is derived from the *fully-qualified* class and method name, so an obfuscated class
# turns every `native` method into an UnsatisfiedLinkError at the first call. The JniBindingParity
# test scans the real source tree, and CI runs it against the minified build so a missing keep rule
# fails the build instead of the app.

# ---------------------------------------------------------------------------
# JNI-bound classes: keep the class name, the method names and the native methods.
# ---------------------------------------------------------------------------

# The sweetloader bridge (Java -> libsweetloader.so). Every public method here is either a native
# method or called from a native callback by name.
#
# The package is org.chimeramc.client.preloader, NOT ...client.launcher.sweetloader: the files sit in
# a `launcher/sweetloader/` directory, but the package declaration (and therefore the mangled JNI
# symbol) is `sweetloader`. Keeping the wrong path left every native method reachable only through the
# broad `native <methods>` rule below — which keeps the method but still lets R8 rename the class,
# so `Java_org_chimeramc_client_preloader_PreloaderInput_*` would no longer resolve. The directory
# segment is not part of the name; keep the package.
-keep class org.chimeramc.client.preloader.** { *; }

# The .universe / .singularity engine JNI surface (Java -> libcaramel_universe.so and
# libcaramel_singularity.so). The JNI symbols encode the FQN and every method name, so both the
# package and the class must survive R8 untouched.
-keep class org.chimeramc.client.universe.** { *; }
-keep class org.chimeramc.client.singularity.** { *; }

# The JNI helpers under core.jni.
-keep class org.chimeramc.client.core.jni.** { *; }

# The mod loader's lib bindings (io.bambosan.mbloader.launcherUtils.LibBindings).
-keep class io.bambosan.mbloader.launcherUtils.** { *; }


# Keep every declared native method and its owning class, anywhere in the app, so a new binding
# cannot be silently obfuscated.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ---------------------------------------------------------------------------
# Classes reached by name from resources, the manifest or reflection.
# ---------------------------------------------------------------------------

# Activities, services and receivers are instantiated by the framework from their manifest name.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application
-keep public class * extends android.app.backup.BackupAgentHelper

# Custom Views inflated from XML need their (Context, AttributeSet) constructors.
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Enums are read by name (valueOf) in several persisted formats.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable CREATOR fields are read by name.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable classes and their fields (controller profiles, manifests).
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------------------------------------------------------------------
# Third-party libraries that rely on reflection / annotations.
# ---------------------------------------------------------------------------

# Gson reads and writes fields by name; the model classes are serialized into SharedPreferences
# (controller profiles, settings backups, instance metadata).
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp / Okio ship their own consumer rules, but keep the platform checks it reflects on.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# MSAL (Microsoft authentication) reflects on its own configuration classes.
-keep class com.microsoft.identity.** { *; }
-keep interface com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**

# LuaJ (script host) loads its classes reflectively.
-keep class org.luaj.** { *; }
-dontwarn org.luaj.**

# xCrash registers its native handlers by class name.
-keep class com.iqiyi.xcrash.** { *; }
-dontwarn com.iqiyi.xcrash.**

# Firebase messaging is referenced from the manifest by name.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Glide generates its API by annotation processing.
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Concentus (Opus) is reached through reflection by Class.forName in VoiceCodec, so the class name
# must survive.
-keep class org.concentus.** { *; }
-dontwarn org.concentus.**

# ---------------------------------------------------------------------------
# Keep line numbers so a crash report maps to a real source line.
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Optional dependencies of the crypto and HTTP stacks that do not exist on Android.
#
# R8 fails the build (not just warns) on a missing class reached from kept code. Each of these is a
# desktop-JVM-only type: nimbus-jose reaches for tink's Ed25519/X25519 primitives only when a caller
# asks for that backend, Apache HttpClient's hostname verifier reaches javax.naming for DN parsing
# that the Android runtime does not provide, and the jgss/blockhound types are never on the classpath.
# The Android code paths do not use them, so they are declared absent rather than bundled.
-dontwarn com.google.crypto.tink.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn reactor.blockhound.**
