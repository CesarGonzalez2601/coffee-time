package com.coffeetime.data.firebase

import android.util.Log
import com.coffeetime.domain.model.CierreCaja
import com.coffeetime.domain.repository.CierreCajaRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CierreCajaRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CierreCajaRepository {

    private val collection
        get() = firestore.collection(COLLECTION)

    override suspend fun realizarCierre(cierre: CierreCaja): Long {
        val nextId = getNextId()
        val data = mapOf(
            FIELD_ID to nextId,
            FIELD_TIMESTAMP to cierre.fechaTimestamp,
            FIELD_TOTAL_INGRESADO to cierre.totalIngresado,
            FIELD_TOTAL_EFECTIVO to cierre.totalEfectivo,
            FIELD_TOTAL_TARJETA to cierre.totalTarjeta,
            FIELD_TOTAL_ORDENES to cierre.totalOrdenes,
            FIELD_MONTO_INICIAL to cierre.montoInicialCaja,
            FIELD_EFECTIVO_ESPERADO to cierre.efectivoEsperado,
            FIELD_EFECTIVO_REPORTADO to cierre.efectivoReportado,
            FIELD_DIFERENCIA to cierre.diferencia,
            FIELD_OBSERVACIONES to cierre.observaciones,
            FIELD_CASHIER_UID to cierre.cajeroUid,
            FIELD_CASHIER_NAME to cierre.cashierName
        )

        collection.document(nextId.toString()).set(data).await()
        return nextId
    }

    override fun getAllCierresFlow(): Flow<List<CierreCaja>> = callbackFlow {
        val listener = collection.orderBy(FIELD_TIMESTAMP)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getAllCierresFlow snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toCierre() }.orEmpty().reversed()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun getNextId(): Long {
        val counterRef = firestore.collection("contadores").document(COLLECTION)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(counterRef)
            val currentId = snapshot.getLong("ultimoId") ?: 0L
            val nextId = currentId + 1
            transaction.set(counterRef, mapOf("ultimoId" to nextId))
            nextId
        }.await()
    }

    private fun DocumentSnapshot.toCierre(): CierreCaja? {
        if (!exists()) return null
        val id = getLong(FIELD_ID)?.toInt() ?: id.toIntOrNull() ?: return null

        return CierreCaja(
            id = id,
            fechaTimestamp = getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
            totalIngresado = getDouble(FIELD_TOTAL_INGRESADO) ?: 0.0,
            totalEfectivo = getDouble(FIELD_TOTAL_EFECTIVO) ?: 0.0,
            totalTarjeta = getDouble(FIELD_TOTAL_TARJETA) ?: 0.0,
            totalOrdenes = getLong(FIELD_TOTAL_ORDENES)?.toInt() ?: 0,
            montoInicialCaja = getDouble(FIELD_MONTO_INICIAL) ?: 0.0,
            efectivoEsperado = getDouble(FIELD_EFECTIVO_ESPERADO) ?: 0.0,
            efectivoReportado = getDouble(FIELD_EFECTIVO_REPORTADO) ?: 0.0,
            diferencia = getDouble(FIELD_DIFERENCIA) ?: 0.0,
            observaciones = getString(FIELD_OBSERVACIONES).orEmpty(),
            cajeroUid = getString(FIELD_CASHIER_UID).orEmpty(),
            cashierName = getString(FIELD_CASHIER_NAME).orEmpty()
        )
    }

    private companion object {
        const val TAG = "CierreCajaRepositoryFS"
        const val COLLECTION = "cierres_caja"
        const val FIELD_ID = "id"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_TOTAL_INGRESADO = "totalIngresado"
        const val FIELD_TOTAL_EFECTIVO = "totalEfectivo"
        const val FIELD_TOTAL_TARJETA = "totalTarjeta"
        const val FIELD_TOTAL_ORDENES = "totalOrdenes"
        const val FIELD_MONTO_INICIAL = "montoInicial"
        const val FIELD_EFECTIVO_ESPERADO = "efectivoEsperado"
        const val FIELD_EFECTIVO_REPORTADO = "efectivoReportado"
        const val FIELD_DIFERENCIA = "diferencia"
        const val FIELD_OBSERVACIONES = "observaciones"
        const val FIELD_CASHIER_UID = "cajeroUid"
        const val FIELD_CASHIER_NAME = "cashierName"
    }
}
