package com.coffeetime.model

import kotlin.math.abs

enum class EstadoCierre {
    CUADRADA,
    FALTANTE,
    SOBRANTE
}

data class CierreCaja(
    val id: Int = 0,
    val fecha: String,
    val usuarioId: Int,
    val cantidadOrdenes: Int,
    val totalVentas: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val efectivoContado: Double,
    val fechaHora: String? = null
) {

    companion object {
        // Tolerancia de un centavo: los totales vienen de aritmetica con Double.
        private const val TOLERANCIA = 0.01
    }

    val efectivoEsperado: Double
        get() = totalEfectivo

    val diferencia: Double
        get() = efectivoContado - efectivoEsperado

    val estado: EstadoCierre
        get() = when {
            abs(diferencia) < TOLERANCIA -> EstadoCierre.CUADRADA
            diferencia < 0 -> EstadoCierre.FALTANTE
            else -> EstadoCierre.SOBRANTE
        }
}
