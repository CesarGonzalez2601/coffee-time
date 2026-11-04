package com.coffeetime.domain.model

data class Reporte(
    val ingresosTotales: Double,
    val ordenesCerradas: Int,
    val ticketPromedio: Double,
    val top5Productos: List<ProductoVendido>,
    val ventasPorCategoria: Map<String, Double>,
    val ventasPorMetodoPago: Map<String, Double>
)

data class ProductoVendido(
    val nombre: String,
    val cantidad: Int,
    val total: Double
)