package com.coffeetime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.coffeetime.domain.model.DetalleOrden

@Entity(tableName = OrderEntity.TABLE)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val subtotal: Double,
    val iva: Double,
    val total: Double,
    val status: String,
    @ColumnInfo(name = "cashier_id")
    val cashierId: Int
) {
    companion object {
        const val TABLE = "orders"
    }
}

@Entity(
    tableName = OrderItemEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("order_id")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Int,
    @ColumnInfo(name = "product_id")
    val productId: Int,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,
    val quantity: Int
) {
    fun toDomain(): DetalleOrden = DetalleOrden(
        id = id,
        ordenId = orderId,
        productoId = productId,
        nombreProducto = productName,
        precioUnitario = unitPrice,
        cantidad = quantity
    )

    companion object {
        const val TABLE = "order_items"
    }
}
