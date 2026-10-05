package com.coffeetime.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.security.PinSecurity

/**
 * Siembra los usuarios de prueba la primera vez que se crea la base (y tras un borrado
 * destructivo). Corre en el hilo de Room, nunca en el principal.
 */
class UserSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        insert(
            db = db,
            id = ADMIN_ID,
            name = "Administrador",
            pin = "2580",
            role = Role.ADMINISTRATOR
        )
        insert(
            db = db,
            id = CASHIER_ID,
            name = "Cajero",
            pin = "1397",
            role = Role.CASHIER
        )

        Log.i(
            TAG,
            "Seeded test users $ADMIN_ID and $CASHIER_ID"
        )
    }

    private fun insert(
        db: SupportSQLiteDatabase,
        id: Int,
        name: String,
        pin: String,
        role: Role
    ) {
        val salt = PinSecurity.generateSalt()

        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("role", role.name)
            put(
                "pin_hash",
                PinSecurity.hashPin(
                    pin = pin,
                    salt = salt
                )
            )
            put("pin_salt", salt)
            put("active", 1)
        }

        db.insert(
            table = UserEntity.TABLE,
            conflictAlgorithm = SQLiteDatabase.CONFLICT_ABORT,
            values = values
        )
    }

    companion object {
        const val ADMIN_ID = 1
        const val CASHIER_ID = 2
        private const val TAG = "UserSeedCallback"
    }
}
