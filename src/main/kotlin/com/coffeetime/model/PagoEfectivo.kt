package com.coffeetime.model

class PagoEfectivo(private val montoRecibido: Double) : MetodoPago {
    override val tipo = "EFECTIVO"
    var cambio: Double = 0.0
        private set

    override fun procesarPago(totalOrden: Double): Boolean {
        if (montoRecibido < totalOrden) {
            throw IllegalArgumentException(
                "Pago insuficiente. Total: $%.2f, Recibido: $%.2f".format(totalOrden, montoRecibido)
            )
        }
        cambio = montoRecibido - totalOrden
        return true
    }

    override fun obtenerDetalle(): String {
        return "Pago en efectivo. Recibido: $%.2f, Cambio: $%.2f".format(montoRecibido, cambio)
    }
}