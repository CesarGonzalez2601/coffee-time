package com.coffeetime.data.firebase

import android.util.Log
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.model.Pago
import com.coffeetime.domain.repository.PaymentRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

class PaymentRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PaymentRepository {

    private val collection
        get() = firestore.collection(COLLECTION)

    override suspend fun processPayment(pago: Pago): String {
        val nextId = getNextId().toInt()
        val correlative = pago.comprobanteCorrelativo.ifBlank {
            String.format(Locale.US, "TICKET-%04d", nextId)
        }

        val data = mapOf(
            FIELD_ID to nextId,
            FIELD_ORDER_ID to pago.ordenId,
            FIELD_PAYMENT_METHOD to pago.metodoPago.name,
            FIELD_AMOUNT_RECEIVED to pago.montoRecibido,
            FIELD_CHANGE to pago.cambio,
            FIELD_CORRELATIVE to correlative,
            FIELD_TIMESTAMP to pago.fechaTimestamp,
            FIELD_CASHIER_UID to pago.cajeroUid
        )

        collection.document(nextId.toString()).set(data).await()
        return correlative
    }

    override fun getAllPaymentsFlow(): Flow<List<Pago>> = callbackFlow {
        val listener = collection.orderBy(FIELD_TIMESTAMP)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getAllPaymentsFlow snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toPago() }.orEmpty().reversed()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPaymentForOrder(orderId: Int): Pago? {
        return try {
            val snapshot = collection.whereEqualTo(FIELD_ORDER_ID, orderId).limit(1).get().await()
            snapshot.documents.firstOrNull()?.toPago()
        } catch (e: Exception) {
            Log.w(TAG, "getPaymentForOrder error", e)
            null
        }
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

    private fun DocumentSnapshot.toPago(): Pago? {
        if (!exists()) return null
        val id = getLong(FIELD_ID)?.toInt() ?: id.toIntOrNull() ?: return null
        val orderId = getLong(FIELD_ORDER_ID)?.toInt() ?: 0

        return Pago(
            id = id,
            ordenId = orderId,
            metodoPago = runCatching { MetodoPago.valueOf(getString(FIELD_PAYMENT_METHOD).orEmpty()) }.getOrDefault(MetodoPago.EFECTIVO),
            montoRecibido = getDouble(FIELD_AMOUNT_RECEIVED) ?: 0.0,
            cambio = getDouble(FIELD_CHANGE) ?: 0.0,
            comprobanteCorrelativo = getString(FIELD_CORRELATIVE).orEmpty(),
            fechaTimestamp = getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
            cajeroUid = getString(FIELD_CASHIER_UID).orEmpty()
        )
    }

    private companion object {
        const val TAG = "PaymentRepositoryFS"
        const val COLLECTION = "pagos"
        const val FIELD_ID = "id"
        const val FIELD_ORDER_ID = "ordenId"
        const val FIELD_PAYMENT_METHOD = "metodoPago"
        const val FIELD_AMOUNT_RECEIVED = "montoRecibido"
        const val FIELD_CHANGE = "cambio"
        const val FIELD_CORRELATIVE = "comprobanteCorrelativo"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_CASHIER_UID = "cajeroUid"
    }
}
