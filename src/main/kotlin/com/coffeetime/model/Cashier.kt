package com.coffeetime.model

class Cashier(
    id: Int,
    name: String,
    active: Boolean = true
) : User(
    id = id,
    name = name,
    active = active
) {

    override val role: Role = Role.CASHIER

    override fun hasPermission(permission: Permission): Boolean {

        return when (permission) {

            Permission.CREATE_ORDER,
            Permission.PROCESS_PAYMENT -> true

            else -> false
        }
    }
}