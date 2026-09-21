package com.coffeetime.service

import com.coffeetime.model.CierreCaja
import com.coffeetime.model.VentaPorMetodoPago
import com.coffeetime.repository.CierreCajaRepositorySQLite
import com.coffeetime.repository.ReporteRepositorySQLite
import com.coffeetime.util.Logger
import java.time.LocalDate

class CierreCajaService(
    private val cierreRepository: CierreCajaRepositorySQLite,
    private val reporteRepository: ReporteRepositorySQLite
) {

    companion object {
        const val METODO_EFECTIVO = "EFECTIVO"
        const val METODO_TARJETA = "TARJETA"
    }

    fun fechaDeHoy(): String {
        return LocalDate.now().toString()
    }

    /**
     * Arma el arqueo de la jornada sin guardarlo, con el efectivo contado todavia en cero.
     */
    fun previsualizarCierre(
        fecha: String,
        usuarioId: Int
    ): CierreCaja {

        return construirCierre(
            fecha = fecha,
            usuarioId = usuarioId,
            efectivoContado = 0.0
        )
    }

    /**
     * Devuelve null si ya hay un cierre para esa fecha o si el monto contado es invalido.
     */
    fun registrarCierre(
        fecha: String,
        usuarioId: Int,
        efectivoContado: Double
    ): CierreCaja? {

        if (efectivoContado < 0) {
            Logger.logError(
                "Cierre de caja rechazado: efectivo contado negativo ($efectivoContado)."
            )
            return null
        }

        if (cierreRepository.buscarPorFecha(fecha) != null) {
            Logger.logError(
                "Cierre de caja rechazado: la jornada $fecha ya fue cerrada."
            )
            return null
        }

        val cierre = construirCierre(
            fecha = fecha,
            usuarioId = usuarioId,
            efectivoContado = efectivoContado
        )

        val guardado = cierreRepository.guardar(cierre)

        if (guardado != null) {
            Logger.logInfo(
                "Cierre de caja $fecha: esperado $%.2f, contado $%.2f, estado ${guardado.estado}."
                    .format(guardado.efectivoEsperado, guardado.efectivoContado)
            )
        }

        return guardado
    }

    fun buscarPorFecha(fecha: String): CierreCaja? {
        return cierreRepository.buscarPorFecha(fecha)
    }

    fun obtenerCierres(): List<CierreCaja> {
        return cierreRepository.obtenerTodos()
    }

    private fun construirCierre(
        fecha: String,
        usuarioId: Int,
        efectivoContado: Double
    ): CierreCaja {

        val resumen = reporteRepository.obtenerResumen(
            desde = fecha,
            hasta = fecha
        )

        val porMetodo = reporteRepository.obtenerPorMetodoPago(
            desde = fecha,
            hasta = fecha
        )

        return CierreCaja(
            fecha = fecha,
            usuarioId = usuarioId,
            cantidadOrdenes = resumen.ordenesCerradas,
            totalVentas = resumen.ingresos,
            totalEfectivo = totalDe(porMetodo, METODO_EFECTIVO),
            totalTarjeta = totalDe(porMetodo, METODO_TARJETA),
            efectivoContado = efectivoContado
        )
    }

    private fun totalDe(
        ventas: List<VentaPorMetodoPago>,
        metodoPago: String
    ): Double {
        return ventas
            .filter { it.metodoPago == metodoPago }
            .sumOf { it.total }
    }
}
