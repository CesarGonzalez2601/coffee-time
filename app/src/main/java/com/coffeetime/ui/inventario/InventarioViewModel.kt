package com.coffeetime.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.MovimientoInventario
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.model.TipoMovimiento
import com.coffeetime.domain.repository.InventoryRepository
import com.coffeetime.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdjustmentFormState(
    val product: Producto,
    val type: TipoMovimiento = TipoMovimiento.ENTRADA,
    val quantityText: String = "",
    val reasonText: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val quantity: Int
        get() = quantityText.toIntOrNull() ?: 0

    val isValid: Boolean
        get() = quantity > 0
}

data class NewProductFormState(
    val name: String = "",
    val category: CategoriaProducto = CategoriaProducto.BEBIDAS_CALIENTES,
    val priceText: String = "",
    val initialStockText: String = "10",
    val minStockText: String = "5",
    val emoji: String = "☕",
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val price: Double
        get() = priceText.toDoubleOrNull() ?: 0.0

    val initialStock: Int
        get() = initialStockText.toIntOrNull() ?: 0

    val minStock: Int
        get() = minStockText.toIntOrNull() ?: 5

    val isValid: Boolean
        get() = name.isNotBlank() && price > 0.0 && initialStock >= 0 && minStock >= 0
}

data class EditProductFormState(
    val productId: Int,
    val name: String,
    val category: CategoriaProducto,
    val priceText: String,
    val minStockText: String,
    val emoji: String,
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val price: Double
        get() = priceText.toDoubleOrNull() ?: 0.0

    val minStock: Int
        get() = minStockText.toIntOrNull() ?: 5

    val isValid: Boolean
        get() = name.isNotBlank() && price > 0.0 && minStock >= 0
}

private data class DialogsState(
    val adjustment: AdjustmentFormState? = null,
    val newProduct: NewProductFormState? = null,
    val editProduct: EditProductFormState? = null,
    val deleteProduct: Producto? = null
)

data class InventarioUiState(
    val products: List<Producto> = emptyList(),
    val totalProductCount: Int = 0,
    val lowStockCount: Int = 0,
    val onlyLowStockFilter: Boolean = false,
    val searchQuery: String = "",
    val movements: List<MovimientoInventario> = emptyList(),
    val adjustmentFormState: AdjustmentFormState? = null,
    val newProductFormState: NewProductFormState? = null,
    val editProductFormState: EditProductFormState? = null,
    val productToDelete: Producto? = null
)

class InventarioViewModel(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _onlyLowStockFilter = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _adjustmentFormState = MutableStateFlow<AdjustmentFormState?>(null)
    private val _newProductFormState = MutableStateFlow<NewProductFormState?>(null)
    private val _editProductFormState = MutableStateFlow<EditProductFormState?>(null)
    private val _productToDelete = MutableStateFlow<Producto?>(null)

    private val _filteredProducts = combine(
        productRepository.getProductsFlow(),
        _onlyLowStockFilter,
        _searchQuery
    ) { products, lowStockOnly, query ->
        products.filter { product ->
            val matchesFilter = if (lowStockOnly) product.isStockBajo else true
            val matchesQuery = query.isBlank() || product.nombre.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    private val _stockSummary = combine(
        productRepository.getProductsFlow(),
        _filteredProducts
    ) { allProducts, filtered ->
        Pair(allProducts, filtered)
    }

    private val _allDialogsState = combine(
        _adjustmentFormState,
        _newProductFormState,
        _editProductFormState,
        _productToDelete
    ) { adjustment, newProduct, editProduct, deleteProduct ->
        DialogsState(adjustment, newProduct, editProduct, deleteProduct)
    }

    val uiState: StateFlow<InventarioUiState> = combine(
        _stockSummary,
        _onlyLowStockFilter,
        _searchQuery,
        inventoryRepository.getAllMovementsFlow(),
        _allDialogsState
    ) { stockData, lowStockOnly, query, movements, dialogs ->
        val (allProducts, filtered) = stockData
        InventarioUiState(
            products = filtered,
            totalProductCount = allProducts.size,
            lowStockCount = allProducts.count { it.isStockBajo },
            onlyLowStockFilter = lowStockOnly,
            searchQuery = query,
            movements = movements,
            adjustmentFormState = dialogs.adjustment,
            newProductFormState = dialogs.newProduct,
            editProductFormState = dialogs.editProduct,
            productToDelete = dialogs.deleteProduct
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventarioUiState()
    )

    fun setOnlyLowStockFilter(enabled: Boolean) {
        _onlyLowStockFilter.value = enabled
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openAdjustmentDialog(product: Producto) {
        _adjustmentFormState.value = AdjustmentFormState(product = product)
    }

    fun updateAdjustmentForm(
        type: TipoMovimiento? = null,
        quantityText: String? = null,
        reasonText: String? = null
    ) {
        val current = _adjustmentFormState.value ?: return
        _adjustmentFormState.value = current.copy(
            type = type ?: current.type,
            quantityText = quantityText ?: current.quantityText,
            reasonText = reasonText ?: current.reasonText
        )
    }

    fun closeAdjustmentDialog() {
        _adjustmentFormState.value = null
    }

    fun submitStockAdjustment() {
        val form = _adjustmentFormState.value ?: return
        if (!form.isValid || form.isSubmitting) return

        viewModelScope.launch {
            _adjustmentFormState.value = form.copy(isSubmitting = true)
            try {
                val previousStock = form.product.stockActual
                val delta = form.quantity
                val newStock = when (form.type) {
                    TipoMovimiento.ENTRADA -> previousStock + delta
                    TipoMovimiento.AJUSTE_MANUAL -> delta
                    TipoMovimiento.SALIDA_VENTA -> maxOf(0, previousStock - delta)
                }

                productRepository.updateStock(form.product.id, newStock)

                inventoryRepository.registerMovement(
                    MovimientoInventario(
                        productoId = form.product.id,
                        nombreProducto = form.product.nombre,
                        tipo = form.type,
                        cantidad = delta,
                        stockAnterior = previousStock,
                        stockNuevo = newStock,
                        motivo = form.reasonText.ifBlank { form.type.displayName }
                    )
                )

                _adjustmentFormState.value = null
            } catch (e: Exception) {
                _adjustmentFormState.value = form.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun openNewProductDialog() {
        _newProductFormState.value = NewProductFormState()
    }

    fun updateNewProductForm(
        name: String? = null,
        category: CategoriaProducto? = null,
        priceText: String? = null,
        initialStockText: String? = null,
        minStockText: String? = null,
        emoji: String? = null
    ) {
        val current = _newProductFormState.value ?: return
        _newProductFormState.value = current.copy(
            name = name ?: current.name,
            category = category ?: current.category,
            priceText = priceText ?: current.priceText,
            initialStockText = initialStockText ?: current.initialStockText,
            minStockText = minStockText ?: current.minStockText,
            emoji = emoji ?: current.emoji
        )
    }

    fun closeNewProductDialog() {
        _newProductFormState.value = null
    }

    fun submitNewProduct() {
        val form = _newProductFormState.value ?: return
        if (!form.isValid || form.isSubmitting) return

        viewModelScope.launch {
            _newProductFormState.value = form.copy(isSubmitting = true)
            try {
                val producto = Producto(
                    nombre = form.name.trim(),
                    categoria = form.category,
                    precio = form.price,
                    stockActual = form.initialStock,
                    stockMinimo = form.minStock,
                    emoji = form.emoji
                )

                val newId = productRepository.addProduct(producto)

                if (form.initialStock > 0) {
                    inventoryRepository.registerMovement(
                        MovimientoInventario(
                            productoId = newId.toInt(),
                            nombreProducto = producto.nombre,
                            tipo = TipoMovimiento.ENTRADA,
                            cantidad = form.initialStock,
                            stockAnterior = 0,
                            stockNuevo = form.initialStock,
                            motivo = "Creación de nuevo producto"
                        )
                    )
                }

                _newProductFormState.value = null
            } catch (e: Exception) {
                _newProductFormState.value = form.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun openEditProductDialog(product: Producto) {
        _editProductFormState.value = EditProductFormState(
            productId = product.id,
            name = product.nombre,
            category = product.categoria,
            priceText = String.format(java.util.Locale.US, "%.2f", product.precio),
            minStockText = product.stockMinimo.toString(),
            emoji = product.emoji
        )
    }

    fun updateEditProductForm(
        name: String? = null,
        category: CategoriaProducto? = null,
        priceText: String? = null,
        minStockText: String? = null,
        emoji: String? = null
    ) {
        val current = _editProductFormState.value ?: return
        _editProductFormState.value = current.copy(
            name = name ?: current.name,
            category = category ?: current.category,
            priceText = priceText ?: current.priceText,
            minStockText = minStockText ?: current.minStockText,
            emoji = emoji ?: current.emoji
        )
    }

    fun closeEditProductDialog() {
        _editProductFormState.value = null
    }

    fun submitEditProduct() {
        val form = _editProductFormState.value ?: return
        if (!form.isValid || form.isSubmitting) return

        viewModelScope.launch {
            _editProductFormState.value = form.copy(isSubmitting = true)
            try {
                val existing = productRepository.getProductById(form.productId)
                if (existing != null) {
                    val updated = existing.copy(
                        nombre = form.name.trim(),
                        categoria = form.category,
                        precio = form.price,
                        stockMinimo = form.minStock,
                        emoji = form.emoji
                    )
                    productRepository.updateProduct(updated)
                }
                _editProductFormState.value = null
            } catch (e: Exception) {
                _editProductFormState.value = form.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun openDeleteProductDialog(product: Producto) {
        _productToDelete.value = product
    }

    fun closeDeleteProductDialog() {
        _productToDelete.value = null
    }

    fun deleteProduct() {
        val product = _productToDelete.value ?: return
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(product.id)
                _productToDelete.value = null
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        fun provideFactory(
            productRepository: ProductRepository,
            inventoryRepository: InventoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InventarioViewModel(productRepository, inventoryRepository) as T
            }
        }
    }
}
