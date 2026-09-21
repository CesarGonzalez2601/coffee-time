package com.coffeetime.service

import com.coffeetime.model.MovimientoInventario
import com.coffeetime.model.Orden
import com.coffeetime.model.Producto
import com.coffeetime.model.TipoMovimiento
import com.coffeetime.repository.InventarioRepositorySQLite
import com.coffeetime.repository.ProductoRepositorySQLite
import com.coffeetime.util.Logger

class InventarioService(
    private val inventarioRepo: InventarioRepositorySQLite,
    private val productoRepo: ProductoRepositorySQLite
) {

    companion object {
        const val LIMITE_STOCK_CRITICO = 5
    }

    fun consultarStock(productoId: Int): Int {
        val stock = inventarioRepo.obtenerStock(productoId)
        if (stock == -1) {
            Logger.logError("Consulta de stock fallida: El producto con ID $productoId no existe.")
        }
        return stock
    }

    fun validarDisponibilidad(productoId: Int, cantidadRequerida: Int): Boolean {
        if (cantidadRequerida <= 0) return false
        val stockActual = consultarStock(productoId)
        return stockActual >= cantidadRequerida
    }

    fun aumentarStock(productoId: Int, cantidad: Int, motivo: String = "Reabastecimiento"): Boolean {
        if (cantidad <= 0) {
            Logger.logError("Aumento de stock fallido: Cantidad inválida ($cantidad) para producto $productoId.")
            return false
        }

        val stockActual = consultarStock(productoId)
        if (stockActual == -1) return false

        val nuevoStock = stockActual + cantidad
        val actualizado = inventarioRepo.actualizarStock(productoId, nuevoStock)

        if (actualizado) {
            inventarioRepo.registrarMovimiento(
                MovimientoInventario(
                    productoId = productoId,
                    tipo = TipoMovimiento.ENTRADA,
                    cantidad = cantidad,
                    motivo = motivo
                )
            )
        } else {
            Logger.logError("Error al actualizar la base de datos para incrementar stock de producto $productoId.")
        }

        return actualizado
    }

    fun disminuirStock(productoId: Int, cantidad: Int, motivo: String = "Ajuste de salida"): Boolean {
        if (cantidad <= 0) {
            Logger.logError("Disminución de stock fallida: Cantidad inválida ($cantidad) para producto $productoId.")
            return false
        }

        val stockActual = consultarStock(productoId)
        if (stockActual < cantidad) {
            Logger.logError("Stock insuficiente para el producto $productoId. Solicitado: $cantidad, Disponible: $stockActual.")
            return false
        }

        val nuevoStock = stockActual - cantidad
        val actualizado = inventarioRepo.actualizarStock(productoId, nuevoStock)

        if (actualizado) {
            inventarioRepo.registrarMovimiento(
                MovimientoInventario(
                    productoId = productoId,
                    tipo = TipoMovimiento.SALIDA,
                    cantidad = cantidad,
                    motivo = motivo
                )
            )
            verificarAlertaCritica(productoId, nuevoStock)
        } else {
            Logger.logError("Error al actualizar la base de datos para disminuir stock de producto $productoId.")
        }

        return actualizado
    }

    fun descontarStockPorVenta(orden: Orden): Boolean {
        var exitoTotal = true
        orden.detalles.forEach { detalle ->
            inventarioRepo.registrarMovimiento(
                MovimientoInventario(
                    productoId = detalle.producto.id,
                    tipo = TipoMovimiento.SALIDA,
                    cantidad = detalle.cantidad,
                    motivo = "Venta confirmada Orden #${orden.id}"
                )
            )
            val stockActual = inventarioRepo.obtenerStock(detalle.producto.id)
            verificarAlertaCritica(detalle.producto.id, stockActual)
        }
        return exitoTotal
    }

    fun ajusteManual(productoId: Int, nuevoStock: Int, motivo: String): Boolean {
        if (nuevoStock < 0) {
            Logger.logError("Ajuste manual rechazado: Stock negativo no permitido para producto $productoId.")
            return false
        }

        val stockActual = consultarStock(productoId)
        if (stockActual == -1) return false

        val actualizado = inventarioRepo.actualizarStock(productoId, nuevoStock)
        if (actualizado) {
            val diferencia = nuevoStock - stockActual
            inventarioRepo.registrarMovimiento(
                MovimientoInventario(
                    productoId = productoId,
                    tipo = TipoMovimiento.AJUSTE,
                    cantidad = diferencia,
                    motivo = motivo
                )
            )
            verificarAlertaCritica(productoId, nuevoStock)
        }
        return actualizado
    }

    fun obtenerProductosCriticos(): List<Producto> {
        return inventarioRepo.obtenerProductosCriticos(LIMITE_STOCK_CRITICO)
    }

    private fun verificarAlertaCritica(productoId: Int, stockActual: Int) {
        if (stockActual in 0..LIMITE_STOCK_CRITICO) {
            println("\n⚠️ [ALERTA DE INVENTARIO] El producto ID $productoId tiene stock crítico: $stockActual unidades.")
        }
    }
}