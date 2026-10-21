package com.coffeetime.domain.model

enum class TipoMovimiento(val displayName: String) {
    ENTRADA("Entrada de stock"),
    SALIDA_VENTA("Venta"),
    AJUSTE_MANUAL("Ajuste manual")
}
