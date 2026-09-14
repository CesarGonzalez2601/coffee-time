package com.coffeetime.model

import java.time.LocalDateTime
import java.util.UUID

data class Pago(
    val ordenId: Int,
    val totalPagado: Double,
    val metodoPago: MetodoPago,
    val fechaHora: LocalDateTime = LocalDateTime.now(),
    val correlativo: String = UUID.randomUUID().toString().substring(0, 8).uppercase()
)