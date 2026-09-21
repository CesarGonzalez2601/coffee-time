package com.coffeetime.repository

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.MovimientoInventario
import com.coffeetime.model.TipoMovimiento
import com.coffeetime.model.Producto
import com.coffeetime.model.CategoriaProducto
import java.sql.ResultSet

class InventarioRepositorySQLite {

    fun registrarMovimiento(movimiento: MovimientoInventario): Boolean {
        val sql = """
            INSERT INTO movimientos_inventario (producto_id, tipo, cantidad, motivo)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        return try {
            DatabaseManager.getConnection().use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setInt(1, movimiento.productoId)
                    statement.setString(2, movimiento.tipo.name)
                    statement.setInt(3, movimiento.cantidad)
                    statement.setString(4, movimiento.motivo)
                    statement.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    fun obtenerStock(productoId: Int): Int {
        val sql = "SELECT stock FROM productos WHERE id = ?"
        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, productoId)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        return result.getInt("stock")
                    }
                }
            }
        }
        return -1
    }

    fun actualizarStock(productoId: Int, nuevoStock: Int): Boolean {
        if (nuevoStock < 0) return false

        val sql = "UPDATE productos SET stock = ? WHERE id = ?"
        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, nuevoStock)
                statement.setInt(2, productoId)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun obtenerProductosCriticos(limite: Int = 5): List<Producto> {
        val lista = mutableListOf<Producto>()
        val sql = "SELECT * FROM productos WHERE stock <= ? ORDER BY stock ASC"

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, limite)
                statement.executeQuery().use { result ->
                    while (result.next()) {
                        lista.add(mapProducto(result))
                    }
                }
            }
        }
        return lista
    }

    private fun mapProducto(result: ResultSet): Producto {
        return Producto(
            id = result.getInt("id"),
            nombre = result.getString("nombre"),
            precio = result.getDouble("precio"),
            categoria = CategoriaProducto.valueOf(result.getString("categoria")),
            stock = result.getInt("stock")
        )
    }
}