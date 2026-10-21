package com.coffeetime.domain.model

data class Pago(
    val id: Int = 0,
    val ordenId: Int,
    val metodoPago: MetodoPago,
    val montoRecibido: Double,
    val cambio: Double,
    val comprobanteCorrelativo: String,
    val fechaTimestamp: Long = System.currentTimeMillis(),
    val cajeroId: Int
)
