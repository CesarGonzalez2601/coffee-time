package com.coffeetime.domain.model

/** Cuenta autenticada en Firebase Auth. Aún no trae rol: el rol se lee de `users/{uid}`. */
data class AuthAccount(
    val uid: String,
    val email: String,
    val displayName: String?
)
