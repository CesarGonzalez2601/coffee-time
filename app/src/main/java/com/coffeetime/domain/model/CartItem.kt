package com.coffeetime.domain.model

data class CartItem(
    val producto: Producto,
    val cantidad: Int = 1
) {
    val subtotalLinea: Double
        get() = producto.precio * cantidad
}
