package com.coffeetime.data.local

import com.coffeetime.domain.model.MovimientoInventario
import com.coffeetime.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepositoryRoom(
    private val inventoryMovementDao: InventoryMovementDao
) : InventoryRepository {

    override suspend fun registerMovement(movimiento: MovimientoInventario) {
        val entity = InventoryMovementEntity(
            id = movimiento.id,
            productId = movimiento.productoId,
            productName = movimiento.nombreProducto,
            type = movimiento.tipo.name,
            quantity = movimiento.cantidad,
            previousStock = movimiento.stockAnterior,
            newStock = movimiento.stockNuevo,
            reason = movimiento.motivo,
            timestamp = movimiento.fechaTimestamp,
            userUid = movimiento.usuarioUid
        )
        inventoryMovementDao.insertMovement(entity)
    }

    override fun getAllMovementsFlow(): Flow<List<MovimientoInventario>> {
        return inventoryMovementDao.getAllMovementsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }
}
