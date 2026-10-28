package com.coffeetime.data.fake

import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Credentials
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository
import com.coffeetime.domain.security.PinSecurity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repositorio en memoria para trabajar login, registro y navegación antes de Room (#8).
 * Se reinicia cada vez que se abre la app.
 */
class FakeUserRepository : UserRepository {

    private data class StoredUser(
        val user: User,
        val credentials: Credentials
    )

    private val mutex = Mutex()
    private val users = mutableListOf<StoredUser>()
    private var nextId = 1

    init {
        insert(
            name = "Administrador",
            pin = "2580",
            role = Role.ADMINISTRATOR
        )
        insert(
            name = "Cajero",
            pin = "1397",
            role = Role.CASHIER
        )
    }

    override suspend fun create(
        name: String,
        pinHash: String,
        pinSalt: String,
        role: Role
    ): User = mutex.withLock {
        store(
            name = name,
            credentials = Credentials(
                pinHash = pinHash,
                pinSalt = pinSalt
            ),
            role = role
        )
    }

    override suspend fun findByUid(uid: String): User? = mutex.withLock {
        users.firstOrNull { it.user.uid == uid }?.user
    }

    override suspend fun getCredentials(uid: String): Credentials? = mutex.withLock {
        users.firstOrNull { it.user.uid == uid }?.credentials
    }

    override suspend fun findAll(): List<User> = mutex.withLock {
        users.map { it.user }
    }

    override suspend fun count(): Int = mutex.withLock {
        users.size
    }

    override suspend fun updateStatus(
        uid: String,
        active: Boolean
    ): Boolean = mutex.withLock {
        val index = users.indexOfFirst { it.user.uid == uid }
        if (index == -1) return@withLock false

        val current = users[index]
        users[index] = current.copy(
            user = buildUser(
                uid = current.user.uid,
                name = current.user.name,
                role = current.user.role,
                active = active
            )
        )
        true
    }

    private fun insert(
        name: String,
        pin: String,
        role: Role
    ) {
        val salt = PinSecurity.generateSalt()
        store(
            name = name,
            credentials = Credentials(
                pinHash = PinSecurity.hashPin(
                    pin = pin,
                    salt = salt
                ),
                pinSalt = salt
            ),
            role = role
        )
    }

    private fun store(
        name: String,
        credentials: Credentials,
        role: Role
    ): User {
        val user = buildUser(
            uid = (nextId++).toString(),
            name = name,
            role = role,
            active = true
        )
        users += StoredUser(
            user = user,
            credentials = credentials
        )
        return user
    }

    private fun buildUser(
        uid: String,
        name: String,
        role: Role,
        active: Boolean
    ): User = when (role) {
        Role.ADMINISTRATOR -> Administrator(
            uid = uid,
            name = name,
            email = "",
            active = active
        )
        Role.CASHIER -> Cashier(
            uid = uid,
            name = name,
            email = "",
            active = active
        )
    }
}
