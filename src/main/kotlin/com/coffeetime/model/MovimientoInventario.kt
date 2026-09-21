package com.coffeetime.model

enum class TipoMovimiento {
    ENTRADA,
    SALIDA,
    AJUSTE
}

data class MovimientoInventario(
    val id: Int = 0,
    val productoId: Int,
    val tipo: TipoMovimiento,
    val cantidad: Int,
    val motivo: String,
    val fecha: String? = null
)