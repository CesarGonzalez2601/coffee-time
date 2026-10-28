package com.coffeetime.data.fake

import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** `users/{uid}` en memoria, para pruebas JVM junto con [FakeAuthGateway]. */
class FakeUserRepository : UserRepository {

    private val mutex = Mutex()
    private val users = linkedMapOf<String, User>()

    override suspend fun create(
        uid: String,
        name: String,
        email: String,
        role: Role
    ): User = mutex.withLock {
        val user = buildUser(
            uid = uid,
            name = name,
            email = email,
            role = role,
            active = true
        )
        users[uid] = user
        user
    }

    override suspend fun findByUid(uid: String): User? = mutex.withLock {
        users[uid]
    }

    override suspend fun findAll(): List<User> = mutex.withLock {
        users.values.toList()
    }

    override suspend fun updateStatus(
        uid: String,
        active: Boolean
    ): Boolean = mutex.withLock {
        val current = users[uid] ?: return@withLock false
        users[uid] = buildUser(
            uid = current.uid,
            name = current.name,
            email = current.email,
            role = current.role,
            active = active
        )
        true
    }

    /** Simula marcar a alguien como administrador a mano en la consola. */
    suspend fun updateRole(
        uid: String,
        role: Role
    ): Boolean = mutex.withLock {
        val current = users[uid] ?: return@withLock false
        users[uid] = buildUser(
            uid = current.uid,
            name = current.name,
            email = current.email,
            role = role,
            active = current.active
        )
        true
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
}
