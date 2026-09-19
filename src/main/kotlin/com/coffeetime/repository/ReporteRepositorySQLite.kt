package com.coffeetime.repository

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.CategoriaProducto
import com.coffeetime.model.ProductoVendido
import com.coffeetime.model.VentaPorCategoria
import com.coffeetime.model.VentaPorMetodoPago

data class ResumenVentas(
    val ordenesCerradas: Int,
    val ingresos: Double
)

class ReporteRepositorySQLite {

    companion object {

        /**
         * Base comun de todos los reportes: los pagos de ordenes PAGADA dentro de la jornada.
         *
         * Se filtra por pagos.fecha_hora (no por ordenes.fecha) para que una orden confirmada
         * a las 23:50 y cobrada a las 00:05 pertenezca al efectivo del dia siguiente, igual que
         * en el cierre de caja. CURRENT_TIMESTAMP de SQLite es UTC, de ahi el 'localtime'.
         *
         * El MIN(p2.id) deja una sola fila por orden aunque existan pagos duplicados historicos.
         *
         * Parametros: 1 = desde, 2 = hasta.
         */
        private val VENTAS_CTE = """
            WITH ventas AS (
                SELECT
                    p.orden_id,
                    p.total_pagado,
                    p.metodo_pago
                FROM pagos p
                INNER JOIN ordenes o ON o.id = p.orden_id
                WHERE o.estado = 'PAGADA'
                  AND date(p.fecha_hora, 'localtime') BETWEEN ? AND ?
                  AND p.id = (
                      SELECT MIN(p2.id)
                      FROM pagos p2
                      WHERE p2.orden_id = p.orden_id
                  )
            )
        """.trimIndent()
    }

    fun obtenerResumen(
        desde: String,
        hasta: String
    ): ResumenVentas {

        val sql = """
            $VENTAS_CTE
            SELECT
                COUNT(DISTINCT orden_id) AS ordenes_cerradas,
                COALESCE(SUM(total_pagado), 0.0) AS ingresos
            FROM ventas
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, desde)
                statement.setString(2, hasta)

                statement.executeQuery().use { result ->
                    if (result.next()) {
                        return ResumenVentas(
                            ordenesCerradas = result.getInt("ordenes_cerradas"),
                            ingresos = result.getDouble("ingresos")
                        )
                    }
                }
            }
        }

        return ResumenVentas(
            ordenesCerradas = 0,
            ingresos = 0.0
        )
    }

    fun obtenerPorMetodoPago(
        desde: String,
        hasta: String
    ): List<VentaPorMetodoPago> {

        val ventas = mutableListOf<VentaPorMetodoPago>()

        val sql = """
            $VENTAS_CTE
            SELECT
                metodo_pago,
                COUNT(*) AS cantidad_ordenes,
                COALESCE(SUM(total_pagado), 0.0) AS total
            FROM ventas
            GROUP BY metodo_pago
            ORDER BY total DESC
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, desde)
                statement.setString(2, hasta)

                statement.executeQuery().use { result ->
                    while (result.next()) {
                        ventas.add(
                            VentaPorMetodoPago(
                                metodoPago = result.getString("metodo_pago"),
                                cantidadOrdenes = result.getInt("cantidad_ordenes"),
                                total = result.getDouble("total")
                            )
                        )
                    }
                }
            }
        }

        return ventas
    }

    /**
     * Los totales salen de detalle_ordenes.subtotal, que es PRE IVA.
     * No suman lo mismo que los ingresos de obtenerResumen (con IVA).
     */
    fun obtenerProductosMasVendidos(
        desde: String,
        hasta: String,
        limite: Int
    ): List<ProductoVendido> {

        val productos = mutableListOf<ProductoVendido>()

        val sql = """
            $VENTAS_CTE
            SELECT
                pr.id,
                pr.nombre,
                SUM(d.cantidad) AS unidades,
                COALESCE(SUM(d.subtotal), 0.0) AS total
            FROM ventas v
            INNER JOIN detalle_ordenes d ON d.orden_id = v.orden_id
            INNER JOIN productos pr ON pr.id = d.producto_id
            GROUP BY pr.id, pr.nombre
            ORDER BY unidades DESC, total DESC
            LIMIT ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, desde)
                statement.setString(2, hasta)
                statement.setInt(3, limite)

                statement.executeQuery().use { result ->
                    while (result.next()) {
                        productos.add(
                            ProductoVendido(
                                productoId = result.getInt("id"),
                                nombre = result.getString("nombre"),
                                unidades = result.getInt("unidades"),
                                total = result.getDouble("total")
                            )
                        )
                    }
                }
            }
        }

        return productos
    }

    /**
     * Totales PRE IVA, igual que obtenerProductosMasVendidos.
     */
    fun obtenerVentasPorCategoria(
        desde: String,
        hasta: String
    ): List<VentaPorCategoria> {

        val ventas = mutableListOf<VentaPorCategoria>()

        val sql = """
            $VENTAS_CTE
            SELECT
                pr.categoria,
                SUM(d.cantidad) AS unidades,
                COALESCE(SUM(d.subtotal), 0.0) AS total
            FROM ventas v
            INNER JOIN detalle_ordenes d ON d.orden_id = v.orden_id
            INNER JOIN productos pr ON pr.id = d.producto_id
            GROUP BY pr.categoria
            ORDER BY total DESC
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, desde)
                statement.setString(2, hasta)

                statement.executeQuery().use { result ->
                    while (result.next()) {
                        ventas.add(
                            VentaPorCategoria(
                                categoria = CategoriaProducto.valueOf(
                                    result.getString("categoria")
                                ),
                                unidades = result.getInt("unidades"),
                                total = result.getDouble("total")
                            )
                        )
                    }
                }
            }
        }

        return ventas
    }
}
