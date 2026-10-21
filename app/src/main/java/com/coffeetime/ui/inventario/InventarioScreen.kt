package com.coffeetime.ui.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coffeetime.R
import com.coffeetime.domain.model.CategoriaProducto
import com.coffeetime.domain.model.MovimientoInventario
import com.coffeetime.domain.model.Producto
import com.coffeetime.domain.model.TipoMovimiento
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EMOJI_OPTIONS = listOf("☕", "🥛", "🧊", "🧋", "🥤", "🥐", "🧁", "🍰", "🥪", "🥙", "🍪", "🧃")

@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    if (uiState.adjustmentFormState != null) {
        StockAdjustmentDialog(
            form = uiState.adjustmentFormState!!,
            onUpdateForm = viewModel::updateAdjustmentForm,
            onSubmit = viewModel::submitStockAdjustment,
            onDismiss = viewModel::closeAdjustmentDialog
        )
    }

    if (uiState.newProductFormState != null) {
        NewProductDialog(
            form = uiState.newProductFormState!!,
            onUpdateForm = viewModel::updateNewProductForm,
            onSubmit = viewModel::submitNewProduct,
            onDismiss = viewModel::closeNewProductDialog
        )
    }

    if (uiState.editProductFormState != null) {
        EditProductDialog(
            form = uiState.editProductFormState!!,
            onUpdateForm = viewModel::updateEditProductForm,
            onSubmit = viewModel::submitEditProduct,
            onDismiss = viewModel::closeEditProductDialog
        )
    }

    if (uiState.productToDelete != null) {
        DeleteConfirmationDialog(
            product = uiState.productToDelete!!,
            onConfirm = viewModel::deleteProduct,
            onDismiss = viewModel::closeDeleteProductDialog
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.s4)
    ) {
        // Encabezado
        Text(
            text = stringResource(R.string.inventario_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Space.s3))

        // Pestañas: Stock de Productos vs Historial de Movimientos
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
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(Space.s2))
                        Text(text = "Stock Actual")
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
                        Text(text = stringResource(R.string.historial_movimientos))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Space.s3))

        if (selectedTab == 0) {
            StockListView(
                uiState = uiState,
                onSearchQueryChanged = viewModel::onSearchQueryChange,
                onToggleLowStockFilter = viewModel::setOnlyLowStockFilter,
                onOpenAdjustment = viewModel::openAdjustmentDialog,
                onOpenNewProduct = viewModel::openNewProductDialog,
                onOpenEditProduct = viewModel::openEditProductDialog,
                onOpenDeleteProduct = viewModel::openDeleteProductDialog
            )
        } else {
            MovementsHistoryView(movements = uiState.movements)
        }
    }
}

@Composable
private fun StockListView(
    uiState: InventarioUiState,
    onSearchQueryChanged: (String) -> Unit,
    onToggleLowStockFilter: (Boolean) -> Unit,
    onOpenAdjustment: (Producto) -> Unit,
    onOpenNewProduct: () -> Unit,
    onOpenEditProduct: (Producto) -> Unit,
    onOpenDeleteProduct: (Producto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Badges de Resumen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s2)
        ) {
            AssistChip(
                onClick = { onToggleLowStockFilter(false) },
                label = { Text(text = stringResource(R.string.total_productos, uiState.totalProductCount)) },
                colors = if (!uiState.onlyLowStockFilter) {
                    AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else AssistChipDefaults.assistChipColors()
            )

            AssistChip(
                onClick = { onToggleLowStockFilter(true) },
                label = { Text(text = stringResource(R.string.alertas_stock_bajo, uiState.lowStockCount)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Size.iconSmall)
                    )
                },
                colors = if (uiState.onlyLowStockFilter) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else AssistChipDefaults.assistChipColors()
            )
        }

        Spacer(modifier = Modifier.height(Space.s2))

        // Botón "+ Nuevo Producto" amplio colocado debajo de Total Productos y arriba del buscador
        Button(
            onClick = onOpenNewProduct,
            modifier = Modifier
                .fillMaxWidth()
                .height(Size.button),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(Size.iconSmall)
            )
            Spacer(modifier = Modifier.width(Space.s2))
            Text(
                text = stringResource(R.string.nuevo_producto),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(Space.s2))

        // Buscador
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = stringResource(R.string.buscar_producto)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(Space.s3))

        // Lista de Productos en Inventario
        if (uiState.products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay productos en esta vista",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Space.s2)
            ) {
                items(uiState.products, key = { it.id }) { product ->
                    ProductStockCard(
                        product = product,
                        onAdjustStock = { onOpenAdjustment(product) },
                        onEdit = { onOpenEditProduct(product) },
                        onDelete = { onOpenDeleteProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductStockCard(
    product: Producto,
    onAdjustStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isStockBajo) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(Space.s3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = product.emoji, fontSize = 28.sp)

                    Spacer(modifier = Modifier.width(Space.s2))

                    Column {
                        Text(
                            text = product.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = String.format(Locale.US, "$%.2f • %s", product.precio, product.categoria.displayName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.editar_producto),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.eliminar_producto),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Size.iconSmall)
                        )
                    }
                }
            }

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
                        text = stringResource(R.string.stock_actual_label, product.stockActual, product.stockMinimo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (product.isStockBajo) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Size.iconSmall)
                            )
                            Spacer(modifier = Modifier.width(Space.s1))
                            Text(
                                text = stringResource(R.string.estado_stock_bajo),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.estado_ok),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAdjustStock,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(Size.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(Space.s1))
                    Text(text = stringResource(R.string.ajustar_stock))
                }
            }
        }
    }
}

@Composable
private fun NewProductDialog(
    form: NewProductFormState,
    onUpdateForm: (String?, CategoriaProducto?, String?, String?, String?, String?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Space.s6),
                verticalArrangement = Arrangement.spacedBy(Space.s3)
            ) {
                Text(
                    text = stringResource(R.string.nuevo_producto),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onUpdateForm(it, null, null, null, null, null) },
                    label = { Text(text = stringResource(R.string.nombre_producto)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.categoria),
                    style = MaterialTheme.typography.labelLarge
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = CategoriaProducto.entries.filter { it != CategoriaProducto.TODOS }
                    items(categories) { cat ->
                        FilterChip(
                            selected = form.category == cat,
                            onClick = { onUpdateForm(null, cat, null, null, null, null) },
                            label = { Text(text = cat.displayName) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s2)
                ) {
                    OutlinedTextField(
                        value = form.priceText,
                        onValueChange = { onUpdateForm(null, null, it, null, null, null) },
                        label = { Text(text = stringResource(R.string.precio)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = form.initialStockText,
                        onValueChange = { onUpdateForm(null, null, null, it.filter { c -> c.isDigit() }, null, null) },
                        label = { Text(text = stringResource(R.string.stock_inicial)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = form.minStockText,
                    onValueChange = { onUpdateForm(null, null, null, null, it.filter { c -> c.isDigit() }, null) },
                    label = { Text(text = stringResource(R.string.stock_minimo)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.icono_emoji),
                    style = MaterialTheme.typography.labelLarge
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EMOJI_OPTIONS) { emojiOption ->
                        val selected = form.emoji == emojiOption
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateForm(null, null, null, null, null, emojiOption) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emojiOption, fontSize = 20.sp)
                        }
                    }
                }

                if (form.error != null) {
                    Text(
                        text = form.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(Space.s2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancelar))
                    }

                    Spacer(modifier = Modifier.width(Space.s2))

                    Button(
                        onClick = onSubmit,
                        enabled = form.isValid && !form.isSubmitting
                    ) {
                        Text(text = stringResource(R.string.guardar_producto))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProductDialog(
    form: EditProductFormState,
    onUpdateForm: (String?, CategoriaProducto?, String?, String?, String?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Space.s6),
                verticalArrangement = Arrangement.spacedBy(Space.s3)
            ) {
                Text(
                    text = stringResource(R.string.editar_producto),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onUpdateForm(it, null, null, null, null) },
                    label = { Text(text = stringResource(R.string.nombre_producto)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.categoria),
                    style = MaterialTheme.typography.labelLarge
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = CategoriaProducto.entries.filter { it != CategoriaProducto.TODOS }
                    items(categories) { cat ->
                        FilterChip(
                            selected = form.category == cat,
                            onClick = { onUpdateForm(null, cat, null, null, null) },
                            label = { Text(text = cat.displayName) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s2)
                ) {
                    OutlinedTextField(
                        value = form.priceText,
                        onValueChange = { onUpdateForm(null, null, it, null, null) },
                        label = { Text(text = stringResource(R.string.precio)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = form.minStockText,
                        onValueChange = { onUpdateForm(null, null, null, it.filter { c -> c.isDigit() }, null) },
                        label = { Text(text = stringResource(R.string.stock_minimo)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = stringResource(R.string.icono_emoji),
                    style = MaterialTheme.typography.labelLarge
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EMOJI_OPTIONS) { emojiOption ->
                        val selected = form.emoji == emojiOption
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateForm(null, null, null, null, emojiOption) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emojiOption, fontSize = 20.sp)
                        }
                    }
                }

                if (form.error != null) {
                    Text(
                        text = form.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(Space.s2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancelar))
                    }

                    Spacer(modifier = Modifier.width(Space.s2))

                    Button(
                        onClick = onSubmit,
                        enabled = form.isValid && !form.isSubmitting
                    ) {
                        Text(text = stringResource(R.string.guardar_cambios))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    product: Producto,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.eliminar_producto),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = stringResource(R.string.confirmar_eliminar_producto, product.nombre))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(R.string.eliminar))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancelar))
            }
        }
    )
}

@Composable
private fun StockAdjustmentDialog(
    form: AdjustmentFormState,
    onUpdateForm: (TipoMovimiento?, String?, String?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Space.s6),
                verticalArrangement = Arrangement.spacedBy(Space.s3)
            ) {
                Text(
                    text = "${stringResource(R.string.ajustar_stock)}: ${form.product.nombre}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Stock actual: ${form.product.stockActual} unidades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.tipo_movimiento),
                    style = MaterialTheme.typography.labelLarge
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf(TipoMovimiento.ENTRADA, TipoMovimiento.AJUSTE_MANUAL)
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = form.type == type,
                            onClick = { onUpdateForm(type, null, null) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                        ) {
                            Text(text = type.displayName)
                        }
                    }
                }

                OutlinedTextField(
                    value = form.quantityText,
                    onValueChange = { onUpdateForm(null, it.filter { char -> char.isDigit() }, null) },
                    label = { Text(text = stringResource(R.string.cantidad_ajuste)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = form.reasonText,
                    onValueChange = { onUpdateForm(null, null, it) },
                    label = { Text(text = stringResource(R.string.motivo_ajuste)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (form.error != null) {
                    Text(
                        text = form.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(Space.s2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancelar))
                    }

                    Spacer(modifier = Modifier.width(Space.s2))

                    Button(
                        onClick = onSubmit,
                        enabled = form.isValid && !form.isSubmitting
                    ) {
                        Text(text = stringResource(R.string.guardar_ajuste))
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementsHistoryView(
    movements: List<MovimientoInventario>,
    modifier: Modifier = Modifier
) {
    if (movements.isEmpty()) {
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
                        text = "Aún no se han registrado movimientos de inventario",
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
            items(movements, key = { it.id }) { movement ->
                MovementCard(movement = movement)
            }
        }
    }
}

@Composable
private fun MovementCard(
    movement: MovimientoInventario,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(movement.fechaTimestamp) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US)
        Instant.ofEpochMilli(movement.fechaTimestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.s3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.nombreProducto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${movement.tipo.displayName} • ${movement.motivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when (movement.tipo) {
                        TipoMovimiento.ENTRADA -> "+${movement.cantidad}"
                        TipoMovimiento.SALIDA_VENTA -> "-${movement.cantidad}"
                        TipoMovimiento.AJUSTE_MANUAL -> "Ajuste -> ${movement.cantidad}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (movement.tipo) {
                        TipoMovimiento.ENTRADA -> MaterialTheme.colorScheme.tertiary
                        TipoMovimiento.SALIDA_VENTA -> MaterialTheme.colorScheme.secondary
                        TipoMovimiento.AJUSTE_MANUAL -> MaterialTheme.colorScheme.primary
                    }
                )

                Text(
                    text = "Stock: ${movement.stockAnterior} ➔ ${movement.stockNuevo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
