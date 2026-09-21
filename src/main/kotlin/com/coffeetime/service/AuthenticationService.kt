package com.coffeetime.service

import com.coffeetime.exception.IncorrectPinException
import com.coffeetime.exception.UserNotFoundException
import com.coffeetime.model.User
import com.coffeetime.repository.UserRepository
import com.coffeetime.util.Logger
import com.coffeetime.util.PinSecurity

class AuthenticationService(
    private val userRepository: UserRepository
) : Authenticatable {

    override fun login(
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

        val validPin = PinSecurity.verifyPin(
            pin = pin,
            salt = credentials.pinSalt,
            expectedHash = credentials.pinHash
        )

        if (!validPin) {

            Logger.logError(
                "Incorrect PIN attempt for user ID $userId"
            )

            throw IncorrectPinException(
                "Incorrect PIN."
            )
        }

        Logger.logInfo(
            "User ID $userId logged in successfully"
        )

        return user
    }
}