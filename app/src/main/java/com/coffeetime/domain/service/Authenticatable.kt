package com.coffeetime.domain.service

import com.coffeetime.domain.model.User

interface Authenticatable {

    suspend fun login(
        userId: Int,
        pin: String
    ): User

    suspend fun register(
        name: String,
        pin: String
    ): User

    fun logout()
}
