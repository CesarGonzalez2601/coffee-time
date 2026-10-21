package com.coffeetime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Query("SELECT * FROM ${InventoryMovementEntity.TABLE} ORDER BY timestamp DESC")
    fun getAllMovementsFlow(): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM ${InventoryMovementEntity.TABLE} WHERE product_id = :productId ORDER BY timestamp DESC")
    fun getMovementsForProductFlow(productId: Int): Flow<List<InventoryMovementEntity>>
}
