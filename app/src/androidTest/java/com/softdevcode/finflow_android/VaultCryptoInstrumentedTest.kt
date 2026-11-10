package com.softdevcode.finflow_android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softdevcode.finflow_android.security.VaultCrypto
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultCryptoInstrumentedTest {
    private val id = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @Test fun sharedFixtureDecrypts() {
        val envelope = VaultCrypto.Envelope(
            "AAECAwQFBgcICQoL",
            "PCCleK2Ar3qve6ank4IRA-f0vRaECT4SSwaG8XQGbpAtMs-RwLR87FaeXdy4txgW3mlCoXiyxqlc5UNpbIqagNIG5CqhpEMDfXbXACruDKF-VSL4upJME7F3xQ"
        )
        val clear = VaultCrypto.open(envelope, "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8", 7L, id)
        assertEquals(
            """{"schema":1,"kind":"transaction","amount":"1000.00","description":"Prueba"}""",
            String(clear, StandardCharsets.UTF_8)
        )
    }

    @Test fun wrongUserCannotDecrypt() {
        val key = VaultCrypto.newRecoveryKey()
        val value = VaultCrypto.seal("private".toByteArray(), key, 7L, id)
        assertThrows(Exception::class.java) { VaultCrypto.open(value, key, 8L, id) }
    }
}
