package com.coffeetime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Int): UserEntity?

    @Query("SELECT * FROM users ORDER BY id")
    suspend fun findAll(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("UPDATE users SET active = :active WHERE id = :id")
    suspend fun updateActive(
        id: Int,
        active: Boolean
    ): Int
}
