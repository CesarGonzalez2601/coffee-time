package com.coffeetime.domain.service

import android.util.Log
import com.coffeetime.domain.exception.IncorrectPinException
import com.coffeetime.domain.exception.UserNotFoundException
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository
import com.coffeetime.domain.security.PinSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthenticationService(
    private val userRepository: UserRepository
) : Authenticatable {

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

        Log.i(
            TAG,
            "User ID $userId logged in successfully"
        )

        return user
    }

    private companion object {
        const val TAG = "AuthenticationService"
    }
}
