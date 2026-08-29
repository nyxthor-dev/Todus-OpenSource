package com.todus.messenger.data.remote.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio de autenticación ToDus.
 *
 * Replica el flujo de auth/token de la app oficial ToDus v2.1.1:
 * 1. Genera un UUID aleatorio y deriva un secret (primeros 32 chars sin guiones)
 * 2. Codifica phone + secret en protobuf (fields 1 y 2, wire type 2)
 * 3. POST a https://auth.todus.cu/v2/auth/token
 * 4. Extrae el JWT de la respuesta binaria
 *
 * El JWT se usa como contraseña para SASL PLAIN en XMPP.
 */
@Singleton
class ToDusAuthService @Inject constructor() {

    companion object {
        private const val TAG = "ToDusAuthService"
        private const val AUTH_URL = "https://auth.todus.cu/v2/auth/token"
        private const val USER_AGENT = "ToDus 2.1.1"
        private const val MEDIA_TYPE_OCTET = "application/octet-stream"
        private val JWT_PATTERN = Pattern.compile("eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .build()
    }

    data class AuthResult(val jwt: String, val phone: String)

    /**
     * Obtiene un token JWT de ToDus usando solo el número de teléfono.
     *
     * @param phoneNumber 10 dígitos, ej: "5353715614"
     * @return [AuthResult] con JWT y teléfono, o excepción si falla
     */
    suspend fun authenticate(phoneNumber: String): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            try {
                val uuid = UUID.randomUUID().toString()
                val secret = uuid.replace("-", "").take(32)

                Log.d(TAG, "Solicitando token para $phoneNumber")

                val protobuf = buildProtobuf(phoneNumber, secret)

                val request = Request.Builder()
                    .url(AUTH_URL)
                    .post(protobuf.toRequestBody(MEDIA_TYPE_OCTET.toMediaType()))
                    .header("Content-Type", MEDIA_TYPE_OCTET)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyBytes = response.body?.bytes()
                    ?: throw IllegalStateException("Respuesta vacía del servidor")

                Log.d(TAG, "Auth status: ${response.code}, body: ${bodyBytes.size} bytes")

                if (!response.isSuccessful) {
                    throw IllegalStateException("Error del servidor: ${response.code}")
                }

                val bodyStr = String(bodyBytes, Charsets.UTF_8)
                val matcher = JWT_PATTERN.matcher(bodyStr)

                if (!matcher.find()) {
                    Log.e(TAG, "Sin JWT en respuesta: ${bodyStr.take(200)}")
                    throw IllegalStateException("No se pudo extraer el token")
                }

                val jwt = matcher.group()
                Log.d(TAG, "JWT obtenido (${jwt.length} chars)")

                Result.success(AuthResult(jwt = jwt, phone = phoneNumber))
            } catch (e: Exception) {
                Log.e(TAG, "Error auth: ${e.message}", e)
                Result.failure(e)
            }
        }

    // --- Protobuf manual (sin librería externa) ---

    /**
     * Field 1 (phone): tag 0x0A, varint length, bytes
     * Field 2 (secret): tag 0x12, varint length, bytes
     */
    private fun buildProtobuf(field1Value: String, field2Value: String): ByteArray {
        val f1 = field1Value.encodeToByteArray()
        val f2 = field2Value.encodeToByteArray()
        val out = ByteArrayOutputStream()

        out.write(0x0A)
        writeVarint(out, f1.size)
        out.write(f1)

        out.write(0x12)
        writeVarint(out, f2.size)
        out.write(f2)

        return out.toByteArray()
    }

    /** Varint base-128 encoding */
    private fun writeVarint(out: ByteArrayOutputStream, value: Int) {
        var v = value
        while (v > 0x7F) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }
}