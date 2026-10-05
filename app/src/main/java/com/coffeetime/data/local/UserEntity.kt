package com.coffeetime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.coffeetime.domain.model.Role

/** Fila de la tabla `users`. Mismas columnas que la tabla de la app de consola (tag etapa-2). */
@Entity(tableName = UserEntity.TABLE)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val role: Role,
    @ColumnInfo(name = "pin_hash")
    val pinHash: String,
    @ColumnInfo(name = "pin_salt")
    val pinSalt: String,
    val active: Boolean = true
) {
    companion object {
        const val TABLE = "users"
    }
}
