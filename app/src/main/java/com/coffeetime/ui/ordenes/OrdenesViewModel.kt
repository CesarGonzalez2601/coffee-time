package com.coffeetime.ui.ordenes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.model.CartItem
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.DetalleOrden
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.repository.OrderRepository
import com.coffeetime.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdenesUiState(
    val products: List<Producto> = emptyList(),
    val selectedCategory: CategoriaProducto = CategoriaProducto.TODOS,
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val isSubmitting: Boolean = false,
    val orderCreatedId: Int? = null
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.subtotalLinea }

    val iva: Double
        get() = subtotal * 0.13

    val total: Double
        get() = subtotal + iva

    val itemCount: Int
        get() = cartItems.sumOf { it.cantidad }
}

class OrdenesViewModel(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CategoriaProducto.TODOS)
    private val _searchQuery = MutableStateFlow("")
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _isSubmitting = MutableStateFlow(false)
    private val _orderCreatedId = MutableStateFlow<Int?>(null)

    private val _filteredProducts = combine(
        productRepository.getProductsFlow(),
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchesCategory = (category == CategoriaProducto.TODOS || product.categoria == category)
            val matchesQuery = query.isBlank() || product.nombre.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val uiState: StateFlow<OrdenesUiState> = combine(
        _filteredProducts,
        _selectedCategory,
        _searchQuery,
        _cartItems,
        _isSubmitting
    ) { products, category, query, cart, submitting ->
        OrdenesUiState(
            products = products,
            selectedCategory = category,
            searchQuery = query,
            cartItems = cart,
            isSubmitting = submitting,
            orderCreatedId = _orderCreatedId.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrdenesUiState()
    )

    fun onCategorySelected(category: CategoriaProducto) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(producto: Producto) {
        if (producto.stockActual <= 0) return
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.producto.id == producto.id }
        if (index >= 0) {
            val existing = current[index]
            if (existing.cantidad < producto.stockActual) {
                current[index] = existing.copy(cantidad = existing.cantidad + 1)
            }
        } else {
            current.add(CartItem(producto = producto, cantidad = 1))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productoId: Int, newQuantity: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.producto.id == productoId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                val existing = current[index]
                if (newQuantity <= existing.producto.stockActual) {
                    current[index] = existing.copy(cantidad = newQuantity)
                }
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productoId: Int) {
        _cartItems.value = _cartItems.value.filter { it.producto.id != productoId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun confirmOrder(cajeroId: Int, onSuccess: (Int) -> Unit) {
        val cart = _cartItems.value
        if (cart.isEmpty() || _isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val subtotal = cart.sumOf { it.subtotalLinea }
                val iva = subtotal * 0.13
                val total = subtotal + iva

                val items = cart.map { item ->
                    DetalleOrden(
                        productoId = item.producto.id,
                        nombreProducto = item.producto.nombre,
                        precioUnitario = item.producto.precio,
                        cantidad = item.cantidad
                    )
                }

                val orden = Orden(
                    items = items,
                    subtotal = subtotal,
                    iva = iva,
                    total = total,
                    estado = EstadoOrden.PENDIENTE,
                    cajeroId = cajeroId
                )

                val createdId = orderRepository.createOrder(orden)
                _orderCreatedId.value = createdId
                clearCart()
                onSuccess(createdId)
            } catch (_: Exception) {
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearOrderCreated() {
        _orderCreatedId.value = null
    }

    companion object {
        fun provideFactory(
            productRepository: ProductRepository,
            orderRepository: OrderRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrdenesViewModel(productRepository, orderRepository) as T
            }
        }
    }
}
