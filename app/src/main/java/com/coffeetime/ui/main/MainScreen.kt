package com.coffeetime.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coffeetime.R
import com.coffeetime.ui.common.PlaceholderScreen
import com.coffeetime.ui.navigation.Inicio
import com.coffeetime.ui.navigation.Inventario
import com.coffeetime.ui.navigation.Mas
import com.coffeetime.ui.navigation.Ordenes
import com.coffeetime.ui.navigation.Pagos

private data class MainTab(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

// #11 filtra estas pestañas por rol con user.hasPermission(...).
private val mainTabs = listOf(
    MainTab(Inicio, R.string.nav_inicio, Icons.Rounded.Home, Icons.Filled.Home),
    MainTab(Ordenes, R.string.nav_ordenes, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    MainTab(Pagos, R.string.nav_pagos, Icons.Rounded.Payments, Icons.Filled.Payments),
    MainTab(Inventario, R.string.nav_inventario, Icons.Rounded.Inventory2, Icons.Filled.Inventory2),
    MainTab(Mas, R.string.nav_mas, Icons.Rounded.MoreHoriz, Icons.Filled.MoreHoriz),
)

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabsNavController = rememberNavController()
    val backStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                mainTabs.forEach { tab ->
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
                                contentDescription = null
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
                // Placeholder: #11 construye Inicio y el botón de cerrar sesión definitivo.
                PlaceholderScreen(title = stringResource(R.string.nav_inicio)) {
                    TextButton(onClick = onLogout) {
                        Text(text = stringResource(R.string.action_logout))
                    }
                }
            }
            composable<Ordenes> {
                PlaceholderScreen(title = stringResource(R.string.nav_ordenes))
            }
            composable<Pagos> {
                PlaceholderScreen(title = stringResource(R.string.nav_pagos))
            }
            composable<Inventario> {
                PlaceholderScreen(title = stringResource(R.string.nav_inventario))
            }
            composable<Mas> {
                PlaceholderScreen(title = stringResource(R.string.nav_mas))
            }
        }
    }
}
