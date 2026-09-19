package com.coffeetime.util

import com.coffeetime.database.DatabaseManager
import com.coffeetime.exception.PermissionDeniedException
import com.coffeetime.model.CategoriaProducto
import com.coffeetime.model.CierreCaja
import com.coffeetime.model.DetalleOrden
import com.coffeetime.model.EstadoOrden
import com.coffeetime.model.Orden
import com.coffeetime.model.Permission
import com.coffeetime.model.Producto
import com.coffeetime.model.Reporte
import com.coffeetime.repository.ProductoRepositorySQLite
import com.coffeetime.service.CierreCajaService
import com.coffeetime.service.InventarioService
import com.coffeetime.service.OrdenService
import com.coffeetime.service.PagoService
import com.coffeetime.service.ReporteService
import com.coffeetime.service.Session
import java.time.LocalDate
import java.time.format.DateTimeParseException

class MenuConsole(
    private val productoRepository: ProductoRepositorySQLite,
    private val ordenService: OrdenService,
    private val reporteService: ReporteService,
    private val cierreCajaService: CierreCajaService,
    private val inventarioService: InventarioService? = null
) {

    fun mostrarMenu() {
        var continuar = true

        while (continuar) {
            println()
            println("===== COFFEE TIME =====")
            println("1. Inicio")
            println("2. Órdenes (Carrito)")
            println("3. Inventario")
            println("4. Pagos")
            println("5. Historial")
            println("6. Reportes")
            println("7. Cierre de caja")
            println("0. Salir")

            when (leerEntero("Seleccione una opción: ")) {
                1 -> mostrarInicio()
                2 -> menuOrdenes()
                3 -> menuInventario()
                4 -> menuPagos()
                5 -> mostrarHistorial()
                6 -> menuReportes()
                7 -> menuCierreCaja()
                0 -> continuar = false
                else -> println("Opción inválida.")
            }
        }
    }

    private fun mostrarInicio() {
        val metricas = obtenerMetricasDelDia()

        println()
        println("===== INICIO =====")
        println("Órdenes del día: ${metricas.totalOrdenes}")
        println("Ingresos acumulados: $%.2f".format(metricas.ingresos))

        if (metricas.stockBajo.isEmpty()) {
            println("Stock crítico (<= 5): ninguno")
        } else {
            println("Stock crítico (<= 5):")
            metricas.stockBajo.forEach {
                println("- ${it.nombre}: ${it.stock} unidades")
            }
        }
    }

    private fun menuOrdenes() {
        var continuar = true

        while (continuar) {
            println()
            println("===== ÓRDENES =====")
            mostrarCarrito()
            println("1. Agregar producto")
            println("2. Modificar cantidad")
            println("3. Eliminar producto")
            println("4. Ver desglose")
            println("5. Confirmar orden / pasar a pago")
            println("0. Volver")

            when (leerEntero("Seleccione una opción: ")) {
                1 -> agregarAlCarrito()
                2 -> modificarCarrito()
                3 -> eliminarDelCarrito()
                4 -> mostrarTotales(
                    ordenService.carritoActual
                )
                5 -> confirmarOrden()
                0 -> continuar = false
                else -> println("Opción inválida.")
            }
        }
    }

    private fun agregarAlCarrito() {
        mostrarProductos(productoRepository.obtenerProductos())
        val productoId = leerEntero("ID del producto: ")
        val producto = productoRepository.buscarPorId(productoId)

        if (producto == null) {
            println("Producto no encontrado.")
            return
        }

        val cantidad = leerEntero("Cantidad: ")
        if (ordenService.agregarProductoAlCarrito(producto, cantidad)) {
            println("Producto agregado al carrito.")
        } else {
            println("No hay stock suficiente o la cantidad no es válida.")
        }
    }

    private fun modificarCarrito() {
        val productoId = leerEntero("ID del producto en el carrito: ")
        val nuevaCantidad = leerEntero("Nueva cantidad: ")

        if (ordenService.modificarCantidadEnCarrito(
                productoId,
                nuevaCantidad
            )
        ) {
            println("Cantidad actualizada.")
        } else {
            println("No se pudo actualizar la cantidad.")
        }
    }

    private fun eliminarDelCarrito() {
        val productoId = leerEntero("ID del producto en el carrito: ")

        if (ordenService.eliminarDelCarrito(productoId)) {
            println("Producto eliminado del carrito.")
        } else {
            println("El producto no está en el carrito.")
        }
    }

    private fun confirmarOrden() {
        val carrito = ordenService.carritoActual

        if (carrito.detalles.isEmpty()) {
            println("El carrito está vacío.")
            return
        }

        val ordenConfirmada = ordenService.confirmarOrden(carrito)

        if (ordenConfirmada == null) {
            println("No se pudo confirmar la orden.")
            return
        }

        println(
            "Orden #${ordenConfirmada.id} confirmada y lista " +
                    "para procesar el pago."
        )
        mostrarTotales(ordenConfirmada)
        procesarPago(ordenConfirmada)
    }

    private fun menuInventario() {
        var continuar = true

        while (continuar) {
            println()
            println("===== GESTIÓN DE INVENTARIO =====")
            println("1. Listar productos por categoría")
            println("2. Ver productos con Stock Crítico (<= 5)")
            println("3. Reabastecer stock (Entrada)")
            println("4. Disminuir stock (Salida)")
            println("5. Ajuste manual de stock")
            println("6. Agregar nuevo producto")
            println("7. Eliminar producto")
            println("0. Volver")

            when (leerEntero("Seleccione una opción: ")) {
                1 -> listarPorCategoria()
                2 -> verStockCritico()
                3 -> reabastecerStock()
                4 -> disminuirStock()
                5 -> ajusteManualStock()
                6 -> agregarProducto()
                7 -> eliminarProducto()
                0 -> continuar = false
                else -> println("Opción inválida.")
            }
        }
    }

    private fun verStockCritico() {
        val criticos = inventarioService?.obtenerProductosCriticos()
            ?: productoRepository.obtenerProductos().filter { it.stock <= 5 }

        println()
        println("--- PRODUCTOS EN ESTADO CRÍTICO (<= 5) ---")
        if (criticos.isEmpty()) {
            println("No hay productos con stock crítico.")
        } else {
            criticos.forEach {
                println("ID: ${it.id} | ${it.nombre} | Stock actual: ${it.stock} unidades")
            }
        }
    }

    private fun reabastecerStock() {
        mostrarProductos(productoRepository.obtenerProductos())
        val id = leerEntero("ID del producto a reabastecer: ")
        val cantidad = leerEntero("Cantidad a agregar: ")
        val motivo = leerTexto("Motivo (ej. Compra a proveedor): ")

        val exito = if (inventarioService != null) {
            inventarioService.aumentarStock(id, cantidad, motivo)
        } else {
            productoRepository.incrementarStock(id, cantidad)
        }

        if (exito) {
            println("Stock reabastecido con éxito.")
        } else {
            println("Error al reabastecer el stock.")
        }
    }

    private fun disminuirStock() {
        mostrarProductos(productoRepository.obtenerProductos())
        val id = leerEntero("ID del producto a descontar: ")
        val cantidad = leerEntero("Cantidad a descontar: ")
        val motivo = leerTexto("Motivo (ej. Merma / Producto dañado): ")

        val exito = if (inventarioService != null) {
            inventarioService.disminuirStock(id, cantidad, motivo)
        } else {
            productoRepository.reducirStock(id, cantidad)
        }

        if (exito) {
            println("Stock descontado con éxito.")
        } else {
            println("Error: cantidad no válida o stock insuficiente.")
        }
    }

    private fun ajusteManualStock() {
        mostrarProductos(productoRepository.obtenerProductos())
        val id = leerEntero("ID del producto a ajustar: ")
        val nuevoStock = leerEntero("Nuevo stock total: ")
        val motivo = leerTexto("Motivo del ajuste de inventario: ")

        val exito = if (inventarioService != null) {
            inventarioService.ajusteManual(id, nuevoStock, motivo)
        } else {
            val prod = productoRepository.buscarPorId(id)
            if (prod != null) productoRepository.actualizarProducto(id, prod.precio, nuevoStock) else false
        }

        if (exito) {
            println("Ajuste manual aplicado correctamente.")
        } else {
            println("Error al aplicar el ajuste manual.")
        }
    }

    private fun listarPorCategoria() {
        val categoria = leerCategoria()
        val productos = productoRepository.obtenerProductos()
            .filter { it.categoria == categoria }

        if (productos.isEmpty()) {
            println("No hay productos en esa categoría.")
        } else {
            mostrarProductos(productos)
        }
    }

    private fun agregarProducto() {
        val nombre = leerTexto("Nombre: ")
        val precio = leerDouble("Precio: ")
        val categoria = leerCategoria()
        val stock = leerEntero("Stock: ")

        val producto = productoRepository.agregarProducto(
            nombre = nombre,
            precio = precio,
            categoria = categoria,
            stock = stock
        )

        println("Producto creado con ID ${producto.id}.")
    }

    private fun eliminarProducto() {
        mostrarProductos(productoRepository.obtenerProductos())
        val id = leerEntero("ID del producto a eliminar: ")

        val productoExiste = productoRepository.buscarPorId(id)
        if (productoExiste == null) {
            println("Producto no encontrado.")
            return
        }

        if (productoRepository.eliminarProducto(id)) {
            println("Producto eliminado correctamente.")
        } else {
            println("No se pudo eliminar el producto (puede tener historial de ventas o movimientos asociados).")
        }
    }

    private fun menuPagos() {
        val ordenes = ordenService.obtenerOrdenesConfirmadas()

        if (ordenes.isEmpty()) {
            println("No hay órdenes confirmadas para cobrar.")
            return
        }

        println()
        println("===== PAGOS =====")
        ordenes.forEach { orden ->
            println(
                "Orden #${orden.id} - Total: " +
                        "$%.2f".format(orden.total)
            )
        }

        val ordenId = leerEntero(
            "ID de la orden a procesar (0 para volver): "
        )

        if (ordenId == 0) {
            return
        }

        val orden = ordenes.find { it.id == ordenId }

        if (orden == null) {
            println("No existe una orden confirmada con ese ID.")
            return
        }

        mostrarTotales(orden)
        println("1. Procesar pago")
        println("2. Cancelar orden")
        println("0. Volver")

        when (leerEntero("Seleccione una opción: ")) {
            1 -> procesarPago(orden)
            2 -> cancelarOrden(orden)
            0 -> return
            else -> println("Opción inválida.")
        }
    }

    private fun procesarPago(orden: Orden) {
        if (!confirmarCobroEnJornadaCerrada(orden)) {
            return
        }

        println()
        println("===== PAGO DE ORDEN #${orden.id} =====")
        println("Seleccione el método de pago:")
        println("1. Efectivo")
        println("2. Tarjeta")
        println("0. Pagar más tarde (Guardar orden)")
        val opcion = leerEntero("Opción: ")

        if (opcion == 0) {
            println("Orden #${orden.id} guardada. Puede cobrarla después desde el menú 'Pagos'.")
            return
        }

        val metodoPago = when (opcion) {
            1 -> {
                val monto = leerDouble("Monto recibido: $")
                com.coffeetime.model.PagoEfectivo(monto)
            }
            2 -> {
                val numero = leerTexto("Número de tarjeta (16 dígitos): ")
                val titular = leerTexto("Nombre del titular: ")
                com.coffeetime.model.PagoTarjeta(numero, titular)
            }
            else -> {
                println("Opción inválida. Orden #${orden.id} guardada para cobrar más tarde.")
                return
            }
        }

        val pago = com.coffeetime.model.Pago(
            ordenId = orden.id,
            totalPagado = orden.total,
            metodoPago = metodoPago
        )

        val pagoService = PagoService(ordenService, inventarioService)

        if (pagoService.registrarPago(pago, orden)) {
            println("Pago procesado exitosamente")
            println("Correlativo de transacción: ${pago.correlativo}")
            println(metodoPago.obtenerDetalle())
        } else {
            println("Transacción rechazada. Revise los datos del pago e intente nuevamente.")
            println("La orden #${orden.id} queda pendiente de cobro en el menú 'Pagos'.")
        }
    }

    /**
     * El cierre guarda un snapshot de la jornada y cierres_caja.fecha es UNIQUE: un cobro
     * posterior al cierre no entra en el cierre ya guardado ni permite volver a cerrar el dia.
     */
    private fun confirmarCobroEnJornadaCerrada(orden: Orden): Boolean {
        val fecha = cierreCajaService.fechaDeHoy()
        val cierre = cierreCajaService.buscarPorFecha(fecha) ?: return true

        println()
        println("[AVISO] La jornada $fecha ya fue cerrada (cierre #${cierre.id}).")
        println("Este cobro no quedará reflejado en ese cierre y la jornada no se puede")
        println("volver a cerrar.")

        if (leerConfirmacion("¿Cobrar de todas formas? (s/n): ")) {
            return true
        }

        println("Cobro cancelado. La orden #${orden.id} queda pendiente en el menú 'Pagos'.")
        return false
    }

    private fun cancelarOrden(orden: Orden) {
        if (!leerConfirmacion("¿Cancelar esta orden? (s/n): ")) {
            return
        }

        if (ordenService.cancelarOrden(orden)) {
            println("Orden cancelada y stock restaurado.")
        } else {
            println("No se pudo cancelar la orden.")
        }
    }

    private fun mostrarHistorial() {
        val ordenes = ordenService.obtenerHistorialOrdenes()

        println()
        println("===== HISTORIAL =====")

        if (ordenes.isEmpty()) {
            println("No hay órdenes pagadas.")
            return
        }

        ordenes.forEach { orden ->
            println("Orden #${orden.id} - ${orden.estado}")
            mostrarTotales(orden)
        }
    }

    private fun menuReportes() {
        if (!validarPermiso(Permission.VIEW_REPORTS)) {
            return
        }

        var continuar = true

        while (continuar) {
            println()
            println("===== REPORTES =====")
            println("1. Reporte de la jornada de hoy")
            println("2. Reporte por rango de fechas")
            println("0. Volver")

            when (leerEntero("Seleccione una opción: ")) {
                1 -> mostrarReporte(reporteService.generarReporteDelDia())
                2 -> reportePorRango()
                0 -> continuar = false
                else -> println("Opción inválida.")
            }
        }
    }

    private fun reportePorRango() {
        val desde = leerFecha("Fecha desde (yyyy-MM-dd): ")
        val hasta = leerFecha("Fecha hasta (yyyy-MM-dd): ")

        if (desde > hasta) {
            println("La fecha inicial no puede ser posterior a la final.")
            return
        }

        mostrarReporte(
            reporteService.generarReporte(
                desde = desde,
                hasta = hasta
            )
        )
    }

    private fun mostrarReporte(reporte: Reporte) {
        println()
        println("===== REPORTE DE VENTAS =====")
        println("Periodo: ${reporte.desde} a ${reporte.hasta}")

        if (reporte.ordenesCerradas == 0) {
            println("No hay ventas registradas en el periodo.")
            return
        }

        println("Ingresos totales (con IVA): $%.2f".format(reporte.ingresosTotales))
        println("Órdenes cerradas: ${reporte.ordenesCerradas}")
        println("Ticket promedio: $%.2f".format(reporte.ticketPromedio))

        println()
        println("--- Productos más vendidos ---")
        reporte.productosMasVendidos.forEachIndexed { indice, producto ->
            println(
                "%d. %-20s %4d u. $%9.2f".format(
                    indice + 1,
                    producto.nombre,
                    producto.unidades,
                    producto.total
                )
            )
        }

        println()
        println("--- Ventas por categoría (sin IVA) ---")
        reporte.ventasPorCategoria.forEach { venta ->
            println(
                "%-12s %4d u. $%9.2f".format(
                    venta.categoria.name,
                    venta.unidades,
                    venta.total
                )
            )
        }

        println()
        println("--- Ventas por método de pago ---")
        reporte.ventasPorMetodoPago.forEach { venta ->
            println(
                "%-12s %4d %-8s $%9.2f".format(
                    venta.metodoPago,
                    venta.cantidadOrdenes,
                    if (venta.cantidadOrdenes == 1) "orden" else "órdenes",
                    venta.total
                )
            )
        }
    }

    private fun menuCierreCaja() {
        if (!validarPermiso(Permission.CLOSE_CASH_REGISTER)) {
            return
        }

        var continuar = true

        while (continuar) {
            println()
            println("===== CIERRE DE CAJA =====")
            println("1. Realizar el cierre de la jornada de hoy")
            println("2. Consultar cierres anteriores")
            println("0. Volver")

            when (leerEntero("Seleccione una opción: ")) {
                1 -> realizarCierre()
                2 -> consultarCierres()
                0 -> continuar = false
                else -> println("Opción inválida.")
            }
        }
    }

    private fun realizarCierre() {
        val fecha = cierreCajaService.fechaDeHoy()
        val existente = cierreCajaService.buscarPorFecha(fecha)

        if (existente != null) {
            println("La jornada $fecha ya fue cerrada.")
            mostrarCierre(existente)
            return
        }

        val usuarioId = Session.getCurrentUser()?.id ?: 0

        val previo = cierreCajaService.previsualizarCierre(
            fecha = fecha,
            usuarioId = usuarioId
        )

        println()
        println("--- VENTAS DE LA JORNADA $fecha ---")
        println("Órdenes cobradas: ${previo.cantidadOrdenes}")
        println("Ventas totales: $%.2f".format(previo.totalVentas))
        println("Pagos en efectivo: $%.2f".format(previo.totalEfectivo))
        println("Pagos con tarjeta: $%.2f".format(previo.totalTarjeta))
        println("Efectivo esperado en caja: $%.2f".format(previo.efectivoEsperado))

        val contado = leerMontoNoNegativo("Efectivo físico contado: $")
        val cierre = previo.copy(efectivoContado = contado)

        println()
        println("Diferencia: ${formatearDiferencia(cierre.diferencia)}")
        println("Resultado: ${cierre.estado}")

        if (!leerConfirmacion("¿Guardar el cierre? (s/n): ")) {
            println("Cierre descartado.")
            return
        }

        val guardado = cierreCajaService.registrarCierre(
            fecha = fecha,
            usuarioId = usuarioId,
            efectivoContado = contado
        )

        if (guardado == null) {
            println("No se pudo guardar el cierre de caja.")
            return
        }

        println("Cierre de caja registrado.")
        mostrarCierre(guardado)
    }

    private fun consultarCierres() {
        val cierres = cierreCajaService.obtenerCierres()

        println()
        println("===== CIERRES ANTERIORES =====")

        if (cierres.isEmpty()) {
            println("No hay cierres de caja registrados.")
            return
        }

        cierres.forEach { mostrarCierre(it) }
    }

    private fun mostrarCierre(cierre: CierreCaja) {
        println()
        println("--- CIERRE #${cierre.id} | Jornada ${cierre.fecha} ---")
        println("Registrado: ${cierre.fechaHora ?: "-"} | Usuario: ${cierre.usuarioId}")
        println("Órdenes cobradas: ${cierre.cantidadOrdenes}")
        println("Ventas totales: $%.2f".format(cierre.totalVentas))
        println(
            "Efectivo: $%.2f | Tarjeta: $%.2f".format(
                cierre.totalEfectivo,
                cierre.totalTarjeta
            )
        )
        println(
            "Esperado: $%.2f | Contado: $%.2f".format(
                cierre.efectivoEsperado,
                cierre.efectivoContado
            )
        )
        println("Diferencia: ${formatearDiferencia(cierre.diferencia)}")
        println("Estado: ${cierre.estado}")
    }

    private fun mostrarCarrito() {
        if (ordenService.carritoActual.detalles.isEmpty()) {
            println("Carrito vacío.")
            return
        }

        ordenService.carritoActual.detalles.forEach {
            println(
                "${it.producto.id}. ${it.producto.nombre} " +
                        "x${it.cantidad} = $%.2f".format(it.subtotalItem)
            )
        }
    }

    private fun mostrarProductos(productos: List<Producto>) {
        productos.forEach {
            println(
                "${it.id}. ${it.nombre} | ${it.categoria} | " +
                        "$%.2f | stock: ${it.stock}".format(it.precio)
            )
        }
    }

    private fun mostrarTotales(orden: Orden) {
        println("Subtotal: $%.2f".format(orden.subtotal))
        println("IVA (10%%): $%.2f".format(orden.impuesto))
        println("Total: $%.2f".format(orden.total))
    }

    private fun leerCategoria(): CategoriaProducto {
        while (true) {
            println(CategoriaProducto.entries.joinToString(", "))
            val valor = leerTexto("Categoría: ").uppercase()

            try {
                return CategoriaProducto.valueOf(valor)
            } catch (_: IllegalArgumentException) {
                println("Categoría inválida.")
            }
        }
    }

    private fun leerEntero(mensaje: String): Int {
        while (true) {
            try {
                return leerTexto(mensaje).toInt()
            } catch (_: NumberFormatException) {
                println("Ingrese un número entero válido.")
            }
        }
    }

    private fun leerDouble(mensaje: String): Double {
        while (true) {
            try {
                return leerTexto(mensaje).replace(',', '.').toDouble()
            } catch (_: NumberFormatException) {
                println("Ingrese un número válido.")
            }
        }
    }

    private fun leerTexto(mensaje: String): String {
        while (true) {
            print(mensaje)
            val valor = readln().trim()

            if (valor.isNotEmpty()) {
                return valor
            }

            println("El valor no puede estar vacío.")
        }
    }

    private fun leerConfirmacion(mensaje: String): Boolean {
        while (true) {
            val respuesta = leerTexto(mensaje).lowercase()

            when (respuesta) {
                "s", "si", "sí" -> return true
                "n", "no" -> return false
                else -> println("Responda s/n.")
            }
        }
    }

    private fun formatearDiferencia(diferencia: Double): String {
        return if (diferencia < 0) {
            "-$%.2f".format(-diferencia)
        } else {
            "$%.2f".format(diferencia)
        }
    }

    private fun leerFecha(mensaje: String): String {
        while (true) {
            try {
                return LocalDate.parse(leerTexto(mensaje)).toString()
            } catch (_: DateTimeParseException) {
                println("Ingrese una fecha válida con formato yyyy-MM-dd.")
            }
        }
    }

    private fun leerMontoNoNegativo(mensaje: String): Double {
        while (true) {
            val monto = leerDouble(mensaje)

            if (monto >= 0) {
                return monto
            }

            println("El monto no puede ser negativo.")
        }
    }

    private fun validarPermiso(permission: Permission): Boolean {
        return try {
            Session.validatePermission(permission)
            true
        } catch (exception: PermissionDeniedException) {
            println("Acceso denegado: ${exception.message}")
            false
        }
    }

    private fun obtenerMetricasDelDia(): MetricasDelDia {
        val sql = """
            SELECT COUNT(*) AS total_ordenes,
                   COALESCE(SUM(total), 0.0) AS ingresos
            FROM ordenes
            WHERE estado = ? AND date(fecha, 'localtime') =
                  date('now', 'localtime')
        """.trimIndent()

        var totalOrdenes = 0
        var ingresos = 0.0

        DatabaseManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, EstadoOrden.PAGADA.name)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        totalOrdenes = result.getInt("total_ordenes")
                        ingresos = result.getDouble("ingresos")
                    }
                }
            }
        }

        val stockBajo = inventarioService?.obtenerProductosCriticos()
            ?: productoRepository.obtenerProductos().filter { it.stock <= 5 }

        return MetricasDelDia(
            totalOrdenes = totalOrdenes,
            ingresos = ingresos,
            stockBajo = stockBajo
        )
    }

    private data class MetricasDelDia(
        val totalOrdenes: Int,
        val ingresos: Double,
        val stockBajo: List<Producto>
    )
}