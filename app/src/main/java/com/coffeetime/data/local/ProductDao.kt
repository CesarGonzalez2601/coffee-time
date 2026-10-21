package com.coffeetime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM ${ProductEntity.TABLE} WHERE active = 1 ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM ${ProductEntity.TABLE} WHERE active = 1 ORDER BY name ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM ${ProductEntity.TABLE} WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT COUNT(*) FROM ${ProductEntity.TABLE}")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE ${ProductEntity.TABLE} SET stock_actual = :newStock WHERE id = :id")
    suspend fun updateStock(id: Int, newStock: Int)

    @Query("UPDATE ${ProductEntity.TABLE} SET active = 0 WHERE id = :id")
    suspend fun deleteProduct(id: Int)
}
