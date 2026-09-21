package com.coffeetime.repository

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.CierreCaja
import com.coffeetime.util.Logger
import java.sql.ResultSet
import java.sql.SQLException

class CierreCajaRepositorySQLite {

    /**
     * Devuelve null si ya existe un cierre para esa fecha (cierres_caja.fecha es UNIQUE)
     * o si el INSERT falla.
     */
    fun guardar(cierre: CierreCaja): CierreCaja? {

        val sql = """
            INSERT INTO cierres_caja (
                fecha,
                usuario_id,
                cantidad_ordenes,
                total_ventas,
                total_efectivo,
                total_tarjeta,
                efectivo_contado
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return try {
            DatabaseManager.getConnection().use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setString(1, cierre.fecha)
                    statement.setInt(2, cierre.usuarioId)
                    statement.setInt(3, cierre.cantidadOrdenes)
                    statement.setDouble(4, cierre.totalVentas)
                    statement.setDouble(5, cierre.totalEfectivo)
                    statement.setDouble(6, cierre.totalTarjeta)
                    statement.setDouble(7, cierre.efectivoContado)
                    statement.executeUpdate()
                }
            }

            buscarPorFecha(cierre.fecha)
        } catch (exception: SQLException) {
            Logger.logError(
                "No se pudo guardar el cierre de caja del ${cierre.fecha}: ${exception.message}"
            )
            null
        }
    }

    fun buscarPorFecha(fecha: String): CierreCaja? {

        val sql = "SELECT * FROM cierres_caja WHERE fecha = ?"

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, fecha)

                statement.executeQuery().use { result ->
                    if (result.next()) {
                        return mapCierre(result)
                    }
                }
            }
        }

        return null
    }

    fun obtenerTodos(): List<CierreCaja> {

        val cierres = mutableListOf<CierreCaja>()
        val sql = "SELECT * FROM cierres_caja ORDER BY fecha DESC"

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { result ->
                    while (result.next()) {
                        cierres.add(mapCierre(result))
                    }
                }
            }
        }

        return cierres
    }

    private fun mapCierre(result: ResultSet): CierreCaja {
        return CierreCaja(
            id = result.getInt("id"),
            fecha = result.getString("fecha"),
            usuarioId = result.getInt("usuario_id"),
            cantidadOrdenes = result.getInt("cantidad_ordenes"),
            totalVentas = result.getDouble("total_ventas"),
            totalEfectivo = result.getDouble("total_efectivo"),
            totalTarjeta = result.getDouble("total_tarjeta"),
            efectivoContado = result.getDouble("efectivo_contado"),
            fechaHora = result.getString("fecha_hora")
        )
    }
}
