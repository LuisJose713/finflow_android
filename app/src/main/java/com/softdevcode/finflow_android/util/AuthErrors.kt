package com.softdevcode.finflow_android.util

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/** Display actionable API errors without leaking exception text or response HTML. */
internal fun authErrorMessage(failure: Throwable, fallback: String): String {
    if (failure is IOException) {
        return "No pudimos conectar con el servidor. Revisa tu conexión y vuelve a intentarlo."
    }
    if (failure !is HttpException) return fallback

    val body = runCatching {
        JsonParser.parseString(failure.response()?.errorBody()?.string().orEmpty()).asJsonObject
    }.getOrNull()
    fun field(name: String): String? = runCatching {
        body?.get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString?.trim()?.takeIf { it.isNotBlank() && it.length <= 300 && '<' !in it }
    }.getOrNull()

    val code = field("code")
    val message = field("error")
    return when {
        failure.code() == 409 && code == "GOOGLE_REGISTRATION_REQUIRED" ->
            "Crea tu cuenta con Google desde Regístrate para comenzar."
        failure.code() == 409 && code == "GOOGLE_LINK_REQUIRED" ->
            "Ya tienes una cuenta con este correo. Inicia sesión con tu contraseña y vincula Google desde Seguridad."
        failure.code() in setOf(400, 403, 409, 422) && message != null -> message
        failure.code() == 401 -> "No se pudo verificar el acceso. Revisa tus credenciales y vuelve a intentarlo."
        failure.code() == 409 -> "El correo o el nombre de usuario ya está en uso. Revisa los datos o inicia sesión."
        failure.code() == 429 -> "Demasiados intentos. Espera un momento antes de volver a intentarlo."
        else -> fallback
    }
}
