package com.coffeetime.repository

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.CategoriaProducto
import com.coffeetime.model.Producto
import java.sql.ResultSet

class ProductoRepositorySQLite {

    fun agregarProducto(
        nombre: String,
        precio: Double,
        categoria: CategoriaProducto,
        stock: Int
    ): Producto {

        val sql = """
            INSERT INTO productos (nombre, precio, categoria, stock)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, nombre)
                statement.setDouble(2, precio)
                statement.setString(3, categoria.name)
                statement.setInt(4, stock)
                statement.executeUpdate()
            }

            connection.prepareStatement(
                "SELECT last_insert_rowid() AS id"
            ).use { statement ->
                statement.executeQuery().use { result ->

                    if (result.next()) {
                        return Producto(
                            id = result.getInt("id"),
                            nombre = nombre,
                            precio = precio,
                            categoria = categoria,
                            stock = stock
                        )
                    }
                }
            }
        }

        throw IllegalStateException("No se pudo obtener el ID del producto creado.")
    }

    fun obtenerProductos(): List<Producto> {

        val productos = mutableListOf<Producto>()
        val sql = "SELECT * FROM productos ORDER BY id"

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { result ->

                    while (result.next()) {
                        productos.add(mapProducto(result))
                    }
                }
            }
        }

        return productos
    }

    fun buscarPorId(id: Int): Producto? {

        val sql = "SELECT * FROM productos WHERE id = ?"

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, id)

                statement.executeQuery().use { result ->
                    if (result.next()) {
                        return mapProducto(result)
                    }
                }
            }
        }

        return null
    }

    fun actualizarProducto(
        id: Int,
        nuevoPrecio: Double,
        nuevoStock: Int
    ): Boolean {

        val sql = """
            UPDATE productos
            SET precio = ?, stock = ?
            WHERE id = ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setDouble(1, nuevoPrecio)
                statement.setInt(2, nuevoStock)
                statement.setInt(3, id)

                return statement.executeUpdate() > 0
            }
        }
    }

    fun eliminarProducto(id: Int): Boolean {

        val sql = "DELETE FROM productos WHERE id = ?"

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, id)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun reducirStock(
        productoId: Int,
        cantidad: Int
    ): Boolean {

        if (cantidad <= 0) {
            return false
        }

        val sql = """
            UPDATE productos
            SET stock = stock - ?
            WHERE id = ? AND stock >= ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, cantidad)
                statement.setInt(2, productoId)
                statement.setInt(3, cantidad)

                return statement.executeUpdate() > 0
            }
        }
    }

    fun incrementarStock(
        productoId: Int,
        cantidad: Int
    ): Boolean {

        if (cantidad <= 0) {
            return false
        }

        val sql = """
            UPDATE productos
            SET stock = stock + ?
            WHERE id = ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, cantidad)
                statement.setInt(2, productoId)

                return statement.executeUpdate() > 0
            }
        }
    }

    private fun mapProducto(
        result: ResultSet
    ): Producto {
        return Producto(
            id = result.getInt("id"),
            nombre = result.getString("nombre"),
            precio = result.getDouble("precio"),
            categoria = CategoriaProducto.valueOf(
                result.getString("categoria")
            ),
            stock = result.getInt("stock")
        )
    }
}
