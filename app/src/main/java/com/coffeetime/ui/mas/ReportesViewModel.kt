package com.coffeetime.ui.mas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.model.CierreCaja
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.repository.CierreCajaRepository
import com.coffeetime.domain.repository.OrderRepository
import com.coffeetime.domain.repository.PaymentRepository
import com.coffeetime.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TopProductItem(
    val ranking: Int,
    val productoId: Int,
    val nombre: String,
    val emoji: String,
    val cantidadVendida: Int,
    val totalMonto: Double
)

data class ReportesUiState(
    val totalIngresado: Double = 0.0,
    val totalEfectivo: Double = 0.0,
    val totalTarjeta: Double = 0.0,
    val totalOrdenes: Int = 0,
    val montoInicialText: String = "50.00",
    val efectivoReportadoText: String = "",
    val observacionesText: String = "",
    val ventasHoy: Double = 0.0,
    val ventasSemana: Double = 0.0,
    val ventasMes: Double = 0.0,
    val top5Productos: List<TopProductItem> = emptyList(),
    val historialCierres: List<CierreCaja> = emptyList(),
    val isSubmitting: Boolean = false,
    val cierreSuccessMessage: String? = null
) {
    val montoInicial: Double
        get() = montoInicialText.toDoubleOrNull() ?: 0.0

    val efectivoEsperado: Double
        get() = montoInicial + totalEfectivo

    val efectivoReportado: Double
        get() = efectivoReportadoText.toDoubleOrNull() ?: 0.0

    val diferencia: Double
        get() = efectivoReportado - efectivoEsperado

    val canRealizarCierre: Boolean
        get() = !isSubmitting && efectivoReportadoText.isNotBlank()
}

private data class SalesComputationResult(
    val totalIngresado: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val totalOrdenes: Int,
    val ventasHoy: Double,
    val ventasSemana: Double,
    val ventasMes: Double,
    val top5Productos: List<TopProductItem>
)

private data class CierreFormInputs(
    val montoInicial: String,
    val efectivoReportado: String,
    val observaciones: String,
    val isSubmitting: Boolean
)

class ReportesViewModel(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val cierreCajaRepository: CierreCajaRepository
) : ViewModel() {

    private val _montoInicialText = MutableStateFlow("50.00")
    private val _efectivoReportadoText = MutableStateFlow("")
    private val _observacionesText = MutableStateFlow("")
    private val _isSubmitting = MutableStateFlow(false)
    private val _cierreSuccessMessage = MutableStateFlow<String?>(null)

    private val _salesDataFlow = combine(
        paymentRepository.getAllPaymentsFlow(),
        orderRepository.getOrdersByStatusFlow(EstadoOrden.PAGADA),
        productRepository.getProductsFlow(),
        cierreCajaRepository.getAllCierresFlow()
    ) { payments, paidOrders, products, cierres ->
        val lastCierreTimestamp = cierres.maxOfOrNull { it.fechaTimestamp } ?: 0L

        // Active session paid orders (created AFTER the last cash closure)
        val activeSessionOrders = paidOrders.filter { it.fechaTimestamp > lastCierreTimestamp }
        val activeSessionPayments = payments.filter { it.fechaTimestamp > lastCierreTimestamp }

        val totalIngresado = activeSessionOrders.sumOf { it.total }

        val cashOrderIds = activeSessionPayments.filter { it.metodoPago == MetodoPago.EFECTIVO }.map { it.ordenId }.toSet()
        val cardOrderIds = activeSessionPayments.filter { it.metodoPago == MetodoPago.TARJETA }.map { it.ordenId }.toSet()

        val totalEfectivo = activeSessionOrders.filter { it.id in cashOrderIds }.sumOf { it.total }
        val totalTarjeta = activeSessionOrders.filter { it.id in cardOrderIds }.sumOf { it.total }
        val totalOrdenes = activeSessionOrders.size

        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000L)
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)

        val ventasHoy = paidOrders.filter { it.fechaTimestamp >= oneDayAgo }.sumOf { it.total }
        val ventasSemana = paidOrders.filter { it.fechaTimestamp >= sevenDaysAgo }.sumOf { it.total }
        val ventasMes = paidOrders.filter { it.fechaTimestamp >= thirtyDaysAgo }.sumOf { it.total }

        // Calculation of Top 5 Products across all paid orders
        val allItems = paidOrders.flatMap { it.items }
        val productMap = products.associateBy { it.id }

        val topProducts = allItems
            .groupBy { it.productoId }
            .map { (prodId, items) ->
                val qty = items.sumOf { it.cantidad }
                val revenue = items.sumOf { it.subtotalLinea }
                val product = productMap[prodId]
                val name = product?.nombre ?: items.firstOrNull()?.nombreProducto ?: "Producto"
                val emoji = product?.emoji ?: "☕"
                Triple(prodId, qty, Pair(revenue, Pair(name, emoji)))
            }
            .sortedByDescending { it.second }
            .take(5)
            .mapIndexed { index, triple ->
                TopProductItem(
                    ranking = index + 1,
                    productoId = triple.first,
                    nombre = triple.third.second.first,
                    emoji = triple.third.second.second,
                    cantidadVendida = triple.second,
                    totalMonto = triple.third.first
                )
            }

        SalesComputationResult(
            totalIngresado = totalIngresado,
            totalEfectivo = totalEfectivo,
            totalTarjeta = totalTarjeta,
            totalOrdenes = totalOrdenes,
            ventasHoy = ventasHoy,
            ventasSemana = ventasSemana,
            ventasMes = ventasMes,
            top5Productos = topProducts
        )
    }

    private val _cierreFormInputs = combine(
        _montoInicialText,
        _efectivoReportadoText,
        _observacionesText,
        _isSubmitting
    ) { montoInicial, efectivoReportado, observaciones, submitting ->
        CierreFormInputs(montoInicial, efectivoReportado, observaciones, submitting)
    }

    val uiState: StateFlow<ReportesUiState> = combine(
        _salesDataFlow,
        _cierreFormInputs,
        cierreCajaRepository.getAllCierresFlow()
    ) { sales, form, cierres ->
        ReportesUiState(
            totalIngresado = sales.totalIngresado,
            totalEfectivo = sales.totalEfectivo,
            totalTarjeta = sales.totalTarjeta,
            totalOrdenes = sales.totalOrdenes,
            montoInicialText = form.montoInicial,
            efectivoReportadoText = form.efectivoReportado,
            observacionesText = form.observaciones,
            ventasHoy = sales.ventasHoy,
            ventasSemana = sales.ventasSemana,
            ventasMes = sales.ventasMes,
            top5Productos = sales.top5Productos,
            historialCierres = cierres,
            isSubmitting = form.isSubmitting,
            cierreSuccessMessage = _cierreSuccessMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportesUiState()
    )

    fun onMontoInicialChange(text: String) {
        _montoInicialText.value = text.filter { it.isDigit() || it == '.' }
    }

    fun onEfectivoReportadoChange(text: String) {
        _efectivoReportadoText.value = text.filter { it.isDigit() || it == '.' }
    }

    fun onObservacionesChange(text: String) {
        _observacionesText.value = text
    }

    fun realizarCierreCaja(cajeroId: Int, cashierName: String) {
        val state = uiState.value
        if (!state.canRealizarCierre || state.isSubmitting) return

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val cierre = CierreCaja(
                    totalIngresado = state.totalIngresado,
                    totalEfectivo = state.totalEfectivo,
                    totalTarjeta = state.totalTarjeta,
                    totalOrdenes = state.totalOrdenes,
                    montoInicialCaja = state.montoInicial,
                    efectivoEsperado = state.efectivoEsperado,
                    efectivoReportado = state.efectivoReportado,
                    diferencia = state.diferencia,
                    observaciones = state.observacionesText,
                    cajeroId = cajeroId,
                    cashierName = cashierName
                )

                cierreCajaRepository.realizarCierre(cierre)
                _cierreSuccessMessage.value = "Cierre de caja registrado exitosamente"
                _efectivoReportadoText.value = ""
                _observacionesText.value = ""
            } catch (_: Exception) {
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun dismissSuccessMessage() {
        _cierreSuccessMessage.value = null
    }

    companion object {
        fun provideFactory(
            paymentRepository: PaymentRepository,
            orderRepository: OrderRepository,
            productRepository: ProductRepository,
            cierreCajaRepository: CierreCajaRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReportesViewModel(
                    paymentRepository,
                    orderRepository,
                    productRepository,
                    cierreCajaRepository
                ) as T
            }
        }
    }
}
