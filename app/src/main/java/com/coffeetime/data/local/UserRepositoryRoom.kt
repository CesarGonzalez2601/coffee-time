package com.coffeetime.data.local

import com.coffeetime.domain.model.Credentials
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.UserRepository

class UserRepositoryRoom(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun create(
        name: String,
        pinHash: String,
        pinSalt: String,
        role: Role
    ): User {
        val entity = UserEntity(
            name = name,
            role = role,
            pinHash = pinHash,
            pinSalt = pinSalt
        )
        val id = userDao.insert(entity).toInt()
        return entity.copy(id = id).toDomain()
    }

    override suspend fun findByUid(uid: String): User? =
        uid.toIntOrNull()?.let { userDao.findById(it) }?.toDomain()

    override suspend fun getCredentials(uid: String): Credentials? =
        uid.toIntOrNull()?.let { userDao.findById(it) }?.toCredentials()

    override suspend fun findAll(): List<User> =
        userDao.findAll().map { it.toDomain() }

    override suspend fun count(): Int =
        userDao.count()

    override suspend fun updateStatus(
        uid: String,
        active: Boolean
    ): Boolean {
        val id = uid.toIntOrNull() ?: return false
        return userDao.updateActive(
            id = id,
            active = active
        ) > 0
    }
}
