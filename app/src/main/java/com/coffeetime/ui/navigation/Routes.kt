package com.coffeetime.ui.navigation

import kotlinx.serialization.Serializable

// Grafo raíz
@Serializable
object Login

@Serializable
object Registro

@Serializable
data class RegistroExito(val userId: Int)

@Serializable
object Main

// Pestañas dentro de Main
@Serializable
object Inicio

@Serializable
object Ordenes

@Serializable
object Pagos

@Serializable
object Inventario

@Serializable
object Mas
