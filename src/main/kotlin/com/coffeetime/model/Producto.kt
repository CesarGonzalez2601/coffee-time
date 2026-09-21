package com.coffeetime.model

data class Producto(
    val id: Int,
    var nombre: String,
    var precio: Double,
    var categoria: CategoriaProducto,
    var stock: Int
)
