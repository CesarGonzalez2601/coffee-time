package com.coffeetime.domain.repository

import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.Orden
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createOrder(orden: Orden): Int
    suspend fun getOrderById(id: Int): Orden?
    fun getAllOrdersFlow(): Flow<List<Orden>>
    fun getOrdersByStatusFlow(status: EstadoOrden): Flow<List<Orden>>
    suspend fun updateOrderStatus(id: Int, status: EstadoOrden)
}
