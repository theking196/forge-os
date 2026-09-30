# Forge OS ProGuard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * { public protected *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.forge.os.data.remote.dto.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.forge.os.domain.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent

# Chaquopy (Python runtime)
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# Kotlinx-Serialization
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.forge.os.**$$serializer { *; }
-keepclassmembers class com.forge.os.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.forge.os.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.flow.**

# Forge data + domain models touched by reflection
-keep class com.forge.os.domain.config.** { *; }
-keep class com.forge.os.domain.memory.** { *; }
-keep class com.forge.os.domain.cron.** { *; }
-keep class com.forge.os.domain.plugins.** { *; }
-keep class com.forge.os.domain.agents.** { *; }

# WorkManager Hilt workers
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Strip verbose debug logs in release
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}


# ── R8 missing-class suppressions ─────────────────────────────────────
# R8 treats unresolved references as a hard error and aborts
# assembleRelease. Every class below is reachable only from code paths that
# cannot execute on Android, so suppressing is safe and costs no behaviour.
#
# This set was derived from the real CI log of run 36712652734 (32 missing
# classes) and verified mechanically to cover all 32.
#
# JGit (org.eclipse.jgit, used by GitRunner.kt + SnapshotManager.kt):
#   javax.management / java.lang.management -> JMX monitoring hooks
#     (WindowCache.publishMBeanIfNeeded); JMX is never enabled on Android.
#   org.ietf.jgss.* -> Kerberos GSS-API, reachable only via the HTTP
#     "Negotiate" auth method, which Forge never enables.
#   Transport/crypto hooks resolve reflectively, so JGit needs a keep rule.
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn javax.management.**
-dontwarn java.lang.management.**
-dontwarn org.ietf.jgss.**
-dontwarn java.beans.**
-dontwarn org.slf4j.**
# Optional SSH transports / SLF4J binding JGit probes for but we never bundle.
-dontwarn org.apache.sshd.**
-dontwarn com.jcraft.jsch.**
-dontwarn org.slf4j.impl.**

# Tink (transitively via androidx.security:security-crypto, used by
# SecureKeyStore.kt and BackupManager.kt for EncryptedFile /
# EncryptedSharedPreferences):
#   com.google.api.client.http.* -> optional transport used solely by
#     KeysDownloader.fetchAndCacheData() to pull remote keysets. We never call
#     KeysDownloader; all keysets are local.
#   com.google.errorprone.annotations.* -> compile-time annotations, absent at runtime.
#   org.joda.time.* -> touched only by KeysDownloader.
-dontwarn com.google.api.client.http.**
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.joda.time.**

