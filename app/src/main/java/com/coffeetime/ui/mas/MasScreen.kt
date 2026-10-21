package com.coffeetime.ui.mas

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeetime.R
import com.coffeetime.domain.model.CierreCaja
import com.coffeetime.domain.model.User
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MasScreen(
    viewModel: ReportesViewModel,
    user: User,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val successMsg = uiState.cierreSuccessMessage
    LaunchedEffect(successMsg) {
        if (successMsg != null) {
            snackbarHostState.showSnackbar(successMsg)
            viewModel.dismissSuccessMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.s4)
    ) {
        // Encabezado
        Text(
            text = stringResource(R.string.cierre_caja_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Space.s3))

        // Pestañas: Cierre de Caja vs Reportes vs Historial Cierres
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(Space.s1))
                        Text(text = stringResource(R.string.tab_cierre))
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(Space.s1))
                        Text(text = stringResource(R.string.tab_reportes))
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(Space.s1))
                        Text(text = stringResource(R.string.tab_historial_cierres))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Space.s3))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> ArqueoCierreView(
                    uiState = uiState,
                    onMontoInicialChanged = viewModel::onMontoInicialChange,
                    onEfectivoReportadoChanged = viewModel::onEfectivoReportadoChange,
                    onObservacionesChanged = viewModel::onObservacionesChange,
                    onRealizarCierre = {
                        viewModel.realizarCierreCaja(cajeroId = user.id, cashierName = user.name)
                    }
                )
                1 -> ReportesVentasView(uiState = uiState)
                2 -> HistorialCierresView(cierres = uiState.historialCierres)
            }
        }

        Spacer(modifier = Modifier.height(Space.s3))

        // Botón permanente de Cerrar Sesión al final de la pantalla Más
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(Size.button)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                modifier = Modifier.size(Size.iconSmall)
            )
            Spacer(modifier = Modifier.size(Space.s2))
            Text(text = stringResource(R.string.action_logout))
        }
    }
}

@Composable
private fun ArqueoCierreView(
    uiState: ReportesUiState,
    onMontoInicialChanged: (String) -> Unit,
    onEfectivoReportadoChanged: (String) -> Unit,
    onObservacionesChanged: (String) -> Unit,
    onRealizarCierre: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Space.s3)
    ) {
        // Resumen General de la Jornada
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(Space.s4),
                    verticalArrangement = Arrangement.spacedBy(Space.s2)
                ) {
                    Text(
                        text = stringResource(R.string.resumen_jornada),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = String.format(Locale.US, "$%.2f", uiState.totalIngresado),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Money,
                                    contentDescription = null,
                                    modifier = Modifier.size(Size.iconSmall),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(Space.s1))
                                Text(
                                    text = stringResource(R.string.ventas_efectivo),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "$%.2f", uiState.totalEfectivo),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(Size.iconSmall),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(Space.s1))
                                Text(
                                    text = stringResource(R.string.ventas_tarjeta),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "$%.2f", uiState.totalTarjeta),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Formulario de Arqueo
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Space.s4),
                    verticalArrangement = Arrangement.spacedBy(Space.s3)
                ) {
                    Text(
                        text = "Arqueo de Caja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = uiState.montoInicialText,
                        onValueChange = onMontoInicialChanged,
                        label = { Text(text = stringResource(R.string.monto_inicial_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Space.s3),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.efectivo_esperado_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", uiState.efectivoEsperado),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.efectivoReportadoText,
                        onValueChange = onEfectivoReportadoChanged,
                        label = { Text(text = stringResource(R.string.efectivo_reportado_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Estado del Cuadre / Diferencia
                    if (uiState.efectivoReportadoText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                uiState.diferencia == 0.0 -> MaterialTheme.colorScheme.tertiaryContainer
                                uiState.diferencia > 0.0 -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(Space.s3),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.diferencia_label),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = when {
                                        uiState.diferencia == 0.0 -> stringResource(R.string.cuadre_perfecto)
                                        uiState.diferencia > 0.0 -> stringResource(R.string.sobrante_label, uiState.diferencia)
                                        else -> stringResource(R.string.faltante_label, kotlin.math.abs(uiState.diferencia))
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.observacionesText,
                        onValueChange = onObservacionesChanged,
                        label = { Text(text = stringResource(R.string.observaciones_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Space.s1))

                    Button(
                        onClick = onRealizarCierre,
                        enabled = uiState.canRealizarCierre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Size.buttonLarge)
                    ) {
                        Text(
                            text = stringResource(R.string.realizar_cierre_btn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportesVentasView(
    uiState: ReportesUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Space.s3)
    ) {
        // Tarjetas de Ventas por Período
        item {
            Text(
                text = stringResource(R.string.reporte_periodos),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Space.s2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s2)
            ) {
                PeriodCard(
                    title = stringResource(R.string.ventas_dia),
                    amount = uiState.ventasHoy,
                    modifier = Modifier.weight(1f)
                )

                PeriodCard(
                    title = stringResource(R.string.ventas_semana),
                    amount = uiState.ventasSemana,
                    modifier = Modifier.weight(1f)
                )

                PeriodCard(
                    title = stringResource(R.string.ventas_mes),
                    amount = uiState.ventasMes,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top 5 Productos Más Vendidos
        item {
            Spacer(modifier = Modifier.height(Space.s2))

            Text(
                text = stringResource(R.string.top_productos_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Space.s2))

            if (uiState.top5Productos.isEmpty()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.sin_ventas_reporte),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Space.s4),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    uiState.top5Productos.forEach { topItem ->
                        TopProductCard(item = topItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(Space.s3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Space.s1))

            Text(
                text = String.format(Locale.US, "$%.2f", amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopProductCard(
    item: TopProductItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.ranking}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(Space.s2))

                Text(text = item.emoji, fontSize = 24.sp)

                Spacer(modifier = Modifier.width(Space.s2))

                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%d unidades", item.cantidadVendida),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(Locale.US, "$%.2f", item.totalMonto),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HistorialCierresView(
    cierres: List<CierreCaja>,
    modifier: Modifier = Modifier
) {
    if (cierres.isEmpty()) {
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
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.sin_cierres_historial),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Space.s2)
        ) {
            items(cierres, key = { it.id }) { cierre ->
                CierreCard(cierre = cierre)
            }
        }
    }
}

@Composable
private fun CierreCard(
    cierre: CierreCaja,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(cierre.fechaTimestamp) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US)
        Instant.ofEpochMilli(cierre.fechaTimestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
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
                Text(
                    text = "Cierre #${cierre.id} • $dateStr",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = String.format(Locale.US, "$%.2f", cierre.totalIngresado),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Cajero: ${cierre.cashierName} • ${cierre.totalOrdenes} órdenes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Efectivo: $%.2f | Tarjeta: $%.2f", cierre.totalEfectivo, cierre.totalTarjeta),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = when {
                        cierre.diferencia == 0.0 -> "Cuadre: $0.00"
                        cierre.diferencia > 0.0 -> String.format(Locale.US, "Sobrante: +$%.2f", cierre.diferencia)
                        else -> String.format(Locale.US, "Faltante: -$%.2f", kotlin.math.abs(cierre.diferencia))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        cierre.diferencia == 0.0 -> MaterialTheme.colorScheme.tertiary
                        cierre.diferencia > 0.0 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            if (cierre.observaciones.isNotBlank()) {
                Text(
                    text = "Obs: ${cierre.observaciones}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
