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

    suspend fun findById(id: Int): User?

    suspend fun getCredentials(id: Int): Credentials?

    suspend fun findAll(): List<User>

    suspend fun count(): Int

    suspend fun updateStatus(
        id: Int,
        active: Boolean
    ): Boolean
}
