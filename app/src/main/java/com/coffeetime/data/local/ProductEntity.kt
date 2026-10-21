package com.coffeetime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.Producto

@Entity(tableName = ProductEntity.TABLE)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    @ColumnInfo(name = "stock_actual")
    val stockActual: Int,
    @ColumnInfo(name = "stock_minimo")
    val stockMinimo: Int,
    val emoji: String,
    val active: Boolean = true
) {
    fun toDomain(): Producto = Producto(
        id = id,
        nombre = name,
        categoria = runCatching { CategoriaProducto.valueOf(category) }.getOrDefault(CategoriaProducto.TODOS),
        precio = price,
        stockActual = stockActual,
        stockMinimo = stockMinimo,
        emoji = emoji,
        activo = active
    )

    companion object {
        const val TABLE = "products"

        fun fromDomain(producto: Producto): ProductEntity = ProductEntity(
            id = producto.id,
            name = producto.nombre,
            category = producto.categoria.name,
            price = producto.precio,
            stockActual = producto.stockActual,
            stockMinimo = producto.stockMinimo,
            emoji = producto.emoji,
            active = producto.activo
        )
    }
}
