package com.coffeetime.domain.service

import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.model.Pago
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.model.Reporte
import com.coffeetime.domain.model.ProductoVendido
import java.time.LocalDateTime
import java.time.ZoneId

class ReporteService {

    fun generarReporte(
        ordenes: List<Orden>,
        pagos: List<Pago>,
        productos: List<Producto>,
        fechaInicio: LocalDateTime,
        fechaFin: LocalDateTime
    ): Reporte {

        val inicioMillis = fechaInicio.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val finMillis = fechaFin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pagosDelPeriodo = pagos.filter { it.fechaTimestamp in inicioMillis..finMillis }
        val idsOrdenesCobradas = pagosDelPeriodo.map { it.ordenId }.toSet()
        val ordenesDelPeriodo = ordenes.filter { it.id in idsOrdenesCobradas }

        val ingresosTotales = ordenesDelPeriodo.sumOf { it.total }
        val ordenesCerradas = ordenesDelPeriodo.size
        val ticketPromedio = if (ordenesCerradas > 0) ingresosTotales / ordenesCerradas else 0.0

        val detallesVendidos = ordenesDelPeriodo.flatMap { it.items }

        val top5Productos = detallesVendidos
            .groupBy { it.nombreProducto }
            .map { (nombre, detalles) ->
                ProductoVendido(
                    nombre = nombre,
                    cantidad = detalles.sumOf { it.cantidad },
                    total = detalles.sumOf { it.subtotalLinea }
                )
            }
            .sortedByDescending { it.cantidad }
            .take(5)

        val ventasPorCategoria = detallesVendidos
            .groupBy { detalle ->
                val producto = productos.find { it.id == detalle.productoId }
                producto?.categoria?.name ?: "OTROS"
            }
            .mapValues { (_, detalles) ->
                detalles.sumOf { it.subtotalLinea }
            }

        val ventasPorMetodoPago = pagosDelPeriodo
            .groupBy { it.metodoPago.displayName }
            .mapValues { (_, pagosGrupo) ->
                pagosGrupo.sumOf { it.montoRecibido - it.cambio }
            }

        return Reporte(
            ingresosTotales = ingresosTotales,
            ordenesCerradas = ordenesCerradas,
            ticketPromedio = ticketPromedio,
            top5Productos = top5Productos,
            ventasPorCategoria = ventasPorCategoria,
            ventasPorMetodoPago = ventasPorMetodoPago
        )
    }
}