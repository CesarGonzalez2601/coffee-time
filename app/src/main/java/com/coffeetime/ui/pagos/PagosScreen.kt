package com.coffeetime.ui.pagos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coffeetime.R
import com.coffeetime.domain.model.MetodoPago
import com.coffeetime.domain.model.Orden
import com.coffeetime.domain.model.User
import com.coffeetime.ui.ordenes.FinancialSummary
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PagosScreen(
    viewModel: PagosViewModel,
    user: User,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    if (uiState.completedReceipt != null) {
        ReceiptDialog(
            receipt = uiState.completedReceipt!!,
            onDismiss = viewModel::dismissReceipt
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.s4)
    ) {
        // Encabezado
        Text(
            text = stringResource(R.string.pagos_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Space.s3))

        // Pestañas: Pendientes vs Historial de Pagos
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    BadgedBox(
                        badge = {
                            if (uiState.pendingOrders.isNotEmpty()) {
                                Badge { Text(text = "${uiState.pendingOrders.size}") }
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(Size.iconSmall)
                            )
                            Spacer(modifier = Modifier.width(Space.s2))
                            Text(text = stringResource(R.string.tab_pendientes))
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(Space.s2))
                        Text(text = stringResource(R.string.tab_historial_pagos))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Space.s3))

        if (selectedTab == 0) {
            PendingPaymentsView(
                uiState = uiState,
                onSelectOrder = viewModel::selectOrder,
                onPaymentMethodSelected = viewModel::setPaymentMethod,
                onAmountReceivedChanged = viewModel::onAmountReceivedChange,
                onQuickCash = viewModel::addQuickCashAmount,
                onProcessPayment = {
                    viewModel.processPayment(cajeroId = user.id, cashierName = user.name)
                }
            )
        } else {
            PaidOrdersHistoryView(
                uiState = uiState,
                onViewReceipt = { record ->
                    viewModel.viewReceiptForPaidOrder(record, cashierName = user.name)
                }
            )
        }
    }
}

@Composable
private fun PendingPaymentsView(
    uiState: PagosUiState,
    onSelectOrder: (Orden) -> Unit,
    onPaymentMethodSelected: (MetodoPago) -> Unit,
    onAmountReceivedChanged: (String) -> Unit,
    onQuickCash: (Double) -> Unit,
    onProcessPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.pendingOrders.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Space.s6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s3)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.sin_ordenes_pendientes),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Space.s3)
        ) {
            item {
                Text(
                    text = "Órdenes pendientes (${uiState.pendingOrders.size}):",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(Space.s2))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.pendingOrders, key = { it.id }) { order ->
                        val isSelected = uiState.selectedOrder?.id == order.id
                        AssistChip(
                            onClick = { onSelectOrder(order) },
                            label = {
                                Text(
                                    text = String.format(Locale.US, "Orden #%d ($%.2f)", order.id, order.total),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(Size.iconSmall)
                                )
                            }
                        )
                    }
                }
            }

            uiState.selectedOrder?.let { order ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(Space.s4)) {
                            Text(
                                text = "Detalle de Orden #${order.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(Space.s2))

                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.cantidad}x ${item.nombreProducto}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%.2f", item.subtotalLinea),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Space.s2))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(Space.s2))

                            FinancialSummary(
                                subtotal = order.subtotal,
                                iva = order.iva,
                                total = order.total
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.metodo_pago),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(Space.s2))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        MetodoPago.entries.forEachIndexed { index, method ->
                            SegmentedButton(
                                selected = uiState.paymentMethod == method,
                                onClick = { onPaymentMethodSelected(method) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = MetodoPago.entries.size),
                                icon = {
                                    Icon(
                                        imageVector = if (method == MetodoPago.EFECTIVO) Icons.Default.Money else Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(Size.iconSmall)
                                    )
                                }
                            ) {
                                Text(text = method.displayName)
                            }
                        }
                    }
                }

                if (uiState.paymentMethod == MetodoPago.EFECTIVO) {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(Space.s4),
                                verticalArrangement = Arrangement.spacedBy(Space.s3)
                            ) {
                                Text(
                                    text = "Calculadora de Cambio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OutlinedTextField(
                                    value = uiState.amountReceivedText,
                                    onValueChange = onAmountReceivedChanged,
                                    label = { Text(text = stringResource(R.string.monto_recibido)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Space.s2)
                                ) {
                                    AssistChip(
                                        onClick = { onQuickCash(-1.0) },
                                        label = { Text(text = "Exacto") }
                                    )
                                    AssistChip(
                                        onClick = { onQuickCash(5.0) },
                                        label = { Text(text = "$5") }
                                    )
                                    AssistChip(
                                        onClick = { onQuickCash(10.0) },
                                        label = { Text(text = "$10") }
                                    )
                                    AssistChip(
                                        onClick = { onQuickCash(20.0) },
                                        label = { Text(text = "$20") }
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.cambio_a_entregar),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = String.format(Locale.US, "$%.2f", uiState.cambio),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (uiState.amountReceived < order.total && uiState.amountReceived > 0) {
                                    Text(
                                        text = stringResource(R.string.pago_insuficiente, order.total),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(Space.s4),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Space.s2)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.tarjeta_aprobada),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Space.s2))

                    Button(
                        onClick = onProcessPayment,
                        enabled = uiState.canProcessPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Size.buttonLarge)
                    ) {
                        Text(
                            text = stringResource(R.string.procesar_pago),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(Space.s6))
                }
            }
        }
    }
}

@Composable
private fun PaidOrdersHistoryView(
    uiState: PagosUiState,
    onViewReceipt: (PaidOrderRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.paidOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Space.s6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s3)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.historial_pagos_vacio),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Resumen de Ventas Cobradas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space.s4),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ventas Totales Cobradas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format(Locale.US, "$%.2f", uiState.totalRecaudado),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    AssistChip(
                        onClick = { },
                        label = { Text(text = "${uiState.paidOrders.size} cobradas") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space.s3))

            // Lista de Órdenes Pagadas
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Space.s2)
            ) {
                items(uiState.paidOrders, key = { it.payment.id }) { record ->
                    PaidOrderCard(
                        record = record,
                        onViewReceipt = { onViewReceipt(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaidOrderCard(
    record: PaidOrderRecord,
    onViewReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(record.payment.fechaTimestamp) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US)
        Instant.ofEpochMilli(record.payment.fechaTimestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    val itemsSummary = remember(record.order.items) {
        record.order.items.joinToString(", ") { "${it.cantidad}x ${it.nombreProducto}" }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Size.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(Space.s1))
                    Text(
                        text = record.payment.comprobanteCorrelativo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = String.format(Locale.US, "$%.2f", record.order.total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr • ${record.payment.metodoPago.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = stringResource(R.string.ver_comprobante))
                }
            }
        }
    }
}

@Composable
private fun ReceiptDialog(
    receipt: CompletedReceiptData,
    onDismiss: () -> Unit
) {
    val dateStr = remember(receipt.timestamp) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US)
        Instant.ofEpochMilli(receipt.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Space.s6),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(Space.s2))

                Text(
                    text = stringResource(R.string.comprobante_titulo),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = receipt.correlative,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(Space.s3))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Space.s3))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Space.s1)
                ) {
                    ReceiptRow(label = stringResource(R.string.fecha_label), value = dateStr)
                    ReceiptRow(label = stringResource(R.string.cajero_label), value = receipt.cashierName)
                    ReceiptRow(label = stringResource(R.string.metodo_pago), value = receipt.paymentMethod.displayName)
                }

                Spacer(modifier = Modifier.height(Space.s3))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Space.s3))

                // Detalle de Ítems
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Space.s1)
                ) {
                    receipt.order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.cantidad}x ${item.nombreProducto}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", item.subtotalLinea),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space.s3))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Space.s3))

                FinancialSummary(
                    subtotal = receipt.order.subtotal,
                    iva = receipt.order.iva,
                    total = receipt.order.total
                )

                if (receipt.paymentMethod == MetodoPago.EFECTIVO) {
                    Spacer(modifier = Modifier.height(Space.s2))
                    ReceiptRow(
                        label = stringResource(R.string.monto_recibido),
                        value = String.format(Locale.US, "$%.2f", receipt.amountReceived)
                    )
                    ReceiptRow(
                        label = stringResource(R.string.cambio_a_entregar),
                        value = String.format(Locale.US, "$%.2f", receipt.cambio)
                    )
                }

                Spacer(modifier = Modifier.height(Space.s4))

                Text(
                    text = stringResource(R.string.gracias_compra),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Space.s4))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Size.button)
                ) {
                    Text(text = stringResource(R.string.nueva_orden))
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
