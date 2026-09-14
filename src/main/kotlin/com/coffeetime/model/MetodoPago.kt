package com.coffeetime.model

interface MetodoPago {
    val tipo: String
    fun procesarPago(totalOrden: Double): Boolean
    fun obtenerDetalle(): String
}