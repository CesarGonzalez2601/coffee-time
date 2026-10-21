package com.coffeetime.domain.model

data class MovimientoInventario(
    val id: Int = 0,
    val productoId: Int,
    val nombreProducto: String,
    val tipo: TipoMovimiento,
    val cantidad: Int,
    val stockAnterior: Int,
    val stockNuevo: Int,
    val motivo: String,
    val fechaTimestamp: Long = System.currentTimeMillis()
)
