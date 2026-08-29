# ==========================================================================
# Reglas ProGuard para ToDus Messenger
# ==========================================================================
# Estas reglas protegen las clases y métodos utilizados por las principales
# libraries del proyecto: Smack (XMPP), Retrofit, Gson y Room.
# ==========================================================================

# ============================================================================
# REGLAS GENERALES
# ============================================================================

# No advertir sobre clases no encontradas en proyectos de bibliotecas
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.-KotlinExtensions

# Mantener las anotaciones para que Hilt/Dagger funcionen correctamente
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin: mantener metadatos necesarios para reflection y corrutinas
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# ============================================================================
# SMACK (Biblioteca XMPP)
# ============================================================================
# Smack utiliza reflection extensivamente para extensiones (XEPs) y proveedores.
# Es necesario mantener todas las clases de Smack y sus subpaquetes.

# Mantener todas las clases de Smack
-keep class org.jivesoftware.smack.** { *; }
-keep interface org.jivesoftware.smack.** { *; }
-keep enum org.jivesoftware.smack.** { *; }

# Mantener los proveedores de IQ (Info/Query) XMPP
-keep class org.jivesoftware.smackx.** { *; }
-keep interface org.jivesoftware.smackx.** { *; }
-keep enum org.jivesoftware.smackx.** { *; }

# Mantener las clases del paquete de extensiones
-keep class org.jivesoftware.smackx.disco.** { *; }
-keep class org.jivesoftware.smackx.muc.** { *; }
-keep class org.jivesoftware.smackx.muclight.** { *; }
-keep class org.jivesoftware.smackx.receipts.** { *; }
-keep class org.jivesoftware.smackx.chatstates.** { *; }
-keep class org.jivesoftware.smackx.xdata.** { *; }
-keep class org.jivesoftware.smackx.ping.** { *; }

# Smack Provider Manager: mantener clases de proveedores registrados
-keep class * implements org.jivesoftware.smack.provider.Provider* { *; }
-keep class * implements org.jivesoftware.smack.provider.IQProvider { *; }
-keep class * implements org.jivesoftware.smack.provider.ExtensionElementProvider { *; }

# Smack Configuration: mantener los archivos de configuración de Smack
-keep class org.jivesoftware.smack.SmackConfiguration { *; }

# jxmpp: usado por Smack para JIDs y Resourceparts
-keep class org.jxmpp.** { *; }

# No eliminar clases enum de Smack
-keepclassmembers enum org.jivesoftware.smack.** {
    **[] $VALUES;
    public *;
}

# ============================================================================
# RETROFIT
# ============================================================================
# Retrofit usa reflection para invocar métodos de la interfaz de servicio.

# Mantener las interfaces de servicio de Retrofit (API)
-keepattributes Signature
-keepattributes Exceptions

# Mantener las interfaces anotadas con @Retrofit (nuestras interfaces API)
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation interface * extends retrofit2.Callback

# No eliminar clases de Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Mantener los adaptadores de RxJava/Coroutine si se usan
-dontwarn retrofit2.adapter.rxjava.**
-keep class retrofit2.adapter.rxjava.** { *; }

# OkHttp: mantener para que funcione correctamente con Retrofit
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okio.**

# ============================================================================
# GSON
# ============================================================================
# Gson utiliza reflection para serializar/deserializar objetos.
# Es crucial mantener las clases de modelo que se serializan con Gson.

# Prevenir el ofuscamiento de clases usadas con Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }

# Mantener clases con la anotación @SerializedName o que Gson necesite
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Mantener campos anotados con @SerializedName (para modelos de datos)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Mantener las clases de dominio que se serializan con Gson
-keep class com.todus.messenger.domain.model.** { *; }

# Mantener las clases de entidad de Room que también usan Gson
-keep class com.todus.messenger.data.local.entity.** { *; }

# Mantener clases de respuesta del servidor (DTOs)
-keep class com.todus.messenger.data.remote.dto.** { *; }

# No advertir sobre clases de Gson
-dontwarn com.google.gson.**
-dontwarn java.lang.invoke.StringConcatFactory

# ============================================================================
# ROOM (ORM de base de datos)
# ============================================================================
# Room genera código en tiempo de compilación que accede a las entidades
# y DAOs mediante reflection. Las reglas de Room son críticas.

# Mantener las clases anotadas con @Entity
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase

# Mantener los DAOs (Data Access Objects)
-keep @androidx.room.Dao interface *

# No eliminar los campos de las entidades anotadas con @ColumnInfo
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
}

# Mantener las columnas de las entidades
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
}

# No eliminar los métodos de los DAOs
-keepclassmembers @androidx.room.Dao interface * {
    *;
}

# Mantener las clases de conversión de tipos (TypeConverters)
-keep class * implements androidx.room.TypeConverter { *; }

# Mantener las relaciones de Room
-keep @androidx.room.Embedded class *
-keep @androidx.room.Relation class *

# No advertir sobre las clases generadas por Room
-dontwarn androidx.room.**

# ============================================================================
# HILT / DAGGER
# ============================================================================
# Hilt genera código que accede a clases anotadas. Proteger las anotaciones.

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Mantener clases anotadas con Hilt
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @javax.inject.Inject class * { *; }

# ============================================================================
# CORRUTINAS Y FLOWS DE KOTLIN
# ============================================================================

# Mantener clases de corrutinas
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================================================
# ANDROIDX COMPOSE
# ============================================================================

# Las clases generadas por el compilador de Compose no deben ofuscar
-dontwarn androidx.compose.**
