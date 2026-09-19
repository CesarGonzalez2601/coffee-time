package com.coffeetime.service

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.Orden
import com.coffeetime.model.Pago
import com.coffeetime.util.Logger

class PagoService(
    private val ordenService: OrdenService,
    private val inventarioService: InventarioService? = null
) {

    fun registrarPago(pago: Pago, orden: Orden): Boolean {
        return try {
            // PagoTarjeta senala el rechazo devolviendo false (PagoEfectivo lanza excepcion).
            // Sin este guard la orden quedaba PAGADA y ensuciaba los reportes.
            if (!pago.metodoPago.procesarPago(orden.total)) {
                Logger.logError(
                    "Pago rechazado para la orden ${orden.id}: metodo ${pago.metodoPago.tipo}"
                )
                return false
            }

            val sql = """
                INSERT INTO pagos (orden_id, total_pagado, metodo_pago, correlativo)
                VALUES (?, ?, ?, ?)
            """.trimIndent()

            DatabaseManager.getConnection().use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setInt(1, pago.ordenId)
                    statement.setDouble(2, pago.totalPagado)
                    statement.setString(3, pago.metodoPago.tipo)
                    statement.setString(4, pago.correlativo)
                    statement.executeUpdate()
                }
            }

            val ordenPagada = ordenService.procesarPagoYConfirmarOrden(orden)

            if (ordenPagada) {
                inventarioService?.descontarStockPorVenta(orden)
            }

            ordenPagada
        } catch (e: Exception) {
            Logger.logError("Pago fallido para la orden ${orden.id}: ${e.message}")
            false
        }
    }
}