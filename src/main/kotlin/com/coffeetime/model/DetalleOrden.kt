package com.coffeetime.model

data class DetalleOrden(
    val producto: Producto,
    var cantidad: Int
) {
    val subtotalItem: Double
        get() = producto.precio * cantidad
}
