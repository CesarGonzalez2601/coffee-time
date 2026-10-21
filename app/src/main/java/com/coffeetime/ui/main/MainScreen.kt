package com.coffeetime.ui.main

import androidx.annotation.StringRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coffeetime.CoffeeTimeApp
import com.coffeetime.R
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Permission
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.model.User
import com.coffeetime.ui.inventario.InventarioScreen
import com.coffeetime.ui.inventario.InventarioViewModel
import com.coffeetime.ui.mas.MasScreen
import com.coffeetime.ui.mas.ReportesViewModel
import com.coffeetime.ui.navigation.Inicio
import com.coffeetime.ui.navigation.Inventario
import com.coffeetime.ui.navigation.Mas
import com.coffeetime.ui.navigation.Ordenes
import com.coffeetime.ui.navigation.Pagos
import com.coffeetime.ui.ordenes.OrdenesScreen
import com.coffeetime.ui.ordenes.OrdenesViewModel
import com.coffeetime.ui.pagos.PagosScreen
import com.coffeetime.ui.pagos.PagosViewModel
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class MainTab(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val requiredPermission: Permission? = null
)

// Pestañas principales con su permiso requerido para RBAC (#11)
private val mainTabs = listOf(
    MainTab(
        route = Inicio,
        label = R.string.nav_inicio,
        icon = Icons.Rounded.Home,
        selectedIcon = Icons.Filled.Home,
        requiredPermission = null
    ),
    MainTab(
        route = Ordenes,
        label = R.string.nav_ordenes,
        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        requiredPermission = Permission.CREATE_ORDER
    ),
    MainTab(
        route = Pagos,
        label = R.string.nav_pagos,
        icon = Icons.Rounded.Payments,
        selectedIcon = Icons.Filled.Payments,
        requiredPermission = Permission.PROCESS_PAYMENT
    ),
    MainTab(
        route = Inventario,
        label = R.string.nav_inventario,
        icon = Icons.Rounded.Inventory2,
        selectedIcon = Icons.Filled.Inventory2,
        requiredPermission = Permission.ADJUST_INVENTORY
    ),
    MainTab(
        route = Mas,
        label = R.string.nav_mas,
        icon = Icons.Rounded.MoreHoriz,
        selectedIcon = Icons.Filled.MoreHoriz,
        requiredPermission = Permission.VIEW_REPORTS
    ),
)

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    user: User? = null,
    tabsNavController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CoffeeTimeApp).container
    val activeUser = remember(user, context) {
        user ?: appContainer.session.getCurrentUser() ?: Cashier(id = 0, name = "Usuario")
    }

    // Capa 1 RBAC: ocultar destinos según user.hasPermission(...)
    val visibleTabs = remember(activeUser) {
        mainTabs.filter { tab ->
            tab.requiredPermission == null || activeUser.hasPermission(tab.requiredPermission)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val accessDeniedMessage = stringResource(R.string.rbac_access_denied)

    val backStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                visibleTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabsNavController.navigate(tab.route) {
                                popUpTo(tabsNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = stringResource(tab.label)
                            )
                        },
                        label = { Text(text = stringResource(tab.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabsNavController,
            startDestination = Inicio,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Inicio> {
                InicioDestination(
                    user = activeUser,
                    appContainer = appContainer,
                    onLogout = onLogout
                )
            }
            composable<Ordenes> {
                PermissionGuard(
                    user = activeUser,
                    requiredPermission = Permission.CREATE_ORDER,
                    navController = tabsNavController,
                    snackbarHostState = snackbarHostState,
                    deniedMessage = accessDeniedMessage
                ) {
                    val ordenesViewModel: OrdenesViewModel = viewModel(
                        factory = OrdenesViewModel.provideFactory(
                            appContainer.productRepository,
                            appContainer.orderRepository
                        )
                    )
                    OrdenesScreen(
                        viewModel = ordenesViewModel,
                        user = activeUser,
                        onNavigateToPagos = {
                            tabsNavController.navigate(Pagos) {
                                popUpTo(tabsNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
            composable<Pagos> {
                PermissionGuard(
                    user = activeUser,
                    requiredPermission = Permission.PROCESS_PAYMENT,
                    navController = tabsNavController,
                    snackbarHostState = snackbarHostState,
                    deniedMessage = accessDeniedMessage
                ) {
                    val pagosViewModel: PagosViewModel = viewModel(
                        factory = PagosViewModel.provideFactory(
                            appContainer.orderRepository,
                            appContainer.paymentRepository,
                            appContainer.productRepository,
                            appContainer.inventoryRepository
                        )
                    )
                    PagosScreen(
                        viewModel = pagosViewModel,
                        user = activeUser
                    )
                }
            }
            composable<Inventario> {
                PermissionGuard(
                    user = activeUser,
                    requiredPermission = Permission.ADJUST_INVENTORY,
                    navController = tabsNavController,
                    snackbarHostState = snackbarHostState,
                    deniedMessage = accessDeniedMessage
                ) {
                    val inventarioViewModel: InventarioViewModel = viewModel(
                        factory = InventarioViewModel.provideFactory(
                            appContainer.productRepository,
                            appContainer.inventoryRepository
                        )
                    )
                    InventarioScreen(
                        viewModel = inventarioViewModel
                    )
                }
            }
            composable<Mas> {
                PermissionGuard(
                    user = activeUser,
                    requiredPermission = Permission.VIEW_REPORTS,
                    navController = tabsNavController,
                    snackbarHostState = snackbarHostState,
                    deniedMessage = accessDeniedMessage
                ) {
                    val reportesViewModel: ReportesViewModel = viewModel(
                        factory = ReportesViewModel.provideFactory(
                            appContainer.paymentRepository,
                            appContainer.orderRepository,
                            appContainer.productRepository,
                            appContainer.cierreCajaRepository
                        )
                    )
                    MasScreen(
                        viewModel = reportesViewModel,
                        user = activeUser,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

/**
 * Capa 2 RBAC: Guard en cada destino.
 * Si el usuario no tiene el permiso requerido, regresa a Inicio y muestra "Acceso denegado".
 */
@Composable
private fun PermissionGuard(
    user: User,
    requiredPermission: Permission,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    deniedMessage: String,
    content: @Composable () -> Unit
) {
    val allowed = user.hasPermission(requiredPermission)

    if (allowed) {
        content()
    } else {
        LaunchedEffect(user, requiredPermission) {
            navController.navigate(Inicio) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
            snackbarHostState.showSnackbar(message = deniedMessage)
        }
    }
}

@Composable
private fun InicioDestination(
    user: User,
    appContainer: com.coffeetime.di.AppContainer,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleLabel = when (user.role) {
        Role.ADMINISTRATOR -> stringResource(R.string.role_admin)
        Role.CASHIER -> stringResource(R.string.role_cashier)
    }

    val currentDate = remember {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-SV"))
        LocalDate.now().format(formatter).replaceFirstChar { it.uppercase() }
    }

    val payments by appContainer.paymentRepository.getAllPaymentsFlow().collectAsState(initial = emptyList())
    val totalVentas = remember(payments) { payments.sumOf { it.montoRecibido - it.cambio } }
    val orderCount = payments.size
    val ticketPromedio = if (orderCount > 0) totalVentas / orderCount else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.s4),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabecera: Saludo en Fraunces con nombre, rol y fecha de la jornada
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.s1)
            ) {
                Text(
                    text = stringResource(R.string.inicio_saludo, user.name),
                    style = MaterialTheme.typography.headlineMedium, // Fraunces 28sp SemiBold
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.inicio_subtitulo, roleLabel, currentDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Métricas de la jornada o estado vacío
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (payments.isEmpty()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space.s6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s3)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(Size.touch),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.inicio_empty_metrics),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Space.s3)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(Space.s4)) {
                            Text(
                                text = stringResource(R.string.total_ventas),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", totalVentas),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.s3)
                    ) {
                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(Space.s4)) {
                                Text(
                                    text = stringResource(R.string.ordenes_cobradas),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$orderCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(Space.s4)) {
                                Text(
                                    text = stringResource(R.string.ticket_promedio),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "$%.2f", ticketPromedio),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botón de cerrar sesión definitivo
        LogoutButton(
            onLogout = onLogout,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LogoutButton(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onLogout,
        modifier = modifier.height(Size.button)
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
