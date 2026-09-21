package com.coffeetime.model

class Administrator(
    id: Int,
    name: String,
    active: Boolean = true
) : User(
    id = id,
    name = name,
    active = active
) {

    override val role: Role = Role.ADMINISTRATOR

    override fun hasPermission(permission: Permission): Boolean {
        return true
    }
}