package com.coffeetime.domain.model

class Cashier(
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

    override val role: Role = Role.CASHIER

    override fun hasPermission(permission: Permission): Boolean {

        return when (permission) {

            Permission.CREATE_ORDER,
            Permission.PROCESS_PAYMENT -> true

            else -> false
        }
    }
}