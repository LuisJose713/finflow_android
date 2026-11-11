package com.softdevcode.finflow_android.domain.model

/** BCrypt accepts at most 72 UTF-8 bytes. Never truncate a user's password. */
internal fun registrationPasswordError(password: String): String? =
    if (password.toByteArray(Charsets.UTF_8).size > 72)
        "La contraseña supera el límite de 72 bytes UTF-8. Acórtala; los acentos y emojis pueden ocupar más de un byte."
    else null
