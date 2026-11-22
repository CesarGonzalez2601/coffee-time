package com.coffeetime.ui.ordenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeetime.R
import com.coffeetime.domain.model.CartItem
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.EstadoOrden
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.model.User
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdenesScreen(
    viewModel: OrdenesViewModel,
    user: User,
    onNavigateToPagos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(user) {
        viewModel.setUser(user)
    }

    uiState.selectedOrderForDetail?.let { historyItem ->
        OrderDetailDialog(
            item = historyItem,
            onDismiss = { viewModel.selectOrderForDetail(null) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.s4)
    ) {
        // Encabezado
        Text(
            text = stringResource(R.string.nav_ordenes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Space.s3))

        // Pestañas internas: Catálogo vs Carrito vs Historial
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(text = stringResource(R.string.catalogo_titulo)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    BadgedBox(
                        badge = {
                            if (uiState.itemCount > 0) {
                                Badge { Text(text = "${uiState.itemCount}") }
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(Size.iconSmall)
                            )
                            Spacer(modifier = Modifier.width(Space.s2))
                            Text(text = stringResource(R.string.carrito_titulo))
                        }
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
                        Spacer(modifier = Modifier.width(Space.s2))
                        Text(text = stringResource(R.string.tab_historial_ordenes))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Space.s3))

        when (selectedTab) {
            0 -> CatalogView(
                uiState = uiState,
                onCategorySelected = viewModel::onCategorySelected,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onAddToCart = viewModel::addToCart
            )
            1 -> CartView(
                uiState = uiState,
                onUpdateQuantity = viewModel::updateCartQuantity,
                onRemoveItem = viewModel::removeFromCart,
                onClearCart = viewModel::clearCart,
                onConfirmOrder = {
                    viewModel.confirmOrder(cajeroUid = user.uid) {
                        onNavigateToPagos()
                    }
                }
            )
            2 -> OrderHistoryView(
                uiState = uiState,
                onFiltroFechaSelected = viewModel::onFiltroFechaSelected,
                onCajeroUidSelected = viewModel::onCajeroUidSelected,
                onSelectDetail = viewModel::selectOrderForDetail
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderHistoryView(
    uiState: OrdenesUiState,
    onFiltroFechaSelected: (FiltroFecha) -> Unit,
    onCajeroUidSelected: (String?) -> Unit,
    onSelectDetail: (OrderHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCashierMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Filtros de fecha (Chips horizontales)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FiltroFecha.entries.toTypedArray()) { filtro ->
                val selected = uiState.selectedFiltroFecha == filtro
                FilterChip(
                    selected = selected,
                    onClick = { onFiltroFechaSelected(filtro) },
                    label = { Text(text = filtro.label) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(Size.iconSmall)
                            )
                        }
                    } else null
                )
            }
        }

        // Filtro por Cajero (Solo visible si el usuario tiene permiso VIEW_REPORTS por RBAC)
        if (uiState.canFilterByCashier) {
            Spacer(modifier = Modifier.height(Space.s3))

            val selectedCashierName = uiState.allCashiers.firstOrNull { it.first == uiState.selectedCajeroUid }?.second
                ?: stringResource(R.string.todos_los_cajeros)

            ExposedDropdownMenuBox(
                expanded = expandedCashierMenu,
                onExpandedChange = { expandedCashierMenu = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCashierName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.filtro_cajero)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCashierMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedCashierMenu,
                    onDismissRequest = { expandedCashierMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.todos_los_cajeros)) },
                        onClick = {
                            onCajeroUidSelected(null)
                            expandedCashierMenu = false
                        }
                    )
                    uiState.allCashiers.forEach { (uid, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onCajeroUidSelected(uid)
                                expandedCashierMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Space.s3))

        // Lista de Órdenes
        if (uiState.historyOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.sin_ordenes_historial),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Space.s3),
                contentPadding = PaddingValues(bottom = Space.s4),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.historyOrders, key = { it.orden.id }) { item ->
                    OrderHistoryCard(
                        item = item,
                        onClickDetail = { onSelectDetail(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    item: OrderHistoryItem,
    onClickDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val fechaStr = dateFormat.format(Date(item.orden.fechaTimestamp))

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Space.s4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orden #${item.orden.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (item.orden.estado) {
                        EstadoOrden.PENDIENTE -> "PENDIENTE"
                        EstadoOrden.PAGADA -> "PAGADA"
                        EstadoOrden.CANCELADA -> "CANCELADA"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (item.orden.estado) {
                        EstadoOrden.PAGADA -> MaterialTheme.colorScheme.primary
                        EstadoOrden.PENDIENTE -> MaterialTheme.colorScheme.tertiary
                        EstadoOrden.CANCELADA -> MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Space.s1))

            Text(
                text = "${stringResource(R.string.fecha_label)}: $fechaStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${stringResource(R.string.creado_por)}: ${item.cashierName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Space.s2))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Space.s2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${item.orden.items.sumOf { it.cantidad }} ítems",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = String.format(Locale.US, "Total: $%.2f", item.orden.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onClickDetail,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(Size.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(Space.s1))
                    Text(text = stringResource(R.string.ver_detalle))
                }
            }
        }
    }
}

@Composable
private fun OrderDetailDialog(
    item: OrderHistoryItem,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.detalle_orden_titulo, item.orden.id),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Space.s2)
            ) {
                Text(text = "${stringResource(R.string.fecha_label)}: ${dateFormat.format(Date(item.orden.fechaTimestamp))}")
                Text(text = "${stringResource(R.string.creado_por)}: ${item.cashierName}")
                Text(text = "${stringResource(R.string.estado_label)}: ${item.orden.estado}")

                item.pago?.let { p ->
                    Text(text = "${stringResource(R.string.metodo_pago)}: ${p.metodoPago.name}")
                    Text(text = "${stringResource(R.string.correlativo_label)}: ${p.comprobanteCorrelativo}")
                }

                Spacer(modifier = Modifier.height(Space.s2))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Space.s2))

                Text(
                    text = "Productos de la orden:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                item.orden.items.forEach { detalle ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${detalle.cantidad}x ${detalle.nombreProducto}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format(Locale.US, "$%.2f", detalle.subtotalLinea),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space.s2))
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.subtotal), style = MaterialTheme.typography.bodyMedium)
                    Text(text = String.format(Locale.US, "$%.2f", item.orden.subtotal), style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.iva), style = MaterialTheme.typography.bodyMedium)
                    Text(text = String.format(Locale.US, "$%.2f", item.orden.iva), style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = String.format(Locale.US, "$%.2f", item.orden.total),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item.pago?.let { p ->
                    if (p.montoRecibido > 0) {
                        Spacer(modifier = Modifier.height(Space.s1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.monto_recibido), style = MaterialTheme.typography.bodySmall)
                            Text(text = String.format(Locale.US, "$%.2f", p.montoRecibido), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.cambio_a_entregar), style = MaterialTheme.typography.bodySmall)
                            Text(text = String.format(Locale.US, "$%.2f", p.cambio), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.nueva_orden))
            }
        }
    )
}

@Composable
private fun CatalogView(
    uiState: OrdenesUiState,
    onCategorySelected: (CategoriaProducto) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddToCart: (Producto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Barra de búsqueda
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = stringResource(R.string.buscar_producto)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(Space.s3))

        // Filtro de Categorías en chips horizontales
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CategoriaProducto.entries.toTypedArray()) { category ->
                val selected = uiState.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onCategorySelected(category) },
                    label = { Text(text = category.displayName) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(Size.iconSmall)
                            )
                        }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(Space.s3))

        // Grid de Productos
        if (uiState.products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron productos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(Space.s3),
                verticalArrangement = Arrangement.spacedBy(Space.s3),
                contentPadding = PaddingValues(bottom = Space.s4),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onAddToCart = { onAddToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Producto,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(Space.s3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.emoji,
                fontSize = 36.sp,
                modifier = Modifier.padding(vertical = Space.s1)
            )

            Text(
                text = product.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = String.format(Locale.US, "$%.2f", product.precio),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Space.s2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (product.stockActual > 0) {
                        stringResource(R.string.stock_disponible, product.stockActual)
                    } else {
                        stringResource(R.string.sin_stock)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.stockActual > 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                IconButton(
                    onClick = onAddToCart,
                    enabled = product.stockActual > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (product.stockActual > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.agregar),
                        tint = if (product.stockActual > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun CartView(
    uiState: OrdenesUiState,
    onUpdateQuantity: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onConfirmOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.cartItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(Space.s6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s3)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.carrito_vacio),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.itemCount} items en carrito",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = onClearCart,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(Size.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(Space.s1))
                    Text(text = stringResource(R.string.vaciar_carrito))
                }
            }

            Spacer(modifier = Modifier.height(Space.s2))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.s2)
            ) {
                items(uiState.cartItems, key = { it.producto.id }) { item ->
                    CartItemRow(
                        item = item,
                        onUpdateQuantity = { qty -> onUpdateQuantity(item.producto.id, qty) },
                        onRemove = { onRemoveItem(item.producto.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space.s3))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Space.s3))

            // Desglose Financiero
            FinancialSummary(
                subtotal = uiState.subtotal,
                iva = uiState.iva,
                total = uiState.total
            )

            Spacer(modifier = Modifier.height(Space.s4))

            Button(
                onClick = onConfirmOrder,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Size.buttonLarge)
            ) {
                Text(
                    text = stringResource(R.string.confirmar_orden),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
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
            Text(
                text = item.producto.emoji,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(Space.s2))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "$%.2f c/u", item.producto.precio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s1)
            ) {
                IconButton(
                    onClick = { onUpdateQuantity(item.cantidad - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Disminuir",
                        modifier = Modifier.size(Size.iconSmall)
                    )
                }

                Text(
                    text = "${item.cantidad}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = Space.s1)
                )

                IconButton(
                    onClick = { onUpdateQuantity(item.cantidad + 1) },
                    enabled = item.cantidad < item.producto.stockActual,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar",
                        modifier = Modifier.size(Size.iconSmall)
                    )
                }

                Spacer(modifier = Modifier.width(Space.s2))

                Text(
                    text = String.format(Locale.US, "$%.2f", item.subtotalLinea),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Size.iconSmall)
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialSummary(
    subtotal: Double,
    iva: Double,
    total: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.s1)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.subtotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.US, "$%.2f", subtotal),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.iva),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.US, "$%.2f", iva),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(Space.s1))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(Locale.US, "$%.2f", total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
