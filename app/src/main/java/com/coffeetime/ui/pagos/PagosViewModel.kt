package com.coffeetime.ui.pagos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.model.MovimientoInventario
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.model.Pago
import com.coffeetime.domain.model.TipoMovimiento
import com.coffeetime.domain.repository.InventoryRepository
import com.coffeetime.domain.repository.OrderRepository
import com.coffeetime.domain.repository.PaymentRepository
import com.coffeetime.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaidOrderRecord(
    val order: Orden,
    val payment: Pago
)

data class PagosUiState(
    val pendingOrders: List<Orden> = emptyList(),
    val paidOrders: List<PaidOrderRecord> = emptyList(),
    val selectedOrder: Orden? = null,
    val paymentMethod: MetodoPago = MetodoPago.EFECTIVO,
    val amountReceivedText: String = "",
    val isProcessing: Boolean = false,
    val completedReceipt: CompletedReceiptData? = null
) {
    val amountReceived: Double
        get() = amountReceivedText.toDoubleOrNull() ?: 0.0

    val totalToPay: Double
        get() = selectedOrder?.total ?: 0.0

    val cambio: Double
        get() = if (paymentMethod == MetodoPago.EFECTIVO) {
            maxOf(0.0, amountReceived - totalToPay)
        } else {
            0.0
        }

    val totalRecaudado: Double
        get() = paidOrders.sumOf { it.order.total }

    val canProcessPayment: Boolean
        get() = when {
            selectedOrder == null || isProcessing -> false
            paymentMethod == MetodoPago.TARJETA -> true
            paymentMethod == MetodoPago.EFECTIVO -> amountReceived >= totalToPay && totalToPay > 0
            else -> false
        }
}

data class CompletedReceiptData(
    val correlative: String,
    val order: Orden,
    val paymentMethod: MetodoPago,
    val amountReceived: Double,
    val cambio: Double,
    val timestamp: Long,
    val cashierName: String
)

class PagosViewModel(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _selectedOrder = MutableStateFlow<Orden?>(null)
    private val _paymentMethod = MutableStateFlow(MetodoPago.EFECTIVO)
    private val _amountReceivedText = MutableStateFlow("")
    private val _isProcessing = MutableStateFlow(false)
    private val _completedReceipt = MutableStateFlow<CompletedReceiptData?>(null)

    private val _pendingOrdersState = combine(
        orderRepository.getOrdersByStatusFlow(EstadoOrden.PENDIENTE),
        _selectedOrder,
        _paymentMethod,
        _amountReceivedText,
        _isProcessing
    ) { pendingOrders, selectedOrder, method, amountText, processing ->
        val activeOrder = if (selectedOrder != null && pendingOrders.any { it.id == selectedOrder.id }) {
            pendingOrders.first { it.id == selectedOrder.id }
        } else {
            pendingOrders.firstOrNull()
        }
        Pair(pendingOrders to activeOrder, Triple(method, amountText, processing))
    }

    private val _paidOrdersState = combine(
        paymentRepository.getAllPaymentsFlow(),
        orderRepository.getAllOrdersFlow()
    ) { payments, allOrders ->
        payments.mapNotNull { payment ->
            val order = allOrders.find { it.id == payment.ordenId }
            if (order != null) PaidOrderRecord(order = order, payment = payment) else null
        }
    }

    val uiState: StateFlow<PagosUiState> = combine(
        _pendingOrdersState,
        _paidOrdersState,
        _completedReceipt
    ) { pendingState, paidOrders, receipt ->
        val (orderPair, triple) = pendingState
        val (pendingOrders, activeOrder) = orderPair
        val (method, amountText, processing) = triple

        PagosUiState(
            pendingOrders = pendingOrders,
            paidOrders = paidOrders,
            selectedOrder = activeOrder,
            paymentMethod = method,
            amountReceivedText = amountText,
            isProcessing = processing,
            completedReceipt = receipt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PagosUiState()
    )

    fun selectOrder(order: Orden) {
        _selectedOrder.value = order
        _amountReceivedText.value = ""
    }

    fun setPaymentMethod(method: MetodoPago) {
        _paymentMethod.value = method
        if (method == MetodoPago.TARJETA) {
            val currentOrder = uiState.value.selectedOrder
            if (currentOrder != null) {
                _amountReceivedText.value = currentOrder.total.toString()
            }
        }
    }

    fun onAmountReceivedChange(text: String) {
        _amountReceivedText.value = text.filter { it.isDigit() || it == '.' }
    }

    fun addQuickCashAmount(amount: Double) {
        val currentOrder = uiState.value.selectedOrder ?: return
        if (amount == -1.0) {
            _amountReceivedText.value = String.format(java.util.Locale.US, "%.2f", currentOrder.total)
        } else {
            _amountReceivedText.value = String.format(java.util.Locale.US, "%.2f", amount)
        }
    }

    fun viewReceiptForPaidOrder(record: PaidOrderRecord, cashierName: String) {
        _completedReceipt.value = CompletedReceiptData(
            correlative = record.payment.comprobanteCorrelativo,
            order = record.order,
            paymentMethod = record.payment.metodoPago,
            amountReceived = record.payment.montoRecibido,
            cambio = record.payment.cambio,
            timestamp = record.payment.fechaTimestamp,
            cashierName = cashierName
        )
    }

    fun processPayment(cajeroUid: String, cashierName: String) {
        val state = uiState.value
        val order = state.selectedOrder ?: return
        if (!state.canProcessPayment || state.isProcessing) return

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val amountReceived = if (state.paymentMethod == MetodoPago.TARJETA) order.total else state.amountReceived
                val cambio = if (state.paymentMethod == MetodoPago.TARJETA) 0.0 else maxOf(0.0, amountReceived - order.total)

                val pago = Pago(
                    ordenId = order.id,
                    metodoPago = state.paymentMethod,
                    montoRecibido = amountReceived,
                    cambio = cambio,
                    comprobanteCorrelativo = "",
                    cajeroUid = cajeroUid
                )

                val correlative = paymentRepository.processPayment(pago)

                // Update order status to PAGADA
                orderRepository.updateOrderStatus(order.id, EstadoOrden.PAGADA)

                // Deduct stock for items and log inventory movement
                for (item in order.items) {
                    val product = productRepository.getProductById(item.productoId)
                    if (product != null) {
                        val previousStock = product.stockActual
                        val newStock = maxOf(0, previousStock - item.cantidad)
                        productRepository.updateStock(product.id, newStock)

                        inventoryRepository.registerMovement(
                            MovimientoInventario(
                                productoId = product.id,
                                nombreProducto = product.nombre,
                                tipo = TipoMovimiento.SALIDA_VENTA,
                                cantidad = item.cantidad,
                                stockAnterior = previousStock,
                                stockNuevo = newStock,
                                motivo = "Venta Orden #${order.id} ($correlative)",
                                usuarioUid = cajeroUid
                            )
                        )
                    }
                }

                _completedReceipt.value = CompletedReceiptData(
                    correlative = correlative,
                    order = order,
                    paymentMethod = state.paymentMethod,
                    amountReceived = amountReceived,
                    cambio = cambio,
                    timestamp = System.currentTimeMillis(),
                    cashierName = cashierName
                )
                _selectedOrder.value = null
                _amountReceivedText.value = ""
            } catch (_: Exception) {
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun dismissReceipt() {
        _completedReceipt.value = null
    }

    companion object {
        fun provideFactory(
            orderRepository: OrderRepository,
            paymentRepository: PaymentRepository,
            productRepository: ProductRepository,
            inventoryRepository: InventoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PagosViewModel(
                    orderRepository,
                    paymentRepository,
                    productRepository,
                    inventoryRepository
                ) as T
            }
        }
    }
}
