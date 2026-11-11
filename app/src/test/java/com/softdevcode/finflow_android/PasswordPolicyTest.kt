package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.domain.model.registrationPasswordError
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordPolicyTest {
    @Test
    fun asciiLimitMatchesTheBackendWithoutTruncation() {
        assertNull(registrationPasswordError("a".repeat(72)))
        assertNotNull(registrationPasswordError("a".repeat(73)))
    }

    @Test
    fun accentedCharactersAreCountedAsUtf8Bytes() {
        assertNull(registrationPasswordError("é".repeat(36)))
        assertNotNull(registrationPasswordError("é".repeat(37)))
    }

    @Test
    fun emojiAreCountedAsUtf8Bytes() {
        assertNull(registrationPasswordError("🔒".repeat(18)))
        assertNotNull(registrationPasswordError("🔒".repeat(19)))
    }
}
