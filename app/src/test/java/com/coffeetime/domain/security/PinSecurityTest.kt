package com.coffeetime.domain.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PinSecurityTest {

    @Test
    fun `verifyPin acepta el PIN con el que se genero el hash`() {
        val salt = PinSecurity.generateSalt()
        val hash = PinSecurity.hashPin(
            pin = "2580",
            salt = salt
        )

        assertTrue(
            PinSecurity.verifyPin(
                pin = "2580",
                salt = salt,
                expectedHash = hash
            )
        )
    }

    @Test
    fun `verifyPin rechaza un PIN distinto`() {
        val salt = PinSecurity.generateSalt()
        val hash = PinSecurity.hashPin(
            pin = "2580",
            salt = salt
        )

        assertFalse(
            PinSecurity.verifyPin(
                pin = "2581",
                salt = salt,
                expectedHash = hash
            )
        )
    }

    @Test
    fun `el mismo PIN con salts distintos produce hashes distintos`() {
        val first = PinSecurity.hashPin(
            pin = "1397",
            salt = PinSecurity.generateSalt()
        )
        val second = PinSecurity.hashPin(
            pin = "1397",
            salt = PinSecurity.generateSalt()
        )

        assertNotEquals(first, second)
    }
}
