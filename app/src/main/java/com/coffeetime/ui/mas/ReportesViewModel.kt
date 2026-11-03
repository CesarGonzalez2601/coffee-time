package com.coffeetime.ui.mas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.model.CierreCaja
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.model.Reporte
import com.coffeetime.domain.repository.CierreCajaRepository
import com.coffeetime.domain.repository.OrderRepository
import com.coffeetime.domain.repository.PaymentRepository
import com.coffeetime.domain.repository.ProductRepository
import com.coffeetime.domain.service.ReporteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class FiltroFecha {
    HOY, ESTA_SEMANA, ESTE_MES, CALENDARIO
}

data class ReportesUiState(
    val reporteActual: Reporte = Reporte(0.0, 0, 0.0, emptyList(), emptyMap(), emptyMap()),
    val filtroFechaSeleccionado: FiltroFecha = FiltroFecha.HOY,

    val totalIngresado: Double = 0.0,
    val totalEfectivo: Double = 0.0,
    val totalTarjeta: Double = 0.0,
    val totalOrdenes: Int = 0,
    val montoInicialText: String = "50.00",
    val efectivoReportadoText: String = "",
    val observacionesText: String = "",
    val historialCierres: List<CierreCaja> = emptyList(),
    val isSubmitting: Boolean = false,
    val cierreSuccessMessage: String? = null,
    val cierreErrorMessage: String? = null
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
    private val cierreCajaRepository: CierreCajaRepository,
    private val reporteService: ReporteService = ReporteService()
) : ViewModel() {

    private val _montoInicialText = MutableStateFlow("50.00")
    private val _efectivoReportadoText = MutableStateFlow("")
    private val _observacionesText = MutableStateFlow("")
    private val _isSubmitting = MutableStateFlow(false)
    private val _cierreSuccessMessage = MutableStateFlow<String?>(null)

    private val _cierreErrorMessage = MutableStateFlow<String?>(null)
    private val _filtroFechaSeleccionado = MutableStateFlow(FiltroFecha.HOY)

    private val _cierreDataFlow = combine(
        paymentRepository.getAllPaymentsFlow(),
        orderRepository.getOrdersByStatusFlow(EstadoOrden.PAGADA),
        cierreCajaRepository.getAllCierresFlow()
    ) { payments, paidOrders, cierres ->
        val lastCierreTimestamp = cierres.maxOfOrNull { it.fechaTimestamp } ?: 0L

        val activeSessionOrders = paidOrders.filter { it.fechaTimestamp > lastCierreTimestamp }
        val activeSessionPayments = payments.filter { it.fechaTimestamp > lastCierreTimestamp }

        val totalIngresado = activeSessionOrders.sumOf { it.total }

        val cashOrderIds = activeSessionPayments.filter { it.metodoPago == MetodoPago.EFECTIVO }.map { it.ordenId }.toSet()
        val cardOrderIds = activeSessionPayments.filter { it.metodoPago == MetodoPago.TARJETA }.map { it.ordenId }.toSet()

        val totalEfectivo = activeSessionOrders.filter { it.id in cashOrderIds }.sumOf { it.total }
        val totalTarjeta = activeSessionOrders.filter { it.id in cardOrderIds }.sumOf { it.total }
        val totalOrdenes = activeSessionOrders.size

        Triple(totalIngresado, Pair(totalEfectivo, totalTarjeta), totalOrdenes)
    }

    private val _reporteFlow = combine(
        orderRepository.getAllOrdersFlow(),
        paymentRepository.getAllPaymentsFlow(),
        productRepository.getProductsFlow(),
        _filtroFechaSeleccionado
    ) { ordenes, pagos, productos, filtro ->
        val now = LocalDateTime.now()
        val (fechaInicio, fechaFin) = when (filtro) {
            FiltroFecha.HOY -> Pair(now.with(LocalTime.MIN), now.with(LocalTime.MAX))
            FiltroFecha.ESTA_SEMANA -> Pair(now.minusDays(now.dayOfWeek.value.toLong() - 1).with(LocalTime.MIN), now.with(LocalTime.MAX))
            FiltroFecha.ESTE_MES -> Pair(now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN), now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX))
            FiltroFecha.CALENDARIO -> Pair(now.minusDays(30).with(LocalTime.MIN), now.with(LocalTime.MAX))
        }

        reporteService.generarReporte(ordenes, pagos, productos, fechaInicio, fechaFin)
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
        _cierreDataFlow,
        _reporteFlow,
        _cierreFormInputs,
        _filtroFechaSeleccionado,
        cierreCajaRepository.getAllCierresFlow()
    ) { cierreData, reporte, form, filtro, cierres ->
        ReportesUiState(
            reporteActual = reporte,
            filtroFechaSeleccionado = filtro,
            totalIngresado = cierreData.first,
            totalEfectivo = cierreData.second.first,
            totalTarjeta = cierreData.second.second,
            totalOrdenes = cierreData.third,
            montoInicialText = form.montoInicial,
            efectivoReportadoText = form.efectivoReportado,
            observacionesText = form.observaciones,
            historialCierres = cierres,
            isSubmitting = form.isSubmitting,
            cierreSuccessMessage = _cierreSuccessMessage.value,
            cierreErrorMessage = _cierreErrorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportesUiState()
    )

    fun onFiltroFechaSeleccionado(filtro: FiltroFecha) {
        _filtroFechaSeleccionado.value = filtro
    }

    fun onMontoInicialChange(text: String) {
        _montoInicialText.value = text.filter { it.isDigit() || it == '.' }
    }

    fun onEfectivoReportadoChange(text: String) {
        _efectivoReportadoText.value = text.filter { it.isDigit() || it == '.' }
    }

    fun onObservacionesChange(text: String) {
        _observacionesText.value = text
    }

    fun realizarCierreCaja(cajeroUid: String, cashierName: String) {
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
                    cajeroUid = cajeroUid,
                    cashierName = cashierName
                )

                cierreCajaRepository.realizarCierre(cierre)
                _cierreSuccessMessage.value = "Cierre de caja registrado exitosamente"
                _efectivoReportadoText.value = ""
                _observacionesText.value = ""
            } catch (e: Exception) {
                _cierreErrorMessage.value = "Fallo al realizar el cierre: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun dismissSuccessMessage() {
        _cierreSuccessMessage.value = null
    }

    fun dismissErrorMessage() {
        _cierreErrorMessage.value = null
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