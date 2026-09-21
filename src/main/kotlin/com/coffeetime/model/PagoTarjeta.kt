package com.coffeetime.model

class PagoTarjeta(
    private val numeroTarjeta: String,
    private val titular: String
) : MetodoPago {
    override val tipo = "TARJETA"

    override fun procesarPago(totalOrden: Double): Boolean {
        return numeroTarjeta.length == 16 && titular.isNotBlank()
    }

    override fun obtenerDetalle(): String {
        val ultimosDigitos = if (numeroTarjeta.length >= 4) numeroTarjeta.takeLast(4) else "****"
        return "Tarjeta terminada en $ultimosDigitos a nombre de $titular"
    }
}