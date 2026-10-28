package com.coffeetime.data.fake

import com.coffeetime.domain.exception.AuthenticationException
import com.coffeetime.domain.model.AuthAccount
import com.coffeetime.domain.service.AuthGateway

/**
 * Firebase Auth en memoria para pruebas JVM. Los tokens de Google aceptados se
 * registran con [addGoogleAccount]; cualquier otro token falla.
 */
class FakeAuthGateway : AuthGateway {

    private data class EmailAccount(
        val account: AuthAccount,
        val password: String
    )

    private val emailAccounts = mutableMapOf<String, EmailAccount>()
    private val googleAccounts = mutableMapOf<String, AuthAccount>()
    private var current: AuthAccount? = null
    private var nextUid = 1

    /** Correos a los que se pidió restablecer la contraseña, en orden. */
    val passwordResets = mutableListOf<String>()

    override fun currentAccount(): AuthAccount? = current

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthAccount {
        val stored = emailAccounts[email.trim().lowercase()]
        if (stored == null || stored.password != password) {
            throw AuthenticationException(
                reason = AuthenticationException.Reason.INVALID_CREDENTIALS,
                message = "Invalid email or password."
            )
        }
        current = stored.account
        return stored.account
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String
    ): AuthAccount {
        val key = email.trim().lowercase()
        if (key in emailAccounts) {
            throw AuthenticationException(
                reason = AuthenticationException.Reason.EMAIL_ALREADY_IN_USE,
                message = "Email already registered."
            )
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw AuthenticationException(
                reason = AuthenticationException.Reason.WEAK_PASSWORD,
                message = "Password must have at least $MIN_PASSWORD_LENGTH characters."
            )
        }
        val account = AuthAccount(
            uid = newUid(),
            email = key,
            displayName = null
        )
        emailAccounts[key] = EmailAccount(
            account = account,
            password = password
        )
        current = account
        return account
    }

    override suspend fun signInWithGoogle(
        idToken: String
    ): AuthAccount {
        val account = googleAccounts[idToken]
            ?: throw AuthenticationException(
                reason = AuthenticationException.Reason.INVALID_CREDENTIALS,
                message = "Invalid Google token."
            )
        current = account
        return account
    }

    override suspend fun sendPasswordReset(
        email: String
    ) {
        // Firebase no avisa si el correo no existe; el fake tampoco.
        passwordResets += email.trim().lowercase()
    }

    override fun signOut() {
        current = null
    }

    /** Simula una cuenta de Google; devuelve el token que la abre. */
    fun addGoogleAccount(
        email: String,
        displayName: String
    ): String {
        val token = "google-token-${nextUid}"
        googleAccounts[token] = AuthAccount(
            uid = newUid(),
            email = email,
            displayName = displayName
        )
        return token
    }

    private fun newUid(): String = "uid-${nextUid++}"

    private companion object {
        // Mismo mínimo que exige Firebase Auth.
        const val MIN_PASSWORD_LENGTH = 6
    }
}
