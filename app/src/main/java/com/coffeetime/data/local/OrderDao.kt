package com.coffeetime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun createOrder(order: OrderEntity, items: List<OrderItemEntity>): Long {
        val orderId = insertOrder(order)
        val itemsWithOrderId = items.map { it.copy(orderId = orderId.toInt()) }
        insertOrderItems(itemsWithOrderId)
        return orderId
    }

    @Query("SELECT * FROM ${OrderEntity.TABLE} WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Query("SELECT * FROM ${OrderItemEntity.TABLE} WHERE order_id = :orderId")
    suspend fun getItemsForOrder(orderId: Int): List<OrderItemEntity>

    @Query("SELECT * FROM ${OrderEntity.TABLE} ORDER BY timestamp DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM ${OrderEntity.TABLE} WHERE status = :status ORDER BY timestamp DESC")
    fun getOrdersByStatusFlow(status: String): Flow<List<OrderEntity>>

    @Query("UPDATE ${OrderEntity.TABLE} SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: Int, status: String)
}
