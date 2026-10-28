package com.coffeetime.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coffeetime.CoffeeTimeApp
import com.coffeetime.domain.service.Session
import com.coffeetime.ui.login.LoginScreen
import com.coffeetime.ui.main.MainScreen
import com.coffeetime.ui.registro.RegistroScreen

@Composable
fun CoffeeTimeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    session: Session? = null
) {
    val context = LocalContext.current
    val container = (context.applicationContext as? CoffeeTimeApp)?.container
    val appSession = remember(session, context) {
        session ?: container?.session ?: Session()
    }
    val currentUser by appSession.currentUser.collectAsState()

    // Si Firebase guardó la sesión, se entra directo a Main sin pasar por Login.
    var startDestination by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(container) {
        val restored = container?.authenticationService?.restoreSession()
        startDestination = if (restored != null) Main else Login
    }

    val start = startDestination
    if (start == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = start,
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
            val authService = (context.applicationContext as CoffeeTimeApp).container.authenticationService

            RegistroScreen(
                authService = authService,
                onRegistered = {
                    // El registro ya abre la sesión.
                    navController.navigate(Main) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Main> {
            MainScreen(
                user = currentUser,
                onLogout = {
                    // También cierra la sesión guardada de Firebase.
                    container?.authenticationService?.logout() ?: appSession.logout()
                    navController.navigate(Login) {
                        popUpTo(Main) { inclusive = true }
                    }
                }
            )
        }
    }
}
