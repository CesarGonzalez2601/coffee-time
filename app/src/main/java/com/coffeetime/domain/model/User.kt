package com.coffeetime.domain.model

/**
 * [uid] es el identificador de Firebase Auth y la llave del documento `users/{uid}`.
 * El rol vive en Firestore; nadie lo deduce del correo.
 */
abstract class User(
    val uid: String,
    val name: String,
    val email: String,
    val active: Boolean = true
) {

    abstract val role: Role

    abstract fun hasPermission(permission: Permission): Boolean

    override fun toString(): String {
        return "$uid - $name ($role)"
    }
}
