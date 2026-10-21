package com.coffeetime.data.local

import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepositoryRoom(
    private val orderDao: OrderDao
) : OrderRepository {

    override suspend fun createOrder(orden: Orden): Int {
        val orderEntity = OrderEntity(
            id = orden.id,
            timestamp = orden.fechaTimestamp,
            subtotal = orden.subtotal,
            iva = orden.iva,
            total = orden.total,
            status = orden.estado.name,
            cashierId = orden.cajeroId
        )

        val itemEntities = orden.items.map { item ->
            OrderItemEntity(
                id = item.id,
                orderId = 0,
                productId = item.productoId,
                productName = item.nombreProducto,
                unitPrice = item.precioUnitario,
                quantity = item.cantidad
            )
        }

        return orderDao.createOrder(orderEntity, itemEntities).toInt()
    }

    override suspend fun getOrderById(id: Int): Orden? {
        val entity = orderDao.getOrderById(id) ?: return null
        val items = orderDao.getItemsForOrder(id).map { it.toDomain() }
        return Orden(
            id = entity.id,
            fechaTimestamp = entity.timestamp,
            items = items,
            subtotal = entity.subtotal,
            iva = entity.iva,
            total = entity.total,
            estado = runCatching { EstadoOrden.valueOf(entity.status) }.getOrDefault(EstadoOrden.PENDIENTE),
            cajeroId = entity.cashierId
        )
    }

    override fun getAllOrdersFlow(): Flow<List<Orden>> {
        return orderDao.getAllOrdersFlow().map { list ->
            list.map { entity ->
                val items = orderDao.getItemsForOrder(entity.id).map { it.toDomain() }
                Orden(
                    id = entity.id,
                    fechaTimestamp = entity.timestamp,
                    items = items,
                    subtotal = entity.subtotal,
                    iva = entity.iva,
                    total = entity.total,
                    estado = runCatching { EstadoOrden.valueOf(entity.status) }.getOrDefault(EstadoOrden.PENDIENTE),
                    cajeroId = entity.cashierId
                )
            }
        }
    }

    override fun getOrdersByStatusFlow(status: EstadoOrden): Flow<List<Orden>> {
        return orderDao.getOrdersByStatusFlow(status.name).map { list ->
            list.map { entity ->
                val items = orderDao.getItemsForOrder(entity.id).map { it.toDomain() }
                Orden(
                    id = entity.id,
                    fechaTimestamp = entity.timestamp,
                    items = items,
                    subtotal = entity.subtotal,
                    iva = entity.iva,
                    total = entity.total,
                    estado = status,
                    cajeroId = entity.cashierId
                )
            }
        }
    }

    override suspend fun updateOrderStatus(id: Int, status: EstadoOrden) {
        orderDao.updateOrderStatus(id, status.name)
    }
}
