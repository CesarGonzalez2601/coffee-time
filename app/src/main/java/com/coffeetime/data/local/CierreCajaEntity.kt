package com.coffeetime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.coffeetime.domain.model.CierreCaja

@Entity(tableName = CierreCajaEntity.TABLE)
data class CierreCajaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    @ColumnInfo(name = "total_ingresado") val totalIngresado: Double,
    @ColumnInfo(name = "total_efectivo") val totalEfectivo: Double,
    @ColumnInfo(name = "total_tarjeta") val totalTarjeta: Double,
    @ColumnInfo(name = "total_ordenes") val totalOrdenes: Int,
    @ColumnInfo(name = "monto_inicial") val montoInicial: Double,
    @ColumnInfo(name = "efectivo_esperado") val efectivoEsperado: Double,
    @ColumnInfo(name = "efectivo_reportado") val efectivoReportado: Double,
    val diferencia: Double,
    val observaciones: String,
    @ColumnInfo(name = "cashier_uid") val cashierUid: String,
    @ColumnInfo(name = "cashier_name") val cashierName: String
) {
    fun toDomain(): CierreCaja = CierreCaja(
        id = id,
        fechaTimestamp = timestamp,
        totalIngresado = totalIngresado,
        totalEfectivo = totalEfectivo,
        totalTarjeta = totalTarjeta,
        totalOrdenes = totalOrdenes,
        montoInicialCaja = montoInicial,
        efectivoEsperado = efectivoEsperado,
        efectivoReportado = efectivoReportado,
        diferencia = diferencia,
        observaciones = observaciones,
        cajeroUid = cashierUid,
        cashierName = cashierName
    )

    companion object {
        const val TABLE = "cierres_caja"

        fun fromDomain(cierre: CierreCaja): CierreCajaEntity = CierreCajaEntity(
            id = cierre.id,
            timestamp = cierre.fechaTimestamp,
            totalIngresado = cierre.totalIngresado,
            totalEfectivo = cierre.totalEfectivo,
            totalTarjeta = cierre.totalTarjeta,
            totalOrdenes = cierre.totalOrdenes,
            montoInicial = cierre.montoInicialCaja,
            efectivoEsperado = cierre.efectivoEsperado,
            efectivoReportado = cierre.efectivoReportado,
            diferencia = cierre.diferencia,
            observaciones = cierre.observaciones,
            cashierUid = cierre.cajeroUid,
            cashierName = cierre.cashierName
        )
    }
}
