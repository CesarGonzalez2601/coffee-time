package com.coffeetime.model

data class ProductoVendido(
    val productoId: Int,
    val nombre: String,
    val unidades: Int,
    val total: Double
)

data class VentaPorCategoria(
    val categoria: CategoriaProducto,
    val unidades: Int,
    val total: Double
)

data class VentaPorMetodoPago(
    val metodoPago: String,
    val cantidadOrdenes: Int,
    val total: Double
)

data class Reporte(
    val desde: String,
    val hasta: String,
    val ingresosTotales: Double,
    val ordenesCerradas: Int,
    val productosMasVendidos: List<ProductoVendido>,
    val ventasPorCategoria: List<VentaPorCategoria>,
    val ventasPorMetodoPago: List<VentaPorMetodoPago>
) {

    val ticketPromedio: Double
        get() = if (ordenesCerradas == 0) {
            0.0
        } else {
            ingresosTotales / ordenesCerradas
        }
}
