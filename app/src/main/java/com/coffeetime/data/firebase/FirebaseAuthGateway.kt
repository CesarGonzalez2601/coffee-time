package com.coffeetime.data.firebase

import android.util.Log
import com.coffeetime.domain.exception.AuthenticationException
import com.coffeetime.domain.model.AuthAccount
import com.coffeetime.domain.service.AuthGateway
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * [AuthGateway] sobre Firebase Auth. Firebase guarda la sesión en el dispositivo, así
 * que [currentAccount] sigue respondiendo después de cerrar la app.
 */
class FirebaseAuthGateway(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthGateway {

    override fun currentAccount(): AuthAccount? =
        auth.currentUser?.toAccount()

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthAccount = authenticate {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String
    ): AuthAccount = authenticate {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun signInWithGoogle(
        idToken: String
    ): AuthAccount = authenticate {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    override suspend fun sendPasswordReset(
        email: String
    ) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toAuthenticationException()
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    private suspend fun authenticate(
        block: suspend () -> AuthResult
    ): AuthAccount {
        val result = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toAuthenticationException()
        }

        return result.user?.toAccount()
            ?: throw AuthenticationException(
                reason = AuthenticationException.Reason.UNKNOWN,
                message = "Firebase returned no user."
            )
    }

    private fun FirebaseUser.toAccount(): AuthAccount = AuthAccount(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName
    )

    // FirebaseAuthWeakPasswordException hereda de InvalidCredentials: va primero.
    private fun Exception.toAuthenticationException(): AuthenticationException {
        val reason = when (this) {
            is FirebaseAuthWeakPasswordException -> AuthenticationException.Reason.WEAK_PASSWORD
            is FirebaseAuthUserCollisionException -> AuthenticationException.Reason.EMAIL_ALREADY_IN_USE
            is FirebaseAuthInvalidCredentialsException ->
                if (errorCode == ERROR_INVALID_EMAIL) {
                    AuthenticationException.Reason.INVALID_EMAIL
                } else {
                    AuthenticationException.Reason.INVALID_CREDENTIALS
                }
            is FirebaseAuthInvalidUserException ->
                if (errorCode == ERROR_USER_DISABLED) {
                    AuthenticationException.Reason.USER_DISABLED
                } else {
                    AuthenticationException.Reason.INVALID_CREDENTIALS
                }
            is FirebaseNetworkException -> AuthenticationException.Reason.NETWORK
            else -> AuthenticationException.Reason.UNKNOWN
        }

        Log.w(
            TAG,
            "Firebase Auth failed: $reason",
            this
        )

        return AuthenticationException(
            reason = reason,
            message = message ?: reason.name,
            cause = this
        )
    }

    private companion object {
        const val TAG = "FirebaseAuthGateway"
        const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"
        const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
    }
}
