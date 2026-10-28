package com.coffeetime.domain.model

data class DetalleOrden(
    val id: Int = 0,
    val ordenId: Int = 0,
    val productoId: Int,
    val nombreProducto: String,
    val precioUnitario: Double,
    val cantidad: Int
) {
    val subtotalLinea: Double
        get() = precioUnitario * cantidad
}

data class Orden(
    val id: Int = 0,
    val fechaTimestamp: Long = System.currentTimeMillis(),
    val items: List<DetalleOrden> = emptyList(),
    val subtotal: Double = 0.0,
    val iva: Double = 0.0,
    val total: Double = 0.0,
    val estado: EstadoOrden = EstadoOrden.PENDIENTE,
    val cajeroUid: String
)
