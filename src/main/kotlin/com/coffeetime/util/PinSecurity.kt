package com.coffeetime.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)

        secureRandom.nextBytes(salt)

        return Base64
            .getEncoder()
            .encodeToString(salt)
    }

    fun hashPin(
        pin: String,
        salt: String
    ): String {

        val saltBytes = Base64
            .getDecoder()
            .decode(salt)

        val specification = PBEKeySpec(
            pin.toCharArray(),
            saltBytes,
            ITERATIONS,
            KEY_LENGTH
        )

        val factory = SecretKeyFactory.getInstance(
            "PBKDF2WithHmacSHA256"
        )

        val hash = factory
            .generateSecret(specification)
            .encoded

        specification.clearPassword()

        return Base64
            .getEncoder()
            .encodeToString(hash)
    }

    fun verifyPin(
        pin: String,
        salt: String,
        expectedHash: String
    ): Boolean {

        val calculatedHash = hashPin(
            pin = pin,
            salt = salt
        )

        val calculatedBytes = Base64
            .getDecoder()
            .decode(calculatedHash)

        val expectedBytes = Base64
            .getDecoder()
            .decode(expectedHash)

        return MessageDigest.isEqual(
            calculatedBytes,
            expectedBytes
        )
    }
}