package com.coffeetime.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Siembra el catálogo inicial la primera vez que se crea la base (y tras un borrado
 * destructivo). Los usuarios ya no viven aquí: vienen de Firebase. Corre en el hilo de Room.
 */
class SeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

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

    companion object {
        private const val TAG = "SeedCallback"
    }
}
