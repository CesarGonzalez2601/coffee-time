package com.coffeetime.domain.repository

import com.coffeetime.domain.model.Producto
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProductsFlow(): Flow<List<Producto>>
    suspend fun getProducts(): List<Producto>
    suspend fun getProductById(id: Int): Producto?
    suspend fun addProduct(producto: Producto): Long
    suspend fun updateProduct(producto: Producto)
    suspend fun deleteProduct(productId: Int)
    suspend fun updateStock(productId: Int, newStock: Int)
}
