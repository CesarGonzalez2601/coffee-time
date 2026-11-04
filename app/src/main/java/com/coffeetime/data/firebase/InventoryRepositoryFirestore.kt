package com.coffeetime.data.firebase

import android.util.Log
import com.coffeetime.domain.model.MovimientoInventario
import com.coffeetime.domain.model.TipoMovimiento
import com.coffeetime.domain.repository.InventoryRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InventoryRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : InventoryRepository {

    private val collection
        get() = firestore.collection(COLLECTION)

    override suspend fun registerMovement(movimiento: MovimientoInventario) {
        val nextId = getNextId().toInt()
        val data = mapOf(
            FIELD_ID to nextId,
            FIELD_PRODUCT_ID to movimiento.productoId,
            FIELD_PRODUCT_NAME to movimiento.nombreProducto,
            FIELD_TYPE to movimiento.tipo.name,
            FIELD_QUANTITY to movimiento.cantidad,
            FIELD_PREVIOUS_STOCK to movimiento.stockAnterior,
            FIELD_NEW_STOCK to movimiento.stockNuevo,
            FIELD_REASON to movimiento.motivo,
            FIELD_TIMESTAMP to movimiento.fechaTimestamp,
            FIELD_USUARIO_UID to movimiento.usuarioUid
        )

        collection.document(nextId.toString()).set(data).await()
    }

    override fun getAllMovementsFlow(): Flow<List<MovimientoInventario>> = callbackFlow {
        val listener = collection.orderBy(FIELD_TIMESTAMP)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getAllMovementsFlow snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toMovimiento() }.orEmpty().reversed()
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

    private fun DocumentSnapshot.toMovimiento(): MovimientoInventario? {
        if (!exists()) return null
        val id = getLong(FIELD_ID)?.toInt() ?: id.toIntOrNull() ?: return null

        return MovimientoInventario(
            id = id,
            productoId = getLong(FIELD_PRODUCT_ID)?.toInt() ?: 0,
            nombreProducto = getString(FIELD_PRODUCT_NAME).orEmpty(),
            tipo = runCatching { TipoMovimiento.valueOf(getString(FIELD_TYPE).orEmpty()) }.getOrDefault(TipoMovimiento.AJUSTE_MANUAL),
            cantidad = getLong(FIELD_QUANTITY)?.toInt() ?: 0,
            stockAnterior = getLong(FIELD_PREVIOUS_STOCK)?.toInt() ?: 0,
            stockNuevo = getLong(FIELD_NEW_STOCK)?.toInt() ?: 0,
            motivo = getString(FIELD_REASON).orEmpty(),
            fechaTimestamp = getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
            usuarioUid = getString(FIELD_USUARIO_UID).orEmpty()
        )
    }

    private companion object {
        const val TAG = "InventoryRepositoryFS"
        const val COLLECTION = "movimientos_inventario"
        const val FIELD_ID = "id"
        const val FIELD_PRODUCT_ID = "productoId"
        const val FIELD_PRODUCT_NAME = "nombreProducto"
        const val FIELD_TYPE = "tipo"
        const val FIELD_QUANTITY = "cantidad"
        const val FIELD_PREVIOUS_STOCK = "stockAnterior"
        const val FIELD_NEW_STOCK = "stockNuevo"
        const val FIELD_REASON = "motivo"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_USUARIO_UID = "usuarioUid"
    }
}
