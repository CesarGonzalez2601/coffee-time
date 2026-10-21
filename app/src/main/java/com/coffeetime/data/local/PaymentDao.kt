package com.coffeetime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM ${PaymentEntity.TABLE} ORDER BY timestamp DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT COUNT(*) FROM ${PaymentEntity.TABLE}")
    suspend fun getCount(): Int

    @Query("SELECT * FROM ${PaymentEntity.TABLE} WHERE order_id = :orderId LIMIT 1")
    suspend fun getPaymentForOrder(orderId: Int): PaymentEntity?
}
