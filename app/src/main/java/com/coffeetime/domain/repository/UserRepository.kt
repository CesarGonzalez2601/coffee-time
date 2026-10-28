package com.coffeetime.domain.repository

import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User

/** Perfil y rol de cada cuenta (`users/{uid}` en Firestore). La contraseña la guarda Firebase Auth. */
interface UserRepository {

    suspend fun create(
        uid: String,
        name: String,
        email: String,
        role: Role
    ): User

    suspend fun findByUid(uid: String): User?

    suspend fun findAll(): List<User>

    suspend fun updateStatus(
        uid: String,
        active: Boolean
    ): Boolean
}
