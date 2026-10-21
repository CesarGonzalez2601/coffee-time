package com.coffeetime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.coffeetime.domain.model.MovimientoInventario
import com.coffeetime.domain.model.TipoMovimiento

@Entity(tableName = InventoryMovementEntity.TABLE)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "product_id")
    val productId: Int,
    @ColumnInfo(name = "product_name")
    val productName: String,
    val type: String,
    val quantity: Int,
    @ColumnInfo(name = "previous_stock")
    val previousStock: Int,
    @ColumnInfo(name = "new_stock")
    val newStock: Int,
    val reason: String,
    val timestamp: Long
) {
    fun toDomain(): MovimientoInventario = MovimientoInventario(
        id = id,
        productoId = productId,
        nombreProducto = productName,
        tipo = runCatching { TipoMovimiento.valueOf(type) }.getOrDefault(TipoMovimiento.AJUSTE_MANUAL),
        cantidad = quantity,
        stockAnterior = previousStock,
        stockNuevo = newStock,
        motivo = reason,
        fechaTimestamp = timestamp
    )

    companion object {
        const val TABLE = "inventory_movements"
    }
}
