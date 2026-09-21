package com.coffeetime.service

import com.coffeetime.model.User

interface Authenticatable {

    fun login(
        userId: Int,
        pin: String
    ): User
}