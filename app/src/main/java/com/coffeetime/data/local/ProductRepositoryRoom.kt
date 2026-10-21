package com.coffeetime.data.local

import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryRoom(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProductsFlow(): Flow<List<Producto>> {
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProducts(): List<Producto> {
        return productDao.getAllProducts().map { it.toDomain() }
    }

    override suspend fun getProductById(id: Int): Producto? {
        return productDao.getProductById(id)?.toDomain()
    }

    override suspend fun addProduct(producto: Producto): Long {
        val entity = ProductEntity.fromDomain(producto)
        return productDao.insert(entity)
    }

    override suspend fun updateProduct(producto: Producto) {
        val entity = ProductEntity.fromDomain(producto)
        productDao.update(entity)
    }

    override suspend fun deleteProduct(productId: Int) {
        productDao.deleteProduct(productId)
    }

    override suspend fun updateStock(productId: Int, newStock: Int) {
        productDao.updateStock(productId, newStock)
    }
}
