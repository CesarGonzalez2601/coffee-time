package com.coffeetime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.coffeetime.ui.login.LoginScreen
import com.coffeetime.ui.main.MainScreen
import com.coffeetime.ui.registro.RegistroExitoScreen
import com.coffeetime.ui.registro.RegistroScreen

@Composable
fun CoffeeTimeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Login,
        modifier = modifier
    ) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    // Al entrar se borra el historial: "atrás" no vuelve al login.
                    navController.navigate(Main) {
                        popUpTo(Login) { inclusive = true }
                    }
                },
                onCreateAccount = { navController.navigate(Registro) }
            )
        }

        composable<Registro> {
            RegistroScreen(
                onRegistered = { userId ->
                    navController.navigate(RegistroExito(userId = userId)) {
                        popUpTo(Registro) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RegistroExito> { backStackEntry ->
            val route = backStackEntry.toRoute<RegistroExito>()
            RegistroExitoScreen(
                userId = route.userId,
                onGoToLogin = {
                    navController.navigate(Login) {
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Main> {
            MainScreen(
                onLogout = {
                    navController.navigate(Login) {
                        popUpTo(Main) { inclusive = true }
                    }
                }
            )
        }
    }
}
