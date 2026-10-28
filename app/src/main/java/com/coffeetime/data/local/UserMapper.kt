package com.coffeetime.data.local

import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Credentials
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User

// Room no guarda correo; desaparece cuando Firestore reemplace a Room (#29).
fun UserEntity.toDomain(): User = when (role) {
    Role.ADMINISTRATOR -> Administrator(
        uid = id.toString(),
        name = name,
        email = "",
        active = active
    )
    Role.CASHIER -> Cashier(
        uid = id.toString(),
        name = name,
        email = "",
        active = active
    )
}

fun UserEntity.toCredentials(): Credentials = Credentials(
    pinHash = pinHash,
    pinSalt = pinSalt
)
