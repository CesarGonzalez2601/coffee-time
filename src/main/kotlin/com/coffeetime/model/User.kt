package com.coffeetime.model

abstract class User(
    val id: Int,
    val name: String,
    val active: Boolean = true
) {

    abstract val role: Role

    abstract fun hasPermission(permission: Permission): Boolean

    override fun toString(): String {
        return "$id - $name ($role)"
    }
}