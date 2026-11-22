package com.coffeetime.ui.ordenes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.model.CartItem
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.DetalleOrden
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.model.Pago
import com.coffeetime.domain.model.Permission
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.model.User
import com.coffeetime.domain.repository.OrderRepository
import com.coffeetime.domain.repository.PaymentRepository
import com.coffeetime.domain.repository.ProductRepository
import com.coffeetime.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FiltroFecha(val label: String) {
    HOY("Hoy"),
    SEMANA("Últimos 7 días"),
    MES("Últimos 30 días"),
    TODAS("Todas las fechas")
}

data class OrderHistoryItem(
    val orden: Orden,
    val pago: Pago?,
    val cashierName: String
)

data class OrdenesUiState(
    val products: List<Producto> = emptyList(),
    val selectedCategory: CategoriaProducto = CategoriaProducto.TODOS,
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val isSubmitting: Boolean = false,
    val orderCreatedId: Int? = null,
    // Historial de Órdenes (#27)
    val historyOrders: List<OrderHistoryItem> = emptyList(),
    val selectedFiltroFecha: FiltroFecha = FiltroFecha.TODAS,
    val selectedCajeroUid: String? = null,
    val allCashiers: List<Pair<String, String>> = emptyList(),
    val canFilterByCashier: Boolean = false,
    val selectedOrderForDetail: OrderHistoryItem? = null
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
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CategoriaProducto.TODOS)
    private val _searchQuery = MutableStateFlow("")
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _isSubmitting = MutableStateFlow(false)
    private val _orderCreatedId = MutableStateFlow<Int?>(null)

    // Filtros de Historial
    private val _selectedFiltroFecha = MutableStateFlow(FiltroFecha.TODAS)
    private val _selectedCajeroUid = MutableStateFlow<String?>(null)
    private val _currentUser = MutableStateFlow<User?>(null)
    private val _selectedOrderForDetail = MutableStateFlow<OrderHistoryItem?>(null)

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

    private val _historyState = combine(
        orderRepository.getAllOrdersFlow(),
        paymentRepository.getAllPaymentsFlow(),
        _selectedFiltroFecha,
        _selectedCajeroUid,
        _currentUser
    ) { orders, payments, filtroFecha, cajeroUid, currentUser ->
        val paymentsMap = payments.associateBy { it.ordenId }

        // RBAC: Usamos currentUser?.hasPermission(Permission.VIEW_REPORTS), NUNCA comparamos roles.
        val canViewAllReports = currentUser?.hasPermission(Permission.VIEW_REPORTS) == true

        val now = System.currentTimeMillis()
        val minTimestamp = when (filtroFecha) {
            FiltroFecha.HOY -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            FiltroFecha.SEMANA -> now - (7L * 24 * 60 * 60 * 1000)
            FiltroFecha.MES -> now - (30L * 24 * 60 * 60 * 1000)
            FiltroFecha.TODAS -> 0L
        }

        val filteredOrders = orders.filter { orden ->
            val matchesDate = orden.fechaTimestamp >= minTimestamp

            // Filtro por cajero con RBAC
            val matchesCashier = if (!canViewAllReports) {
                // Si NO tiene permiso VIEW_REPORTS (Cajero), ve SOLO sus propias órdenes
                currentUser?.let { orden.cajeroUid == it.uid } ?: true
            } else {
                // Si TIENE permiso VIEW_REPORTS (Admin), ve todas o la del cajero seleccionado
                cajeroUid == null || orden.cajeroUid == cajeroUid
            }

            matchesDate && matchesCashier
        }

        val users = userRepository.findAll()
        val usersMap = users.associateBy { it.uid }

        val historyItems = filteredOrders.map { orden ->
            val cashierName = usersMap[orden.cajeroUid]?.name
                ?: if (orden.cajeroUid.isNotBlank()) "Cajero (${orden.cajeroUid.take(6)})" else "Cajero General"
            OrderHistoryItem(
                orden = orden,
                pago = paymentsMap[orden.id],
                cashierName = cashierName
            )
        }

        val allCashiers = users.map { it.uid to it.name }

        data class HistoryPayload(
            val historyOrders: List<OrderHistoryItem>,
            val allCashiers: List<Pair<String, String>>,
            val canFilterByCashier: Boolean
        )

        HistoryPayload(historyItems, allCashiers, canViewAllReports)
    }

    val uiState: StateFlow<OrdenesUiState> = combine(
        _filteredProducts,
        _selectedCategory,
        _searchQuery,
        _cartItems,
        _historyState
    ) { products, category, query, cart, historyPayload ->
        OrdenesUiState(
            products = products,
            selectedCategory = category,
            searchQuery = query,
            cartItems = cart,
            isSubmitting = _isSubmitting.value,
            orderCreatedId = _orderCreatedId.value,
            historyOrders = historyPayload.historyOrders,
            selectedFiltroFecha = _selectedFiltroFecha.value,
            selectedCajeroUid = _selectedCajeroUid.value,
            allCashiers = historyPayload.allCashiers,
            canFilterByCashier = historyPayload.canFilterByCashier,
            selectedOrderForDetail = _selectedOrderForDetail.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrdenesUiState()
    )

    fun setUser(user: User?) {
        _currentUser.value = user
    }

    fun onFiltroFechaSelected(filtro: FiltroFecha) {
        _selectedFiltroFecha.value = filtro
    }

    fun onCajeroUidSelected(cajeroUid: String?) {
        _selectedCajeroUid.value = cajeroUid
    }

    fun selectOrderForDetail(item: OrderHistoryItem?) {
        _selectedOrderForDetail.value = item
    }

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

    fun confirmOrder(cajeroUid: String, onSuccess: (Int) -> Unit) {
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
                    cajeroUid = cajeroUid
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
            orderRepository: OrderRepository,
            paymentRepository: PaymentRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrdenesViewModel(
                    productRepository = productRepository,
                    orderRepository = orderRepository,
                    paymentRepository = paymentRepository,
                    userRepository = userRepository
                ) as T
            }
        }
    }
}
