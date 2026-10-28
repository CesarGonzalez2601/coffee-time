package com.coffeetime.domain.service

import com.coffeetime.domain.model.User

interface Authenticatable {

    suspend fun login(
        email: String,
        password: String
    ): User

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): User

    suspend fun loginWithGoogle(
        idToken: String
    ): User

    /** Si Firebase guardó una sesión en el dispositivo, la vuelve a abrir sin pedir datos. */
    suspend fun restoreSession(): User?

    suspend fun sendPasswordReset(
        email: String
    )

    fun logout()
}
