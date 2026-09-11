package com.coffeetime.service

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.CategoriaProducto
import com.coffeetime.model.DetalleOrden
import com.coffeetime.model.EstadoOrden
import com.coffeetime.model.Orden
import com.coffeetime.model.Producto
import com.coffeetime.repository.ProductoRepositorySQLite
import java.sql.Connection
import java.sql.ResultSet

class OrdenService(
    private val productoRepository: ProductoRepositorySQLite
) {

    private var ordenActual = crearCarrito()

    val carritoActual: Orden
        get() = ordenActual

    val subtotal: Double
        get() = carritoActual.subtotal

    val impuesto: Double
        get() = carritoActual.impuesto

    val total: Double
        get() = carritoActual.total

    fun agregarProductoAlCarrito(
        producto: Producto,
        cantidad: Int
    ): Boolean {

        if (cantidad <= 0 ||
            !productoRepository.reducirStock(producto.id, cantidad)
        ) {
            return false
        }

        val detalleExistente = carritoActual.detalles
            .find { it.producto.id == producto.id }

        if (detalleExistente != null) {
            detalleExistente.cantidad += cantidad
        } else {
            carritoActual.detalles.add(
                DetalleOrden(
                    producto = producto,
                    cantidad = cantidad
                )
            )
        }

        return true
    }

    fun modificarCantidadEnCarrito(
        productoId: Int,
        nuevaCantidad: Int
    ): Boolean {

        if (nuevaCantidad <= 0) {
            return false
        }

        val detalle = carritoActual.detalles
            .find { it.producto.id == productoId }
            ?: return false

        val diferencia = nuevaCantidad - detalle.cantidad

        if (diferencia > 0 &&
            !productoRepository.reducirStock(productoId, diferencia)
        ) {
            return false
        }

        if (diferencia < 0 &&
            !productoRepository.incrementarStock(productoId, -diferencia)
        ) {
            return false
        }

        detalle.cantidad = nuevaCantidad
        return true
    }

    fun eliminarDelCarrito(
        productoId: Int
    ): Boolean {

        val detalle = carritoActual.detalles
            .find { it.producto.id == productoId }
            ?: return false

        if (!productoRepository.incrementarStock(
                productoId,
                detalle.cantidad
            )
        ) {
            return false
        }

        carritoActual.detalles.remove(detalle)
        return true
    }

    fun confirmarOrden(
        orden: Orden = carritoActual
    ): Orden? {

        if (orden.detalles.isEmpty() ||
            orden.estado != EstadoOrden.ABIERTA
        ) {
            return null
        }

        val connection = DatabaseManager.getConnection()

        return try {
            connection.autoCommit = false

            val ordenId = insertarOrden(
                connection = connection,
                orden = orden,
                estado = EstadoOrden.CONFIRMADA
            )

            insertarDetalles(
                connection = connection,
                ordenId = ordenId,
                orden = orden
            )

            connection.commit()
            orden.estado = EstadoOrden.CONFIRMADA

            val ordenConfirmada = Orden(
                id = ordenId,
                detalles = orden.detalles.map { it.copy() }.toMutableList(),
                estado = EstadoOrden.CONFIRMADA
            )

            if (orden === carritoActual) {
                limpiarCarrito()
            }

            ordenConfirmada
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        } finally {
            connection.close()
        }
    }

    fun procesarPagoYConfirmarOrden(
        orden: Orden
    ): Boolean {

        if (orden.id <= 0 ||
            orden.estado != EstadoOrden.CONFIRMADA
        ) {
            return false
        }

        val sql = """
            UPDATE ordenes
            SET estado = ?
            WHERE id = ? AND estado = ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, EstadoOrden.PAGADA.name)
                statement.setInt(2, orden.id)
                statement.setString(3, EstadoOrden.CONFIRMADA.name)

                if (statement.executeUpdate() == 0) {
                    return false
                }
            }
        }

        orden.estado = EstadoOrden.PAGADA
        return true
    }

    fun cancelarOrden(
        orden: Orden
    ): Boolean {

        if (orden.id <= 0 ||
            orden.estado != EstadoOrden.CONFIRMADA
        ) {
            return false
        }

        val connection = DatabaseManager.getConnection()

        return try {
            connection.autoCommit = false

            val updateOrderSql = """
                UPDATE ordenes
                SET estado = ?
                WHERE id = ? AND estado = ?
            """.trimIndent()

            connection.prepareStatement(updateOrderSql).use { statement ->
                statement.setString(1, EstadoOrden.CANCELADA.name)
                statement.setInt(2, orden.id)
                statement.setString(3, EstadoOrden.CONFIRMADA.name)

                if (statement.executeUpdate() == 0) {
                    connection.rollback()
                    return false
                }
            }

            val updateStockSql = """
                UPDATE productos
                SET stock = stock + ?
                WHERE id = ?
            """.trimIndent()

            connection.prepareStatement(updateStockSql).use { statement ->
                orden.detalles.forEach { detalle ->
                    statement.setInt(1, detalle.cantidad)
                    statement.setInt(2, detalle.producto.id)
                    statement.addBatch()
                }
                statement.executeBatch()
            }

            connection.commit()
            orden.estado = EstadoOrden.CANCELADA
            true
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        } finally {
            connection.close()
        }
    }

    fun obtenerOrdenesConfirmadas(): List<Orden> {
        return obtenerOrdenesPorEstado(EstadoOrden.CONFIRMADA)
    }

    fun obtenerHistorialOrdenes(): List<Orden> {
        return obtenerOrdenesPorEstado(EstadoOrden.PAGADA)
    }

    private fun obtenerOrdenesPorEstado(
        estado: EstadoOrden
    ): List<Orden> {

        val ordenes = mutableListOf<Orden>()
        val sql = """
            SELECT id, estado
            FROM ordenes
            WHERE estado = ?
            ORDER BY id
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, estado.name)

                statement.executeQuery().use { result ->
                    while (result.next()) {
                        ordenes.add(
                            cargarOrden(
                                connection = connection,
                                id = result.getInt("id"),
                                estado = estado
                            )
                        )
                    }
                }
            }
        }

        return ordenes
    }

    private fun insertarOrden(
        connection: Connection,
        orden: Orden,
        estado: EstadoOrden
    ): Int {

        val sql = """
            INSERT INTO ordenes (estado, total)
            VALUES (?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, estado.name)
            statement.setDouble(2, orden.total)
            statement.executeUpdate()
        }

        connection.prepareStatement(
            "SELECT last_insert_rowid() AS id"
        ).use { statement ->
            statement.executeQuery().use { result ->
                if (result.next()) {
                    return result.getInt("id")
                }
            }
        }

        throw IllegalStateException("No se pudo obtener el ID de la orden.")
    }

    private fun insertarDetalles(
        connection: Connection,
        ordenId: Int,
        orden: Orden
    ) {

        val sql = """
            INSERT INTO detalle_ordenes (
                orden_id,
                producto_id,
                cantidad,
                subtotal
            )
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            orden.detalles.forEach { detalle ->
                statement.setInt(1, ordenId)
                statement.setInt(2, detalle.producto.id)
                statement.setInt(3, detalle.cantidad)
                statement.setDouble(4, detalle.subtotalItem)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun cargarOrden(
        connection: Connection,
        id: Int,
        estado: EstadoOrden
    ): Orden {

        val detalles = mutableListOf<DetalleOrden>()
        val sql = """
            SELECT
                p.id,
                p.nombre,
                p.precio,
                p.categoria,
                p.stock,
                d.cantidad
            FROM detalle_ordenes d
            INNER JOIN productos p ON p.id = d.producto_id
            WHERE d.orden_id = ?
            ORDER BY d.id
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, id)

            statement.executeQuery().use { result ->
                while (result.next()) {
                    detalles.add(
                        DetalleOrden(
                            producto = mapProducto(result),
                            cantidad = result.getInt("cantidad")
                        )
                    )
                }
            }
        }

        return Orden(
            id = id,
            detalles = detalles,
            estado = estado
        )
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

    private fun limpiarCarrito() {
        ordenActual = crearCarrito()
    }

    private fun crearCarrito(): Orden {
        return Orden(
            id = 0,
            detalles = mutableListOf(),
            estado = EstadoOrden.ABIERTA
        )
    }
}
