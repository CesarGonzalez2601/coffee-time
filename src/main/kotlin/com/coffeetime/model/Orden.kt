package com.coffeetime.model

data class Orden(
    val id: Int,
    val detalles: MutableList<DetalleOrden>,
    var estado: EstadoOrden
) {
    val subtotal: Double
        get() = detalles.sumOf { it.subtotalItem }

    val impuesto: Double
        get() = subtotal * 0.10

    val total: Double
        get() = subtotal + impuesto
}
