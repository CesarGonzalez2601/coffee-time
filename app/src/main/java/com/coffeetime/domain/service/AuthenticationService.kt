package com.coffeetime.domain.service

import android.util.Log
import com.coffeetime.domain.exception.AuthenticationException
import com.coffeetime.domain.exception.InvalidInputException
import com.coffeetime.domain.model.AuthAccount
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException

/**
 * Único punto de entrada de autenticación. [AuthGateway] valida la identidad y
 * [UserRepository] aporta el perfil con su rol; con ambos se abre la [Session].
 */
class AuthenticationService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
    private val session: Session
) : Authenticatable {

    override suspend fun login(
        email: String,
        password: String
    ): User {

        val account = authGateway.signInWithEmail(
            email = requireEmail(email),
            password = password
        )

        return openSession(account)
    }

    /**
     * Crea la cuenta en Firebase Auth y su perfil, siempre con rol CASHIER.
     * Las validaciones de la pantalla (largo del nombre, contraseña, confirmación) van
     * antes, en el registro; aquí solo se rechaza lo que nunca debe llegar.
     */
    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): User {

        val cleanName = name.trim()

        if (cleanName.isEmpty()) {
            throw InvalidInputException(
                "Name is required."
            )
        }

        val account = authGateway.registerWithEmail(
            email = requireEmail(email),
            password = password
        )

        val user = userRepository.create(
            uid = account.uid,
            name = cleanName,
            email = account.email,
            role = Role.CASHIER
        )

        session.start(user)

        Log.i(
            TAG,
            "Registered cashier ${user.uid}"
        )

        return user
    }

    /** La primera vez que alguien entra con Google se le crea su perfil de cajero. */
    override suspend fun loginWithGoogle(
        idToken: String
    ): User {

        val account = authGateway.signInWithGoogle(
            idToken = idToken
        )

        return openSession(account)
    }

    override suspend fun restoreSession(): User? {

        val account = authGateway.currentAccount() ?: return null

        return try {
            openSession(account)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Could not restore session for ${account.uid}",
                e
            )
            null
        }
    }

    override suspend fun sendPasswordReset(
        email: String
    ) {
        authGateway.sendPasswordReset(
            email = requireEmail(email)
        )
    }

    override fun logout() {
        authGateway.signOut()
        session.logout()
    }

    /**
     * Busca el perfil de la cuenta y abre la sesión. Si la cuenta existe en Auth pero
     * no tiene perfil (primer acceso con Google, o un registro que se cortó), se crea
     * como cajero.
     */
    private suspend fun openSession(
        account: AuthAccount
    ): User {

        val user = userRepository.findByUid(account.uid)
            ?: userRepository.create(
                uid = account.uid,
                name = defaultName(account),
                email = account.email,
                role = Role.CASHIER
            )

        if (!user.active) {
            authGateway.signOut()
            throw AuthenticationException(
                reason = AuthenticationException.Reason.USER_DISABLED,
                message = "User ${account.uid} is inactive."
            )
        }

        session.start(user)

        Log.i(
            TAG,
            "User ${user.uid} logged in"
        )

        return user
    }

    private fun requireEmail(
        email: String
    ): String {

        val cleanEmail = email.trim()

        if (cleanEmail.isEmpty()) {
            throw InvalidInputException(
                "Email is required."
            )
        }

        return cleanEmail
    }

    private fun defaultName(
        account: AuthAccount
    ): String =
        account.displayName
            ?.takeIf { it.isNotBlank() }
            ?: account.email.substringBefore('@')

    private companion object {
        const val TAG = "AuthenticationService"
    }
}
