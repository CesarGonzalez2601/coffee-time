package com.coffeetime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local. Sin política de migraciones: si cambia el esquema se borra y se
 * vuelve a crear (no hay datos reales que preservar). El seed corre en [SeedCallback].
 */
@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentEntity::class,
        InventoryMovementEntity::class,
        CierreCajaEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun cierreCajaDao(): CierreCajaDao

    companion object {

        private const val NAME = "coffeetime.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context = context.applicationContext,
                klass = AppDatabase::class.java,
                name = NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(SeedCallback())
                .build()
    }
}
