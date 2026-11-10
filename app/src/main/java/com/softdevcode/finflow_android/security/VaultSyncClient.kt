package com.softdevcode.finflow_android.security

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import kotlinx.coroutines.flow.first
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

/**
 * Experimental E2EE sync client. Deliberately not connected to existing finance screens.
 * Financial values are JSON-encrypted on device before an HTTP request is issued.
 * The recovery key is caller-supplied and must NEVER enter DataStore, logs or the API.
 */
data class VaultOpaqueEntry(
    val id: String,
    val revision: Long,
    val nonce: String,
    val ciphertext: String,
    val updatedAt: String
)

data class VaultPutRequest(
    val nonce: String,
    val ciphertext: String,
    val expectedRevision: Long
)

data class VaultDecryptedEntry(
    val id: String,
    val revision: Long,
    val updatedAt: String,
    val value: JsonElement
)

class VaultSyncClient @Inject constructor(
    private val api: FinFlowApi,
    private val tokens: TokenManager
) {
    suspend fun listPage(
        userId: Long,
        recoveryKey: String,
        after: UUID? = null,
        limit: Int = 100
    ): List<VaultDecryptedEntry> {
        verifyActiveAccount(userId)
        require(limit in 1..250) { "Invalid page size" }
        val entries = api.getVaultEntries(after?.toString(), limit)
        verifyActiveAccount(userId)
        return entries.map { entry ->
            val uuid = UUID.fromString(entry.id)
            val plaintext = VaultCrypto.open(
                VaultCrypto.Envelope(entry.nonce, entry.ciphertext),
                recoveryKey, userId, uuid
            )
            VaultDecryptedEntry(
                entry.id, entry.revision, entry.updatedAt,
                JsonParser.parseString(String(plaintext, StandardCharsets.UTF_8))
            )
        }.also { verifyActiveAccount(userId) }
    }

    suspend fun save(
        userId: Long,
        recoveryKey: String,
        entryId: UUID,
        value: JsonElement,
        expectedRevision: Long = 0
    ): VaultOpaqueEntry {
        verifyActiveAccount(userId)
        require(expectedRevision >= 0) { "Invalid revision" }
        val plaintext = value.toString().toByteArray(StandardCharsets.UTF_8)
        val envelope = VaultCrypto.seal(plaintext, recoveryKey, userId, entryId)
        verifyActiveAccount(userId)
        return api.putVaultEntry(
            entryId.toString(),
            VaultPutRequest(envelope.nonce, envelope.ciphertext, expectedRevision)
        )
    }

    suspend fun remove(userId: Long, entryId: UUID, expectedRevision: Long) {
        verifyActiveAccount(userId)
        require(expectedRevision > 0) { "Invalid revision" }
        api.deleteVaultEntry(entryId.toString(), expectedRevision)
    }

    private suspend fun verifyActiveAccount(userId: Long) {
        require(userId > 0 &&
            tokens.userId.first()?.toLong() == userId &&
            !tokens.getTokenOnce().isNullOrEmpty()) {
            "The authenticated account changed; unlock the vault again"
        }
    }
}
