package com.coffeetime.domain.model

class Administrator(
    uid: String,
    name: String,
    email: String,
    active: Boolean = true
) : User(
    uid = uid,
    name = name,
    email = email,
    active = active
) {

    override val role: Role = Role.ADMINISTRATOR

    override fun hasPermission(permission: Permission): Boolean {
        return true
    }
}