package com.softdevcode.finflow_android.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Local-only AES-256-GCM; no encryption key is sent to the backend or Neon.
 * This primitive is not yet wired to existing repositories or user onboarding.
 */
object VaultCrypto {
    data class Envelope(val nonce: String, val ciphertext: String)
    private const val AAD_PREFIX = "finflow:private-vault:v1:"
    private val random = SecureRandom()

    fun newRecoveryKey(): String = ByteArray(32).also(random::nextBytes).encode()

    fun seal(clear: ByteArray, recoveryKey: String, userId: Long, entryId: UUID): Envelope {
        val key = key(recoveryKey)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad(userId, entryId))
        return Envelope(nonce.encode(), cipher.doFinal(clear).encode())
    }

    fun open(envelope: Envelope, recoveryKey: String, userId: Long, entryId: UUID): ByteArray {
        val key = key(recoveryKey)
        val nonce = envelope.nonce.decode()
        val bytes = envelope.ciphertext.decode()
        require(nonce.size == 12 && bytes.size >= 16) { "Invalid E2EE envelope" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad(userId, entryId))
        return cipher.doFinal(bytes)
    }

    private fun aad(userId: Long, entryId: UUID): ByteArray {
        require(userId > 0) { "Invalid E2EE account id" }
        return (AAD_PREFIX + userId + ":" + entryId).toByteArray(StandardCharsets.UTF_8)
    }

    private fun key(encoded: String): ByteArray = encoded.decode().also {
        require(it.size == 32) { "Recovery key must contain 256 bits" }
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(
        this, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )

    private fun String.decode(): ByteArray {
        require(isNotEmpty() && all {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_'
        }) { "Invalid base64url" }
        return Base64.decode(this, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
