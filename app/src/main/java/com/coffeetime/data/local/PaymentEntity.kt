package com.coffeetime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.model.Pago

@Entity(tableName = PaymentEntity.TABLE)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Int,
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String,
    @ColumnInfo(name = "amount_received")
    val amountReceived: Double,
    val change: Double,
    val correlative: String,
    val timestamp: Long,
    @ColumnInfo(name = "cashier_id")
    val cashierId: Int
) {
    fun toDomain(): Pago = Pago(
        id = id,
        ordenId = orderId,
        metodoPago = runCatching { MetodoPago.valueOf(paymentMethod) }.getOrDefault(MetodoPago.EFECTIVO),
        montoRecibido = amountReceived,
        cambio = change,
        comprobanteCorrelativo = correlative,
        fechaTimestamp = timestamp,
        cajeroId = cashierId
    )

    companion object {
        const val TABLE = "payments"
    }
}
