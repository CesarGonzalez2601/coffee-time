package com.coffeetime.data.firebase

import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Perfiles en `users/{uid}`. El rol solo se cambia a mano en la consola (o por un
 * administrador); las reglas de `firestore.rules` impiden que alguien cambie el suyo.
 */
class UserRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val users
        get() = firestore.collection(COLLECTION)

    override suspend fun create(
        uid: String,
        name: String,
        email: String,
        role: Role
    ): User {
        users.document(uid)
            .set(
                mapOf(
                    FIELD_NAME to name,
                    FIELD_EMAIL to email,
                    FIELD_ROLE to role.name,
                    FIELD_ACTIVE to true,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp()
                )
            )
            .await()

        return buildUser(
            uid = uid,
            name = name,
            email = email,
            role = role,
            active = true
        )
    }

    override suspend fun findByUid(uid: String): User? =
        users.document(uid)
            .get()
            .await()
            .toUser()

    /** Solo un administrador puede listar a todos (lo exigen las reglas). */
    override suspend fun findAll(): List<User> =
        users.orderBy(FIELD_NAME)
            .get()
            .await()
            .documents
            .mapNotNull { it.toUser() }

    override suspend fun updateStatus(
        uid: String,
        active: Boolean
    ): Boolean {
        val document = users.document(uid)
        if (!document.get().await().exists()) return false

        document.update(FIELD_ACTIVE, active).await()
        return true
    }

    private fun DocumentSnapshot.toUser(): User? {
        if (!exists()) return null

        // Un rol mal escrito a mano en la consola no da permisos de más.
        val role = runCatching { Role.valueOf(getString(FIELD_ROLE).orEmpty()) }
            .getOrDefault(Role.CASHIER)

        return buildUser(
            uid = id,
            name = getString(FIELD_NAME).orEmpty(),
            email = getString(FIELD_EMAIL).orEmpty(),
            role = role,
            active = getBoolean(FIELD_ACTIVE) ?: true
        )
    }

    private fun buildUser(
        uid: String,
        name: String,
        email: String,
        role: Role,
        active: Boolean
    ): User = when (role) {
        Role.ADMINISTRATOR -> Administrator(
            uid = uid,
            name = name,
            email = email,
            active = active
        )
        Role.CASHIER -> Cashier(
            uid = uid,
            name = name,
            email = email,
            active = active
        )
    }

    private companion object {
        const val COLLECTION = "users"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_ACTIVE = "active"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
