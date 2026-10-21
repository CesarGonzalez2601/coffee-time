package com.coffeetime.domain.model

data class Producto(
    val id: Int = 0,
    val nombre: String,
    val categoria: CategoriaProducto,
    val precio: Double,
    val stockActual: Int,
    val stockMinimo: Int = 5,
    val emoji: String = "☕",
    val activo: Boolean = true
) {
    val isStockBajo: Boolean
        get() = stockActual <= stockMinimo
}
