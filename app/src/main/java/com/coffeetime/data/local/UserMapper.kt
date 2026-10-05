package com.coffeetime.data.local

import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Credentials
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User

fun UserEntity.toDomain(): User = when (role) {
    Role.ADMINISTRATOR -> Administrator(
        id = id,
        name = name,
        active = active
    )
    Role.CASHIER -> Cashier(
        id = id,
        name = name,
        active = active
    )
}

fun UserEntity.toCredentials(): Credentials = Credentials(
    pinHash = pinHash,
    pinSalt = pinSalt
)
