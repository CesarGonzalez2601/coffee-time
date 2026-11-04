package com.coffeetime.data.firebase

import android.util.Log
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProductRepository {

    private val collection
        get() = firestore.collection(COLLECTION)

    override fun getProductsFlow(): Flow<List<Producto>> = callbackFlow {
        val listener = collection.whereEqualTo(FIELD_ACTIVE, true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getProductsFlow snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        Producto(
                            id = doc.getLong(FIELD_ID)?.toInt() ?: doc.id.toIntOrNull() ?: 0,
                            nombre = doc.getString(FIELD_NAME).orEmpty(),
                            categoria = runCatching { CategoriaProducto.valueOf(doc.getString(FIELD_CATEGORY).orEmpty()) }.getOrDefault(CategoriaProducto.TODOS),
                            precio = doc.getDouble(FIELD_PRICE) ?: 0.0,
                            stockActual = doc.getLong(FIELD_STOCK_ACTUAL)?.toInt() ?: 0,
                            stockMinimo = doc.getLong(FIELD_STOCK_MINIMO)?.toInt() ?: 5,
                            emoji = doc.getString(FIELD_EMOJI) ?: "☕",
                            activo = doc.getBoolean(FIELD_ACTIVE) ?: true
                        )
                    }.getOrNull()
                }.orEmpty().sortedBy { it.nombre }

                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getProducts(): List<Producto> {
        return try {
            val snapshot = collection.whereEqualTo(FIELD_ACTIVE, true).get().await()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    Producto(
                        id = doc.getLong(FIELD_ID)?.toInt() ?: doc.id.toIntOrNull() ?: 0,
                        nombre = doc.getString(FIELD_NAME).orEmpty(),
                        categoria = runCatching { CategoriaProducto.valueOf(doc.getString(FIELD_CATEGORY).orEmpty()) }.getOrDefault(CategoriaProducto.TODOS),
                        precio = doc.getDouble(FIELD_PRICE) ?: 0.0,
                        stockActual = doc.getLong(FIELD_STOCK_ACTUAL)?.toInt() ?: 0,
                        stockMinimo = doc.getLong(FIELD_STOCK_MINIMO)?.toInt() ?: 5,
                        emoji = doc.getString(FIELD_EMOJI) ?: "☕",
                        activo = doc.getBoolean(FIELD_ACTIVE) ?: true
                    )
                }.getOrNull()
            }.sortedBy { it.nombre }
        } catch (e: Exception) {
            Log.w(TAG, "getProducts error", e)
            emptyList()
        }
    }

    override suspend fun getProductById(id: Int): Producto? {
        return try {
            val doc = collection.document(id.toString()).get().await()
            if (!doc.exists()) return null
            Producto(
                id = doc.getLong(FIELD_ID)?.toInt() ?: id,
                nombre = doc.getString(FIELD_NAME).orEmpty(),
                categoria = runCatching { CategoriaProducto.valueOf(doc.getString(FIELD_CATEGORY).orEmpty()) }.getOrDefault(CategoriaProducto.TODOS),
                precio = doc.getDouble(FIELD_PRICE) ?: 0.0,
                stockActual = doc.getLong(FIELD_STOCK_ACTUAL)?.toInt() ?: 0,
                stockMinimo = doc.getLong(FIELD_STOCK_MINIMO)?.toInt() ?: 5,
                emoji = doc.getString(FIELD_EMOJI) ?: "☕",
                activo = doc.getBoolean(FIELD_ACTIVE) ?: true
            )
        } catch (e: Exception) {
            Log.w(TAG, "getProductById error", e)
            null
        }
    }

    override suspend fun addProduct(producto: Producto): Long {
        val nextId = getNextId()
        val docRef = collection.document(nextId.toString())

        val data = mapOf(
            FIELD_ID to nextId,
            FIELD_NAME to producto.nombre,
            FIELD_CATEGORY to producto.categoria.name,
            FIELD_PRICE to producto.precio,
            FIELD_STOCK_ACTUAL to producto.stockActual,
            FIELD_STOCK_MINIMO to producto.stockMinimo,
            FIELD_EMOJI to producto.emoji,
            FIELD_ACTIVE to true
        )

        docRef.set(data).await()
        return nextId
    }

    override suspend fun updateProduct(producto: Producto) {
        val docRef = collection.document(producto.id.toString())
        val data = mapOf(
            FIELD_NAME to producto.nombre,
            FIELD_CATEGORY to producto.categoria.name,
            FIELD_PRICE to producto.precio,
            FIELD_STOCK_MINIMO to producto.stockMinimo,
            FIELD_EMOJI to producto.emoji
        )
        docRef.update(data).await()
    }

    override suspend fun deleteProduct(productId: Int) {
        collection.document(productId.toString()).update(FIELD_ACTIVE, false).await()
    }

    override suspend fun updateStock(productId: Int, newStock: Int) {
        collection.document(productId.toString()).update(FIELD_STOCK_ACTUAL, newStock).await()
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

    private companion object {
        const val TAG = "ProductRepositoryFS"
        const val COLLECTION = "productos"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_CATEGORY = "category"
        const val FIELD_PRICE = "price"
        const val FIELD_STOCK_ACTUAL = "stockActual"
        const val FIELD_STOCK_MINIMO = "stockMinimo"
        const val FIELD_EMOJI = "emoji"
        const val FIELD_ACTIVE = "active"
    }
}
