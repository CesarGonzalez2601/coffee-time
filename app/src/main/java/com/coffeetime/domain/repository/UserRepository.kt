package com.coffeetime.domain.repository

import com.coffeetime.domain.model.Credentials
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User

interface UserRepository {

    suspend fun create(
        name: String,
        pinHash: String,
        pinSalt: String,
        role: Role
    ): User

    suspend fun findByUid(uid: String): User?

    suspend fun getCredentials(uid: String): Credentials?

    suspend fun findAll(): List<User>

    suspend fun count(): Int

    suspend fun updateStatus(
        uid: String,
        active: Boolean
    ): Boolean
}
