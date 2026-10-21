package com.coffeetime.domain.model

data class CierreCaja(
    val id: Int = 0,
    val fechaTimestamp: Long = System.currentTimeMillis(),
    val totalIngresado: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val totalOrdenes: Int,
    val montoInicialCaja: Double,
    val efectivoEsperado: Double,
    val efectivoReportado: Double,
    val diferencia: Double,
    val observaciones: String = "",
    val cajeroId: Int,
    val cashierName: String
)
