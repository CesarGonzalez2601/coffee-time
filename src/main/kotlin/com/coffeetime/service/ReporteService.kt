package com.coffeetime.service

import com.coffeetime.model.Reporte
import com.coffeetime.repository.ReporteRepositorySQLite
import com.coffeetime.util.Logger
import java.time.LocalDate

class ReporteService(
    private val reporteRepository: ReporteRepositorySQLite
) {

    companion object {
        const val TOP_PRODUCTOS = 5
    }

    fun generarReporteDelDia(): Reporte {

        val hoy = LocalDate.now().toString()

        return generarReporte(
            desde = hoy,
            hasta = hoy
        )
    }

    fun generarReporte(
        desde: String,
        hasta: String
    ): Reporte {

        if (desde > hasta) {
            Logger.logError(
                "Reporte rechazado: rango invalido ($desde a $hasta)."
            )
            return reporteVacio(
                desde = desde,
                hasta = hasta
            )
        }

        val resumen = reporteRepository.obtenerResumen(
            desde = desde,
            hasta = hasta
        )

        return Reporte(
            desde = desde,
            hasta = hasta,
            ingresosTotales = resumen.ingresos,
            ordenesCerradas = resumen.ordenesCerradas,
            productosMasVendidos = reporteRepository.obtenerProductosMasVendidos(
                desde = desde,
                hasta = hasta,
                limite = TOP_PRODUCTOS
            ),
            ventasPorCategoria = reporteRepository.obtenerVentasPorCategoria(
                desde = desde,
                hasta = hasta
            ),
            ventasPorMetodoPago = reporteRepository.obtenerPorMetodoPago(
                desde = desde,
                hasta = hasta
            )
        )
    }

    private fun reporteVacio(
        desde: String,
        hasta: String
    ): Reporte {
        return Reporte(
            desde = desde,
            hasta = hasta,
            ingresosTotales = 0.0,
            ordenesCerradas = 0,
            productosMasVendidos = emptyList(),
            ventasPorCategoria = emptyList(),
            ventasPorMetodoPago = emptyList()
        )
    }
}
