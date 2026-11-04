package com.coffeetime.data.firebase

import android.util.Log
import com.coffeetime.domain.model.DetalleOrden
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.repository.OrderRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : OrderRepository {

    private val collection
        get() = firestore.collection(COLLECTION)

    override suspend fun createOrder(orden: Orden): Int {
        val nextId = getNextId().toInt()
        val docRef = collection.document(nextId.toString())

        val itemsData = orden.items.map { item ->
            mapOf(
                "productoId" to item.productoId,
                "nombreProducto" to item.nombreProducto,
                "precioUnitario" to item.precioUnitario,
                "cantidad" to item.cantidad
            )
        }

        val data = mapOf(
            FIELD_ID to nextId,
            FIELD_TIMESTAMP to orden.fechaTimestamp,
            FIELD_ITEMS to itemsData,
            FIELD_SUBTOTAL to orden.subtotal,
            FIELD_IVA to orden.iva,
            FIELD_TOTAL to orden.total,
            FIELD_STATUS to orden.estado.name,
            FIELD_CASHIER_UID to orden.cajeroUid
        )

        docRef.set(data).await()
        return nextId
    }

    override suspend fun getOrderById(id: Int): Orden? {
        return try {
            val doc = collection.document(id.toString()).get().await()
            doc.toOrden()
        } catch (e: Exception) {
            Log.w(TAG, "getOrderById error", e)
            null
        }
    }

    override fun getAllOrdersFlow(): Flow<List<Orden>> = callbackFlow {
        val listener = collection.orderBy(FIELD_TIMESTAMP)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getAllOrdersFlow snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toOrden() }.orEmpty().reversed()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun getOrdersByStatusFlow(status: EstadoOrden): Flow<List<Orden>> = callbackFlow {
        val listener = collection.whereEqualTo(FIELD_STATUS, status.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getOrdersByStatusFlow snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toOrden() }.orEmpty()
                    .sortedByDescending { it.fechaTimestamp }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateOrderStatus(id: Int, status: EstadoOrden) {
        collection.document(id.toString()).update(FIELD_STATUS, status.name).await()
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

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toOrden(): Orden? {
        if (!exists()) return null
        val id = getLong(FIELD_ID)?.toInt() ?: id.toIntOrNull() ?: return null
        val itemsList = (get(FIELD_ITEMS) as? List<Map<String, Any>>).orEmpty().map { map ->
            DetalleOrden(
                ordenId = id,
                productoId = (map["productoId"] as? Long)?.toInt() ?: (map["productoId"] as? Int) ?: 0,
                nombreProducto = map["nombreProducto"] as? String ?: "",
                precioUnitario = (map["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                cantidad = (map["cantidad"] as? Long)?.toInt() ?: (map["cantidad"] as? Int) ?: 1
            )
        }

        return Orden(
            id = id,
            fechaTimestamp = getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
            items = itemsList,
            subtotal = getDouble(FIELD_SUBTOTAL) ?: 0.0,
            iva = getDouble(FIELD_IVA) ?: 0.0,
            total = getDouble(FIELD_TOTAL) ?: 0.0,
            estado = runCatching { EstadoOrden.valueOf(getString(FIELD_STATUS).orEmpty()) }.getOrDefault(EstadoOrden.PENDIENTE),
            cajeroUid = getString(FIELD_CASHIER_UID).orEmpty()
        )
    }

    private companion object {
        const val TAG = "OrderRepositoryFS"
        const val COLLECTION = "ordenes"
        const val FIELD_ID = "id"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_ITEMS = "items"
        const val FIELD_SUBTOTAL = "subtotal"
        const val FIELD_IVA = "iva"
        const val FIELD_TOTAL = "total"
        const val FIELD_STATUS = "status"
        const val FIELD_CASHIER_UID = "cajeroUid"
    }
}
