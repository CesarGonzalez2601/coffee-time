package com.coffeetime.domain.service

import com.coffeetime.domain.model.AuthAccount

/**
 * Proveedor de identidad (Firebase Auth en la app, [com.coffeetime.data.fake.FakeAuthGateway]
 * en pruebas). Solo autentica: el perfil y el rol viven en [com.coffeetime.domain.repository.UserRepository].
 *
 * Los errores esperados se lanzan como [com.coffeetime.domain.exception.AuthenticationException].
 */
interface AuthGateway {

    /** Cuenta con sesión guardada en el dispositivo, o null si nadie ha iniciado sesión. */
    fun currentAccount(): AuthAccount?

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthAccount

    suspend fun registerWithEmail(
        email: String,
        password: String
    ): AuthAccount

    /**
     * Recibe el ID token que entrega Credential Manager en la UI. La cuenta se crea en
     * Firebase si es la primera vez que entra con Google.
     */
    suspend fun signInWithGoogle(
        idToken: String
    ): AuthAccount

    suspend fun sendPasswordReset(
        email: String
    )

    fun signOut()
}
