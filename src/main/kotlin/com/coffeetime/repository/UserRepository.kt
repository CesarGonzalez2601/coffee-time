package com.coffeetime.repository

import com.coffeetime.model.Credentials
import com.coffeetime.model.Role
import com.coffeetime.model.User

interface UserRepository {

    fun create(
        name: String,
        pinHash: String,
        pinSalt: String,
        role: Role
    ): User

    fun findById(id: Int): User?

    fun getCredentials(id: Int): Credentials?

    fun findAll(): List<User>

    fun count(): Int

    fun updateStatus(
        id: Int,
        active: Boolean
    ): Boolean
}