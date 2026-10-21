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

        seedProducts(db)
    }

    private fun seedProducts(db: SupportSQLiteDatabase) {
        val initialProducts = listOf(
            Triple("Espresso", "BEBIDAS_CALIENTES" to 2.00, Triple(50, 10, "☕")),
            Triple("Cappuccino", "BEBIDAS_CALIENTES" to 3.25, Triple(40, 10, "☕")),
            Triple("Café Latte", "BEBIDAS_CALIENTES" to 3.50, Triple(35, 10, "🥛")),
            Triple("Americano", "BEBIDAS_CALIENTES" to 2.50, Triple(3, 5, "☕")),
            Triple("Iced Latte", "BEBIDAS_FRIAS" to 3.75, Triple(25, 10, "🧋")),
            Triple("Cold Brew", "BEBIDAS_FRIAS" to 4.00, Triple(2, 5, "🧊")),
            Triple("Frappuccino", "BEBIDAS_FRIAS" to 4.50, Triple(20, 8, "🥤")),
            Triple("Croissant", "REPOSTERIA" to 2.25, Triple(15, 5, "🥐")),
            Triple("Muffin de Arándanos", "REPOSTERIA" to 2.50, Triple(4, 5, "🧁")),
            Triple("Cheesecake", "REPOSTERIA" to 3.80, Triple(10, 3, "🍰")),
            Triple("Sandwich Jamón y Queso", "ALIMENTOS" to 4.25, Triple(12, 4, "🥪")),
            Triple("Panini de Pollo", "ALIMENTOS" to 5.00, Triple(8, 4, "🥙"))
        )

        for ((name, catPrice, stockInfo) in initialProducts) {
            val (category, price) = catPrice
            val (stockActual, stockMinimo, emoji) = stockInfo
            val values = ContentValues().apply {
                put("name", name)
                put("category", category)
                put("price", price)
                put("stock_actual", stockActual)
                put("stock_minimo", stockMinimo)
                put("emoji", emoji)
                put("active", 1)
            }
            db.insert(
                table = ProductEntity.TABLE,
                conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
                values = values
            )
        }
        Log.i(TAG, "Seeded initial products")
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
