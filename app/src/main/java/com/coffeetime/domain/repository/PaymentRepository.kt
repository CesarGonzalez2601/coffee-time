package com.coffeetime.domain.repository

import com.coffeetime.domain.model.Pago
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    suspend fun processPayment(pago: Pago): String
    fun getAllPaymentsFlow(): Flow<List<Pago>>
    suspend fun getPaymentForOrder(orderId: Int): Pago?
}
