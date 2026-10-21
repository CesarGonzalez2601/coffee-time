package com.coffeetime.domain.repository

import com.coffeetime.domain.model.MovimientoInventario
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    suspend fun registerMovement(movimiento: MovimientoInventario)
    fun getAllMovementsFlow(): Flow<List<MovimientoInventario>>
}
