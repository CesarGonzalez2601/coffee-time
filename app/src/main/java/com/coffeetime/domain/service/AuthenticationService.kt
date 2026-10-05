package com.coffeetime.domain.service

import android.util.Log
import com.coffeetime.domain.exception.IncorrectPinException
import com.coffeetime.domain.exception.InvalidInputException
import com.coffeetime.domain.exception.UserNotFoundException
import com.coffeetime.domain.model.Credentials
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository
import com.coffeetime.domain.security.PinSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthenticationService(
    private val userRepository: UserRepository,
    private val session: Session
) : Authenticatable {

    /** Valida ID y PIN y, si son correctos, inicia la sesión. */
    override suspend fun login(
        userId: Int,
        pin: String
    ): User {

        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException(
                "User with ID $userId was not found."
            )

        if (!user.active) {
            throw UserNotFoundException(
                "User is inactive."
            )
        }

        val credentials =
            userRepository.getCredentials(userId)
                ?: throw UserNotFoundException(
                    "Credentials were not found."
                )

        // PBKDF2 con 120k iteraciones tarda; fuera del hilo principal.
        val validPin = withContext(Dispatchers.Default) {
            PinSecurity.verifyPin(
                pin = pin,
                salt = credentials.pinSalt,
                expectedHash = credentials.pinHash
            )
        }

        if (!validPin) {

            Log.w(
                TAG,
                "Incorrect PIN attempt for user ID $userId"
            )

            throw IncorrectPinException(
                "Incorrect PIN."
            )
        }

        session.start(user)

        Log.i(
            TAG,
            "User ID $userId logged in successfully"
        )

        return user
    }

    /**
     * Crea una cuenta siempre con rol CASHIER y devuelve el usuario con su ID nuevo.
     * Las validaciones de la pantalla (largo del nombre, PIN débil, confirmación) van
     * antes, en el registro (#10); aquí solo se rechaza lo que nunca debe llegar.
     */
    override suspend fun register(
        name: String,
        pin: String
    ): User {

        val cleanName = name.trim()

        if (cleanName.isEmpty()) {
            throw InvalidInputException(
                "Name is required."
            )
        }

        if (!PIN_FORMAT.matches(pin)) {
            throw InvalidInputException(
                "PIN must be exactly $PIN_LENGTH digits."
            )
        }

        val credentials = withContext(Dispatchers.Default) {
            val salt = PinSecurity.generateSalt()
            Credentials(
                pinHash = PinSecurity.hashPin(
                    pin = pin,
                    salt = salt
                ),
                pinSalt = salt
            )
        }

        val user = userRepository.create(
            name = cleanName,
            pinHash = credentials.pinHash,
            pinSalt = credentials.pinSalt,
            role = Role.CASHIER
        )

        Log.i(
            TAG,
            "Registered cashier with ID ${user.id}"
        )

        return user
    }

    override fun logout() {
        session.logout()
    }

    private companion object {
        const val TAG = "AuthenticationService"
        const val PIN_LENGTH = 4
        val PIN_FORMAT = Regex("\\d{$PIN_LENGTH}")
    }
}
