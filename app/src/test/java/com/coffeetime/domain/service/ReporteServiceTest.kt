package com.coffeetime.domain.service

import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.DetalleOrden
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.model.Pago
import com.coffeetime.domain.model.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReporteServiceTest {

    private val reporteService = ReporteService()
    private val zoneId = ZoneId.systemDefault()

    private fun getMillis(fecha: LocalDateTime): Long {
        return fecha.atZone(zoneId).toInstant().toEpochMilli()
    }

    @Test
    fun `ticket promedio con cero ordenes no da error y devuelve cero`() {
        val inicio = LocalDateTime.now().minusDays(1)
        val fin = LocalDateTime.now()

        val reporte = reporteService.generarReporte(
            ordenes = emptyList(),
            pagos = emptyList(),
            productos = emptyList(),
            fechaInicio = inicio,
            fechaFin = fin
        )

        assertEquals(0.0, reporte.ticketPromedio, 0.0)
        assertEquals(0, reporte.ordenesCerradas)
    }

    @Test
    fun `periodo vacio retorna reporte en cero`() {
        val inicio = LocalDateTime.of(2026, 1, 1, 0, 0)
        val fin = LocalDateTime.of(2026, 1, 31, 23, 59)

        // Creamos una orden y pago FUERA del periodo
        val ordenFuera = Orden(id = 1, fechaTimestamp = getMillis(LocalDateTime.of(2026, 2, 1, 10, 0)), cajeroUid = "123")
        val pagoFuera = Pago(id = 1, ordenId = 1, metodoPago = MetodoPago.EFECTIVO, montoRecibido = 10.0, cambio = 0.0, comprobanteCorrelativo = "001", fechaTimestamp = getMillis(LocalDateTime.of(2026, 2, 1, 10, 5)), cajeroUid = "123")

        val reporte = reporteService.generarReporte(
            ordenes = listOf(ordenFuera),
            pagos = listOf(pagoFuera),
            productos = emptyList(),
            fechaInicio = inicio,
            fechaFin = fin
        )

        assertEquals(0.0, reporte.ingresosTotales, 0.0)
        assertTrue(reporte.ventasPorMetodoPago.isEmpty())
    }

    @Test
    fun `venta cobrada despues de medianoche no entra en el reporte del dia anterior`() {
        val inicioDia1 = LocalDateTime.of(2026, 10, 25, 0, 0)
        val finDia1 = LocalDateTime.of(2026, 10, 25, 23, 59, 59)

        // Orden creada a las 23:55 del Dia 1, pero cobrada a las 00:05 del Dia 2
        val orden = Orden(id = 1, total = 5.0, fechaTimestamp = getMillis(LocalDateTime.of(2026, 10, 25, 23, 55)), cajeroUid = "123")
        val pagoMedianoche = Pago(id = 1, ordenId = 1, metodoPago = MetodoPago.EFECTIVO, montoRecibido = 5.0, cambio = 0.0, comprobanteCorrelativo = "001", fechaTimestamp = getMillis(LocalDateTime.of(2026, 10, 26, 0, 5)), cajeroUid = "123")

        val reporteDia1 = reporteService.generarReporte(
            ordenes = listOf(orden),
            pagos = listOf(pagoMedianoche),
            productos = emptyList(),
            fechaInicio = inicioDia1,
            fechaFin = finDia1
        )

        //Como se cobró en el Día 2, el Día 1 no debe tener ingresos
        assertEquals(0.0, reporteDia1.ingresosTotales, 0.0)
    }

    @Test
    fun `top 5 respeta el periodo y ordena correctamente`() {
        val inicio = LocalDateTime.of(2026, 11, 1, 0, 0)
        val fin = LocalDateTime.of(2026, 11, 30, 23, 59)

        val detalleCafe = DetalleOrden(productoId = 1, nombreProducto = "Cafe", precioUnitario = 2.0, cantidad = 10)
        val detallePan = DetalleOrden(productoId = 2, nombreProducto = "Pan", precioUnitario = 1.0, cantidad = 5)

        val ordenPeriodo = Orden(id = 1, items = listOf(detalleCafe, detallePan), total = 25.0, fechaTimestamp = getMillis(LocalDateTime.of(2026, 11, 15, 10, 0)), cajeroUid = "123")
        val pagoPeriodo = Pago(id = 1, ordenId = 1, metodoPago = MetodoPago.EFECTIVO, montoRecibido = 25.0, cambio = 0.0, comprobanteCorrelativo = "001", fechaTimestamp = getMillis(LocalDateTime.of(2026, 11, 15, 10, 5)), cajeroUid = "123")

        val reporte = reporteService.generarReporte(
            ordenes = listOf(ordenPeriodo),
            pagos = listOf(pagoPeriodo),
            productos = emptyList(),
            fechaInicio = inicio,
            fechaFin = fin
        )

        // Verificamos que el Top 5 los ordena por cantidad
        assertEquals(2, reporte.top5Productos.size)
        assertEquals("Cafe", reporte.top5Productos[0].nombre)
        assertEquals(10, reporte.top5Productos[0].cantidad)
        assertEquals("Pan", reporte.top5Productos[1].nombre)
    }
}